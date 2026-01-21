package com.treewalker.rfidapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;
import com.treewalker.rfidapp.databinding.ActivityScannerBinding;
import com.treewalker.rfidapp.model.ProductNetwork;
import com.treewalker.rfidapp.network.ApiClient;
import com.treewalker.rfidapp.network.ApiService;
import com.treewalker.rfidapp.network.ProductsRequest;
import com.treewalker.rfidapp.network.ProductsResponse;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ScannerActivity - ultra-low-overhead inventory loop.
 * High-throughput producer + batched UI consumer.
 */
public class ScannerActivity extends BaseDrawerActivity {

    private static final String TAG = "ScannerActivity";

    private ActivityScannerBinding scannerBinding;
    private UHFService mDevice;

    // Scanning state
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private Thread scanThread;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Sound
    private SoundPool soundPool;
    private int soundScanId = 0;
    private long lastSoundTime = 0;
    private boolean continuousMode = false;

    // Lock-free structures for max throughput
    // scannedSet = very fast contains-check
    private final ConcurrentHashMap<String, Boolean> scannedSet = new ConcurrentHashMap<>();
    // scannedDeque holds order (newest first) for background use
    private final ConcurrentLinkedDeque<String> scannedDeque = new ConcurrentLinkedDeque<>();

    // Queue for newly discovered tags (producer -> UI flusher consumer)
    private final ConcurrentLinkedQueue<String> newTagsQueue = new ConcurrentLinkedQueue<>();

    // Adapter-visible list (ONLY mutated on UI thread)
    private final List<String> adapterList = new ArrayList<>();

    private TagListAdapter tagListAdapter;

    // Product search & selection
    private ArrayAdapter<String> productDropAdapter;
    private final List<ProductNetwork> productSearchResults = new ArrayList<>();
    private ProductNetwork selectedProductForMapping = null;

    // Tuning constants
    private static final int UI_UPDATE_INTERVAL_MS = 90;   // UI flush interval (ms)
    private static final int INVENTORY_TIMEOUT_MS = 20;    // inventoryOnce timeout (ms) - aggressive (tweak per device)
    private static final int SOUND_THROTTLE_MS = 60;       // ms between beeps
    private static final int ERROR_BACKOFF_MS = 60;        // sleep on hardware error (small)

    private final Gson gson = new Gson();

    // Search debounce
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY_MS = 400;

    // last UI update timestamp
    private long lastUiUpdateTime = 0L;

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_scanner; }

    @Override
    protected boolean useDataBinding() { return true; }

    @Override
    protected String getToolbarTitle() { return "Tag Mapping"; }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scannerBinding = (ActivityScannerBinding) super.binding;

        initUhfService();
        initSound();
        initViews();
        setupAdapters();
        setupListeners();

        // start UI flusher (runs periodically)
        mainHandler.post(uiFlushRunnable);

        updateUiState();
    }

    private void initUhfService() {
        try {
            mDevice = UHFService.getInstance();
            if (mDevice == null) throw new Exception("UHF not present");
        } catch (Exception e) {
            showSnackbarWithSettingsAction("UHF not available on this device. Open power settings?");
            Log.e(TAG, "UHF init failed: " + e.getMessage(), e);
            mDevice = null;
        }
    }

    private void initSound() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                soundPool = new SoundPool.Builder().setAudioAttributes(attrs).setMaxStreams(3).build();
            } else {
                soundPool = new SoundPool(3, android.media.AudioManager.STREAM_MUSIC, 0);
            }
            soundScanId = soundPool.load(this, R.raw.scan, 1);
        } catch (Exception e) {
            Log.w(TAG, "SoundPool init failed: " + e.getMessage());
            soundPool = null;
        }
    }

    private void initViews() {
        scannerBinding.productSelect.setInputType(InputType.TYPE_CLASS_TEXT);
        scannerBinding.productSelect.setThreshold(0);
        scannerBinding.productSelect.setSingleLine(true);
        scannerBinding.productSelect.setHorizontallyScrolling(true);

        scannerBinding.scanProgress.setVisibility(View.GONE);
        scannerBinding.scanStatusText.setVisibility(View.GONE);
        scannerBinding.scannedTagsRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupAdapters() {
        // adapterList is the single source of truth for RecyclerView — mutated only on main thread
        tagListAdapter = new TagListAdapter(adapterList,
                epc -> Toast.makeText(ScannerActivity.this, "Tag: " + epc, Toast.LENGTH_SHORT).show(),
                this::removeScannedTag
        );
        scannerBinding.scannedTagsRecycler.setAdapter(tagListAdapter);

        productDropAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        scannerBinding.productSelect.setAdapter(productDropAdapter);
    }

    private void setupListeners() {

        // ⭐ CONTINUOUS MODE SWITCH ⭐
        scannerBinding.modeSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            continuousMode = isChecked;

            // restart scanning with new mode
            if (isScanning.get()) {
                stopRFIDScan();
                startRFIDScan();
            }

            // UI feedback
            Snackbar.make(
                    scannerBinding.scannedTagsRecycler,
                    isChecked ? "Continuous Mode Enabled" : "Normal Mode Enabled",
                    Snackbar.LENGTH_SHORT
            ).show();
        });

        // ⭐ START / STOP SCAN BUTTON ⭐
        scannerBinding.startScanBtn.setOnClickListener(v -> {
            if (!isScanning.get()) startRFIDScan();
            else stopRFIDScan();
        });


        // ⭐ MAP TAGS BUTTON ⭐
        scannerBinding.mapTagsBtn.setOnClickListener(v -> {
            if (adapterList.isEmpty() || selectedProductForMapping == null) return;
            confirmAndMapScannedTags();
        });

        // ⭐ PRODUCT SEARCH TYPING LISTENER ⭐
        scannerBinding.productSelect.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                final String q = s == null ? "" : s.toString().trim();
                searchRunnable = () -> searchProducts(q);
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // ⭐ PRODUCT SELECT CLICK ⭐
        scannerBinding.productSelect.setOnItemClickListener((parent, view, position, id) -> {
            String display = (String) parent.getItemAtPosition(position);
            selectedProductForMapping = null;

            for (ProductNetwork p : productSearchResults) {
                String label = p.getProductName() + " | " + p.getProductCode();
                if (label.equals(display) ||
                        p.getProductName().equalsIgnoreCase(display) ||
                        p.getProductCode().equalsIgnoreCase(display)) {
                    selectedProductForMapping = p;
                    break;
                }
            }

            try {
                scannerBinding.productSelect.setText(display, false);
                scannerBinding.productSelect.setSelection(display.length());
            } catch (Throwable t) {
                scannerBinding.productSelect.setText(display);
            }

            mainHandler.post(this::updateUiState);
        });

        // ⭐ CLEAR TAGS BUTTON ⭐
        scannerBinding.clearTagsBtn.setOnClickListener(v -> {
            scannedSet.clear();
            scannedDeque.clear();
            newTagsQueue.clear();

            runOnUiThreadSafe(() -> {
                adapterList.clear();
                tagListAdapter.notifyDataSetChanged();
                updateUiState();
            });
        });
    }



    // remove tag (UI action, runs from UI thread because remove button is in RecyclerView item)
    private void removeScannedTag(String epc) {
        if (epc == null) return;

        // remove from background structures
        scannedSet.remove(epc);
        scannedDeque.remove(epc);

        // update UI list on main thread
        runOnUiThreadSafe(() -> {
            int pos = adapterList.indexOf(epc);
            if (pos >= 0) {
                adapterList.remove(pos);
                try {
                    tagListAdapter.notifyItemRemoved(pos);
                } catch (Exception ex) {
                    tagListAdapter.notifyDataSetChanged();
                }
            }
            updateUiState();
            Snackbar.make(scannerBinding.scannedTagsRecycler, "Removed " + epc, Snackbar.LENGTH_SHORT).show();
        });
    }

    // start scanner (producer)
    private void startRFIDScan() {
        if (mDevice == null) {
            showSnackbarWithSettingsAction("UHF not available. Open power settings?");
            tryReinitUhfIfNeeded();
            return;
        }

        isScanning.set(true);

        runOnUiThreadSafe(() -> {
            scannerBinding.startScanBtn.setText("Stop Scan");
            scannerBinding.scanProgress.setVisibility(View.VISIBLE);
            scannerBinding.scanStatusText.setText("Scanning...");
            scannerBinding.scanStatusText.setVisibility(View.VISIBLE);
        });

        // If continuous mode enabled → start hardware continuous scanning
        if (continuousMode) {
            try {
                mDevice.inventoryStart();   // start continuous scan
                Log.d(TAG, "Continuous scan started");
            } catch (Exception e) {
                Log.e(TAG, "inventoryStart failed: " + e.getMessage());
            }
        }

        scanThread = new Thread(this::scanLoop, "ScannerFastLoop");
        scanThread.start();
    }


    // the tight scan loop (producer). Reuses EPC to reduce allocation.
    private void scanLoop() {
        EPC epc = new EPC();
        int loopCounter = 0;

        try {
            while (isScanning.get() && !Thread.currentThread().isInterrupted()) {

                try {
                    if (continuousMode) {
                        // -------- CONTINUOUS SCAN MODE -------- //
                        List<EPC> list = mDevice.getTagIDs();
                        if (list != null && !list.isEmpty()) {
                            for (EPC tag : list) {
                                String id = tag.getId();
                                if (id != null && !id.isEmpty()) {
                                    Boolean prev = scannedSet.putIfAbsent(id, Boolean.TRUE);
                                    if (prev == null) {
                                        scannedDeque.addFirst(id);
                                        newTagsQueue.add(id);
                                    }
                                }
                            }
                        }

                        Thread.sleep(30); // small sleep to prevent 100% CPU
                    } else {
                        // -------- SINGLE SCAN MODE (your old logic) -------- //
                        boolean ok = mDevice.inventoryOnce(epc, INVENTORY_TIMEOUT_MS);
                        if (ok) {
                            final String id = epc.getId();
                            if (id != null && !id.isEmpty()) {
                                Boolean prev = scannedSet.putIfAbsent(id, Boolean.TRUE);
                                if (prev == null) {
                                    scannedDeque.addFirst(id);
                                    newTagsQueue.add(id);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "scan error: " + t.getMessage());
                    tryReinitUhfIfNeeded();
                    Thread.sleep(ERROR_BACKOFF_MS);
                }

                loopCounter++;
                if ((loopCounter & 0x3F) == 0) Thread.yield();
            }

        } catch (InterruptedException ignored) {}

        finally {
            // Stop continuous mode safely
            if (continuousMode) {
                try { mDevice.inventoryStop(); } catch (Exception ignored) {}
            }

            runOnUiThreadSafe(() -> {
                updateUiState();
                scannerBinding.scanProgress.setVisibility(View.GONE);
                scannerBinding.scanStatusText.setVisibility(View.GONE);
                scannerBinding.startScanBtn.setText("Start Scan");
            });
        }
    }


    // UI flusher (consumer) — batches new tags and updates adapter on main thread
    private final Runnable uiFlushRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (!newTagsQueue.isEmpty()) {
                    long now = System.currentTimeMillis();
                    final List<String> batch = new ArrayList<>();

                    // drain queue quickly
                    String epc;
                    while ((epc = newTagsQueue.poll()) != null) {
                        batch.add(epc);
                    }

                    if (!batch.isEmpty()) {
                        // beep once per batch (throttled)
                        if (soundPool != null && soundScanId != 0 && now - lastSoundTime > SOUND_THROTTLE_MS) {
                            try { soundPool.play(soundScanId, 1f, 1f, 1, 0, 1f); } catch (Exception ignored) {}
                            lastSoundTime = now;
                        }

                        // Update adapterList on UI thread
                        runOnUiThreadSafe(() -> {
                            try {
                                // insert batch at position 0 (batch[0] is oldest in this drained list; we want newest-first)
                                // Because scannedDeque.addFirst() adds newest first, newTagsQueue will yield newest -> oldest
                                // we want to insert in order such that newest becomes index 0: iterate batch and add in same order
                                adapterList.addAll(0, batch);
                                tagListAdapter.notifyItemRangeInserted(0, batch.size());
                                scannerBinding.scannedTagsRecycler.scrollToPosition(0);
                            } catch (Exception ex) {
                                tagListAdapter.notifyDataSetChanged(); // fallback
                            }
                            updateUiState();
                        });
                    }
                } else {
                    // occasional UI tick so counts remain up-to-date while scanning
                    if (isScanning.get()) {
                        long now = System.currentTimeMillis();
                        if (now - lastUiUpdateTime > UI_UPDATE_INTERVAL_MS * 4) {
                            runOnUiThreadSafe(ScannerActivity.this::updateUiState);
                        }
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "uiFlushRunnable error: " + t.getMessage());
            } finally {
                mainHandler.postDelayed(this, UI_UPDATE_INTERVAL_MS);
            }
        }
    };

    private void stopRFIDScan() {
        isScanning.set(false);
        if (scanThread != null) {
            scanThread.interrupt();
            scanThread = null;
        }
        runOnUiThreadSafe(() -> {
            scannerBinding.startScanBtn.setText("Start Scan");
            scannerBinding.scanProgress.setVisibility(View.GONE);
            scannerBinding.scanStatusText.setVisibility(View.GONE);
            updateUiState();
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 142
                || keyCode == KeyEvent.KEYCODE_F1
                || keyCode == KeyEvent.KEYCODE_F2
                || keyCode == KeyEvent.KEYCODE_BUTTON_R1
                || keyCode == KeyEvent.KEYCODE_BUTTON_A) {

            if (!isScanning.get()) startRFIDScan();
            else stopRFIDScan();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // Product search (unchanged)
    private void searchProducts(String query) {
        ProductsRequest req = new ProductsRequest();
        req.setSearch(query);
        req.setStatus("all");
        req.setPage(1);
        req.setLimit(20);

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getProducts(req).enqueue(new Callback<ProductsResponse>() {
            @Override public void onResponse(Call<ProductsResponse> call, Response<ProductsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    productSearchResults.clear();
                    List<ProductNetwork> list = response.body().getProducts();
                    if (list != null) productSearchResults.addAll(list);

                    List<String> labels = new ArrayList<>();
                    for (ProductNetwork p : productSearchResults) {
                        labels.add(p.getProductName() + " | " + p.getProductCode());
                    }

                    productDropAdapter.clear();
                    productDropAdapter.addAll(labels);
                    productDropAdapter.notifyDataSetChanged();

                    if (!labels.isEmpty()) {
                        scannerBinding.productSelect.post(() -> scannerBinding.productSelect.showDropDown());
                    }
                }
            }

            @Override public void onFailure(Call<ProductsResponse> call, Throwable t) {
                Log.e(TAG, "search failed", t);
                Snackbar.make(scannerBinding.scannedTagsRecycler, "Product search failed", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    // confirm & map
    private void confirmAndMapScannedTags() {
        String productLabel = selectedProductForMapping != null ? selectedProductForMapping.getProductName() : "—";
        new AlertDialog.Builder(this)
                .setTitle("Confirm mapping")
                .setMessage("Map " + adapterList.size() + " tags to product:\n" + productLabel)
                .setPositiveButton("Map", (d, w) -> mapScannedTagsToProduct())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void mapScannedTagsToProduct() {
        if (selectedProductForMapping == null || adapterList.isEmpty()) return;

        // adapterList is up-to-date and on UI thread (this method is triggered from UI)
        List<String> epcCodes = new ArrayList<>(adapterList);

        if (epcCodes.isEmpty()) {
            Snackbar.make(scannerBinding.scannedTagsRecycler, "No valid tags to map", Snackbar.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("product_id", selectedProductForMapping.getId());
        payload.put("epc_codes", epcCodes);

        runOnUiThreadSafe(() -> {
            scannerBinding.scanStatusText.setText("Mapping tags...");
            scannerBinding.scanStatusText.setVisibility(View.VISIBLE);
        });

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.tagMapping(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                runOnUiThreadSafe(() -> scannerBinding.scanStatusText.setVisibility(View.GONE));
                if (!response.isSuccessful() || response.body() == null) {
                    Snackbar.make(scannerBinding.scannedTagsRecycler,
                            "Mapping failed: HTTP " + response.code(), Snackbar.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> body = response.body();
                boolean anySuccess = false, anyFailure = false;
                Object resultsObj = body.get("results");
                if (resultsObj instanceof List<?>) {
                    List<?> results = (List<?>) resultsObj;
                    for (Object item : results) {
                        if (item instanceof Map<?, ?>) {
                            Map<String, Object> row = (Map<String, Object>) item;
                            Object rowOk = row.get("success");
                            boolean rowSuccess = rowOk instanceof Boolean ? (Boolean) rowOk : "1".equals(String.valueOf(rowOk));
                            if (rowSuccess) anySuccess = true;
                            else anyFailure = true;
                        }
                    }
                }

                if (!anySuccess && anyFailure) {
                    Snackbar.make(scannerBinding.scannedTagsRecycler, "Tags are already mapped.", Snackbar.LENGTH_LONG).show();
                    return;
                }
                if (anySuccess) {
                    // clear background structures & adapter list
                    scannedSet.clear();
                    scannedDeque.clear();
                    newTagsQueue.clear();
                    runOnUiThreadSafe(() -> {
                        adapterList.clear();
                        tagListAdapter.notifyDataSetChanged();
                        updateUiState();
                    });
                }

                if (anySuccess && !anyFailure) {
                    Snackbar.make(scannerBinding.scannedTagsRecycler, "Mapping completed", Snackbar.LENGTH_SHORT).show();
                } else if (anySuccess) {
                    Snackbar.make(scannerBinding.scannedTagsRecycler, "Some tags mapped", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                runOnUiThreadSafe(() -> {
                    scannerBinding.scanStatusText.setVisibility(View.GONE);
                    Snackbar.make(scannerBinding.scannedTagsRecycler,
                            "Network error: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryReinitUhfIfNeeded();
        try {
            if (mDevice != null) {
                try { mDevice.open(); } catch (Exception ignored) {}
            }
        } catch (Throwable t) {
            Log.w(TAG, "onResume: failed to open mDevice: " + t.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRFIDScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRFIDScan();
        if (soundPool != null) {
            try { soundPool.release(); } catch (Exception ignore) {}
            soundPool = null;
        }
        mainHandler.removeCallbacks(uiFlushRunnable);
    }

    private void updateUiState() {
        long now = System.currentTimeMillis();
        if (isScanning.get() && (now - lastUiUpdateTime) < UI_UPDATE_INTERVAL_MS) return;
        lastUiUpdateTime = now;

        runOnUiThreadSafe(() -> {
            scannerBinding.tvScannedCount.setText(String.valueOf(scannedDeque.size()));
            boolean enabled = !adapterList.isEmpty();
            if (scannerBinding.mapTagsBtn.isEnabled() != enabled) scannerBinding.mapTagsBtn.setEnabled(enabled);
            boolean clearEnabled = !adapterList.isEmpty();
            if (scannerBinding.clearTagsBtn.isEnabled() != clearEnabled) scannerBinding.clearTagsBtn.setEnabled(clearEnabled);
        });
    }

    // Recycler adapter (backed by adapterList which is mutated only on UI thread)
    private static class TagListAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<TagViewHolder> {
        interface OnTagClick { void onTagClicked(String tag); }
        interface OnRemoveClick { void onRemoveClicked(String tag); }

        private final List<String> tags;
        private final OnTagClick clickListener;
        private final OnRemoveClick removeListener;

        TagListAdapter(List<String> tags, OnTagClick clickListener, OnRemoveClick removeListener) {
            this.tags = tags;
            this.clickListener = clickListener;
            this.removeListener = removeListener;
        }

        // client (UI flusher) will mutate tags list directly (it's the same reference)
        // optionally helper methods could be added here if desired

        @NonNull @Override
        public TagViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scanned_tag, parent, false);
            return new TagViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
            String epc = tags.get(position);
            holder.bind(epc, () -> clickListener.onTagClicked(epc), () -> removeListener.onRemoveClicked(epc));
        }

        @Override public int getItemCount() { return tags == null ? 0 : tags.size(); }
    }

    private static class TagViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        private final com.google.android.material.textview.MaterialTextView tv;
        private final android.widget.ImageButton btnRemove;

        TagViewHolder(@NonNull View v) {
            super(v);
            tv = v.findViewById(R.id.tvTag);
            btnRemove = v.findViewById(R.id.btnRemoveTag);
        }

        void bind(String epc, Runnable onClick, Runnable onRemove) {
            tv.setText(epc);
            itemView.setOnClickListener(v -> onClick.run());
            btnRemove.setOnClickListener(v -> onRemove.run());
        }
    }

    // try reinit UHFService with light checks
    private void tryReinitUhfIfNeeded() {
        try {
            if (mDevice == null) {
                mDevice = UHFService.getInstance();
                if (mDevice != null) {
                    Snackbar.make(scannerBinding.scannedTagsRecycler, "UHF re-initialized", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                showSnackbarWithSettingsAction("UHF service not running. Open power settings?");
            } else {
                try {
                    EPC e = new EPC();
                    boolean ok = mDevice.inventoryOnce(e, 50);
                    Log.d(TAG, "UHF test -> " + ok);
                } catch (Throwable t) {
                    try {
                        mDevice = UHFService.getInstance();
                        if (mDevice == null) showSnackbarWithSettingsAction("UHF error — open power settings?");
                    } catch (Throwable ignore) {
                        showSnackbarWithSettingsAction("UHF error — open power settings?");
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Re-init UHFService failed: " + ex.getMessage(), ex);
            mDevice = null;
            showSnackbarWithSettingsAction("UHF re-init failed. Open power settings?");
        }
    }

    private void showSnackbarWithSettingsAction(String message) {
        mainHandler.post(() -> {
            Snackbar snackbar = Snackbar.make(scannerBinding.scannedTagsRecycler, message, Snackbar.LENGTH_LONG);
            snackbar.setAction("Open power settings", v -> openPowerSettings());
            snackbar.show();
        });
    }

    private void openPowerSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            } else {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open power settings: " + e.getMessage(), e);
            Snackbar.make(scannerBinding.scannedTagsRecycler, "Unable to open settings", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void runOnUiThreadSafe(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) r.run();
        else mainHandler.post(r);
    }
}
