package com.galla.rfidapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;
import com.galla.rfidapp.databinding.ActivityTagmappingBinding;
import com.galla.rfidapp.model.AssetNetwork;
import com.galla.rfidapp.network.ApiClient;
import com.galla.rfidapp.network.ApiService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.galla.rfidapp.util.SyncManager;
/**
 * ScannerActivity - RFID scanning and tag → asset mapping
 *
 * Behavior changes:
 *  - Continuous reading mode removed.
 *  - Each Start Scan run will try to read one tag, then stop.
 *  - The scan attempt has a 2 second overall timeout (if no tag found, it stops).
 */
public class TagMappingActivity extends BaseDrawerActivity {

    private static final String TAG = "ScannerActivity";
    private long lastSeenSyncVersion = 0;

    private ActivityTagmappingBinding scannerBinding;
    private UHFService mDevice;

    // Scanning state
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private Thread scanThread;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Sound
    private SoundPool soundPool;
    private int soundScanId = 0;
    private long lastSoundTime = 0;

    // Lock-free structures for max throughput
    private final ConcurrentHashMap<String, Boolean> scannedSet = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<String> scannedDeque = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedQueue<String> newTagsQueue = new ConcurrentLinkedQueue<>();

    // Adapter-visible list (ONLY mutated on UI thread)
    private final List<String> adapterList = new ArrayList<>();
    private TagListAdapter tagListAdapter;

    // Dropdown adapter
    private ArrayAdapter<String> productDropAdapter;

    // Cached assets keyed by rfid (AssetNetwork objects)
    private final Map<String, AssetNetwork> assetsByRfid = new HashMap<>();

    // Simple list used for dropdown results (AssetItem)
    private final List<AssetItem> productSearchResults = new ArrayList<>();
    private final Map<String, AssetItem> labelToAsset = new HashMap<>();
    private AssetItem selectedAssetForMapping = null;

    // Selection guard to ignore TextWatcher events triggered by programmatic setText()
    private boolean isSelectingFromDropdown = false;

    // keeps track of found rfids (so UI can mark them if you later want)
    private final ConcurrentHashMap<String, Boolean> foundRfids = new ConcurrentHashMap<>();

    // Tuning constants
    private static final int UI_UPDATE_INTERVAL_MS = 90;
    private static final int INVENTORY_TIMEOUT_MS = 20; // ms passed to inventoryOnce
    private static final int SOUND_THROTTLE_MS = 60;
    private static final int ERROR_BACKOFF_MS = 60;

    private final Gson gson = new Gson();

    // Search debounce
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY_MS = 400;

    private long lastUiUpdateTime = 0L;

    // --- cache keys ---
    private static final String PREFS_NAME = "galla_inventory_cache";
    private static final String PREF_KEY_ASSETS = "cached_assets_v1";

    // ----------------- Inner helper models -----------------
    private static class AssetItem {
        final Integer id;
        final String name;
        final String assetTag;

        AssetItem(String name, String assetTag, Integer id) {
            this.name = name;
            this.assetTag = assetTag;
            this.id = id;
        }
    }

    // ----------------- Activity lifecycle -----------------
    @Override
    protected int getLayoutResourceId() { return R.layout.activity_tagmapping; }

    @Override
    protected boolean useDataBinding() { return true; }

    @Override
    protected String getToolbarTitle() { return "Tag Mapping"; }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scannerBinding = (ActivityTagmappingBinding) super.binding;

        initUhfService();
        initSound();
        initViews();
        setupAdapters();
        setupListeners();

        // load cached assets (and try live)
        loadAssetsAndLocations();

        // start UI flusher
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

        // NOTE: do not reference modeSwitch directly here (removed/optional in layout)

        scannerBinding.scanProgress.setVisibility(View.GONE);
        scannerBinding.scanStatusText.setVisibility(View.GONE);
        scannerBinding.scannedTagsRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupAdapters() {
        tagListAdapter = new TagListAdapter(adapterList,
                epc -> Toast.makeText(TagMappingActivity.this, "Tag: " + epc, Toast.LENGTH_SHORT).show(),
                this::removeScannedTag
        );
        scannerBinding.scannedTagsRecycler.setAdapter(tagListAdapter);

        productDropAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        scannerBinding.productSelect.setAdapter(productDropAdapter);
    }

    private void setupListeners() {

        // START / STOP SCAN BUTTON
        scannerBinding.startScanBtn.setOnClickListener(v -> {
            if (!isScanning.get()) startRFIDScan();
            else stopRFIDScan();
        });

        // MAP TAGS BUTTON
        scannerBinding.mapTagsBtn.setOnClickListener(v -> {
            if (adapterList.isEmpty() || selectedAssetForMapping == null) {
                Snackbar.make(scannerBinding.scannedTagsRecycler, "Select an asset and add tags first", Snackbar.LENGTH_SHORT).show();
                return;
            }
            confirmAndMapScannedTags();
        });

        // PRODUCT SEARCH TYPING LISTENER (debounced)
        scannerBinding.productSelect.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isSelectingFromDropdown) {
                    // ignore events triggered by dropdown selection programmatic setText()
                    return;
                }
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                final String q = s == null ? "" : s.toString().trim();
                searchRunnable = () -> searchProducts(q);
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }
        });

        // PRODUCT SELECT CLICK (maps AssetItem -> selectedAssetForMapping)
        scannerBinding.productSelect.setOnItemClickListener((parent, view, position, id) -> {
            Object obj = parent.getItemAtPosition(position);
            if (obj == null) {
                selectedAssetForMapping = null;
                return;
            }

            // guard against TextWatcher clearing maps while selection in-flight
            isSelectingFromDropdown = true;

            String display = String.valueOf(obj);
            // try direct lookup
            AssetItem ai = labelToAsset.get(display);

            // fallback: try plain name or tag match
            if (ai == null) {
                for (AssetItem it : productSearchResults) {
                    String label = makeLabel(it);
                    if (label.equals(display) || it.name.equals(display) || it.assetTag.equals(display)) {
                        ai = it;
                        break;
                    }
                }
            }

            // last fallback: try extract tag after '|' and match
            if (ai == null && display.contains("|")) {
                String maybeTag = display.substring(display.lastIndexOf("|") + 1).trim();
                ai = labelToAsset.get(maybeTag); // unlikely but try
                if (ai == null) {
                    for (AssetItem it : productSearchResults) {
                        if (maybeTag.equals(it.assetTag)) { ai = it; break; }
                    }
                }
            }

            selectedAssetForMapping = ai;

            // set text to the chosen label (if any), else set raw display
            try {
                scannerBinding.productSelect.setText(display, false);
                scannerBinding.productSelect.setSelection(display.length());
            } catch (Throwable t) {
                scannerBinding.productSelect.setText(display);
            }

            Log.d(TAG, "Product selected: display=" + display + " -> asset=" + (ai == null ? "NULL" : ai.assetTag + " / " + ai.name));
            updateUiState();

            // restore normal handling after a short delay (allow UI to settle)
            scannerBinding.productSelect.postDelayed(() -> isSelectingFromDropdown = false, 200);
        });

        // CLEAR TAGS BUTTON
        scannerBinding.clearTagsBtn.setOnClickListener(v -> {
            scannedSet.clear();
            scannedDeque.clear();
            newTagsQueue.clear();
            foundRfids.clear();

            runOnUiThreadSafe(() -> {
                adapterList.clear();
                tagListAdapter.notifyDataSetChanged();
                updateUiState();
            });
        });
    }

    // remove tag (UI action)
    private void removeScannedTag(String epc) {
        if (epc == null) return;

        scannedSet.remove(epc);
        scannedDeque.remove(epc);
        foundRfids.remove(epc);

        runOnUiThreadSafe(() -> {
            int pos = adapterList.indexOf(epc);
            if (pos >= 0) {
                adapterList.remove(pos);
                try { tagListAdapter.notifyItemRemoved(pos); } catch (Exception ex) { tagListAdapter.notifyDataSetChanged(); }
            }
            updateUiState();
            Snackbar.make(scannerBinding.scannedTagsRecycler, "Removed " + epc, Snackbar.LENGTH_SHORT).show();
        });
    }

    // start scanner
    private void startRFIDScan() {
        if (mDevice == null) {
            showSnackbarWithSettingsAction("UHF not available. Open power settings?");
            tryReinitUhfIfNeeded();
            return;
        }

        // 🚫 Prevent double start
        if (!isScanning.compareAndSet(false, true)) {
            return;
        }

        // 🧹 IMPORTANT: clear previous scan result (ONE TAG PER SCAN)
        clearScannedTags();

        // 🔒 Disable asset search input while scanning
        disableSearchInput();

        runOnUiThreadSafe(() -> {
            scannerBinding.startScanBtn.setText("Scanning...");
            scannerBinding.startScanBtn.setEnabled(false);
            scannerBinding.scanProgress.setVisibility(View.VISIBLE);
            scannerBinding.scanStatusText.setText("Scanning (2s)...");
            scannerBinding.scanStatusText.setVisibility(View.VISIBLE);
        });

        // 🔁 Start single-tag scan thread
        scanThread = new Thread(this::scanLoop, "ScannerSingleTagLoop");
        scanThread.start();
    }



    private void stopRFIDScanInternal(boolean foundOne) {
        isScanning.set(false);

        // 🔓 Re-enable asset search input
        enableSearchInput();

        runOnUiThreadSafe(() -> {
            scannerBinding.startScanBtn.setText("Start Scan");
            scannerBinding.startScanBtn.setEnabled(true);
            scannerBinding.scanProgress.setVisibility(View.GONE);
            scannerBinding.scanStatusText.setVisibility(View.GONE);

            // ✅ If scan succeeded → keep only latest tag
            if (foundOne) {
                adapterList.clear();
                tagListAdapter.notifyDataSetChanged();
            } else {
                Snackbar.make(
                        scannerBinding.scannedTagsRecycler,
                        "No tag found (timeout)",
                        Snackbar.LENGTH_SHORT
                ).show();
            }

            updateUiState();
        });
    }



    // scan loop: attempt inventoryOnce repeatedly until tag found or 2 second timeout
    private void scanLoop() {
        EPC epc = new EPC();
        long endTime = System.currentTimeMillis() + 2000L; // 2 seconds
        boolean foundOne = false;

        try {
            while (isScanning.get()
                    && System.currentTimeMillis() < endTime
                    && !Thread.currentThread().isInterrupted()) {

                try {
                    boolean ok = mDevice.inventoryOnce(epc, INVENTORY_TIMEOUT_MS);
                    if (ok) {
                        String id = epc.getId();
                        if (id != null && !id.isEmpty()) {

                            // ✅ ensure only ONE tag ever enters
                            if (scannedSet.putIfAbsent(id, Boolean.TRUE) == null) {
                                scannedDeque.clear();
                                newTagsQueue.clear();

                                scannedDeque.addFirst(id);
                                newTagsQueue.add(id);

                                foundOne = true;
                                isScanning.set(false); // 🔒 HARD STOP
                                break;
                            }
                        }
                    }
                    Thread.sleep(30);
                } catch (Throwable t) {
                    Log.w(TAG, "scan error: " + t.getMessage());
                    tryReinitUhfIfNeeded();
                    Thread.sleep(ERROR_BACKOFF_MS);
                }
            }
        } catch (InterruptedException ignored) {
        } finally {
            stopRFIDScanInternal(foundOne);
        }
    }


    private final Runnable uiFlushRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (!newTagsQueue.isEmpty()) {
                    long now = System.currentTimeMillis();
                    final List<String> batch = new ArrayList<>();

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

                        // mark found assets from cache if present
                        for (String e : batch) {
                            if (assetsByRfid.containsKey(e)) {
                                foundRfids.put(e, Boolean.TRUE);
                            }
                        }

                        // Update adapterList on UI thread
                        runOnUiThreadSafe(() -> {
                            try {
                                adapterList.addAll(0, batch);
                                tagListAdapter.notifyItemRangeInserted(0, batch.size());
                                scannerBinding.scannedTagsRecycler.scrollToPosition(0);
                            } catch (Exception ex) {
                                tagListAdapter.notifyDataSetChanged();
                            }
                            updateUiState();
                        });
                    }
                } else {
                    if (isScanning.get()) {
                        long now = System.currentTimeMillis();
                        if (now - lastUiUpdateTime > UI_UPDATE_INTERVAL_MS * 4) {
                            runOnUiThreadSafe(TagMappingActivity.this::updateUiState);
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

    // ---------------- SEARCH / SELECTION ----------------
    private static String makeLabel(AssetItem it) {
        if (it.assetTag == null || it.assetTag.isEmpty()) return it.name;
        return it.name + " | " + it.assetTag;
    }

    private void searchProducts(String query) {
        final String q = query == null ? "" : query.trim().toLowerCase();

        productSearchResults.clear();
        labelToAsset.clear();
        List<String> labels = new ArrayList<>();

        if (q.length() < 1) {
            productDropAdapter.clear();
            productDropAdapter.notifyDataSetChanged();
            return;
        }

        int cap = 50;

        for (AssetNetwork an : assetsByRfid.values()) {
            String name = an.getName() != null ? an.getName() : "";
            String code = an.getAssetTag() != null ? an.getAssetTag() : "";

            if (!name.toLowerCase().contains(q)
                    && !code.toLowerCase().contains(q)) {
                continue;
            }

            AssetItem ai = new AssetItem(name, code, an.getId());
            productSearchResults.add(ai);

            String label = makeLabel(ai);
            labels.add(label);
            labelToAsset.put(label, ai);          // map label -> asset
            labelToAsset.put(code, ai);           // also map bare code -> asset for fallback

            if (labels.size() >= cap) break;
        }

        productDropAdapter.clear();
        productDropAdapter.addAll(labels);
        productDropAdapter.notifyDataSetChanged();

        if (!labels.isEmpty()) {
            scannerBinding.productSelect.post(
                    () -> scannerBinding.productSelect.showDropDown()
            );
        }
    }

    // confirm & map
    private void confirmAndMapScannedTags() {
        String productLabel = selectedAssetForMapping != null ? selectedAssetForMapping.name : "—";
        new AlertDialog.Builder(this)
                .setTitle("Confirm mapping")
                .setMessage("Map " + adapterList.size() + " tags to asset:\n" + productLabel)
                .setPositiveButton("Map", (d, w) -> mapScannedTagsToProduct())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // mapScannedTagsToProduct unchanged — keep your previous robust implementation
    private void mapScannedTagsToProduct() {
        if (selectedAssetForMapping == null || adapterList.isEmpty()) return;

        List<String> epcCodes = new ArrayList<>(adapterList);
        if (epcCodes.isEmpty()) {
            Snackbar.make(scannerBinding.scannedTagsRecycler,
                    "No valid tags to map", Snackbar.LENGTH_SHORT).show();
            return;
        }

        ApiService api = ApiClient.getClient(this).create(ApiService.class);

        // ================= SINGLE TAG =================
        if (epcCodes.size() == 1) {
            final String epc = epcCodes.get(0);

            Map<String, Object> payload = new HashMap<>();
            payload.put("asset_tag", selectedAssetForMapping.assetTag);
            payload.put("rfid", epc);

            runOnUiThreadSafe(() -> {
                scannerBinding.scanStatusText.setText("Updating RFID...");
                scannerBinding.scanStatusText.setVisibility(View.VISIBLE);
            });

            api.updateRfid(payload).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call,
                                       Response<Map<String, Object>> response) {

                    runOnUiThreadSafe(() ->
                            scannerBinding.scanStatusText.setVisibility(View.GONE));

                    try {
                        // ✅ SUCCESS
                        if (response.isSuccessful() && response.body() != null) {
                            String message = response.body().get("message") != null
                                    ? String.valueOf(response.body().get("message"))
                                    : "RFID updated successfully";

                            clearScannedTags();

                            Snackbar.make(scannerBinding.scannedTagsRecycler,
                                    message, Snackbar.LENGTH_SHORT).show();
                            return;
                        }

                        // ❌ ERROR (409 / 404 / 422)
                        String errorMessage = "Update failed";

                        if (response.errorBody() != null) {
                            String errJson = response.errorBody().string();
                            Map<String, Object> errBody =
                                    new Gson().fromJson(errJson, Map.class);

                            if (errBody != null && errBody.get("message") != null) {
                                errorMessage = String.valueOf(errBody.get("message"));
                            }
                        }

                        Snackbar.make(scannerBinding.scannedTagsRecycler,
                                errorMessage, Snackbar.LENGTH_LONG).show();

                    } catch (Exception e) {
                        Log.e(TAG, "updateRfid parse error", e);
                        Snackbar.make(scannerBinding.scannedTagsRecycler,
                                "Unexpected server response",
                                Snackbar.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    runOnUiThreadSafe(() -> {
                        scannerBinding.scanStatusText.setVisibility(View.GONE);
                        Snackbar.make(scannerBinding.scannedTagsRecycler,
                                "Network error: " + t.getMessage(),
                                Snackbar.LENGTH_SHORT).show();
                    });
                }
            });
            return;
        }

        // ================= MULTI TAG =================
        Map<String, Object> payload = new HashMap<>();
        payload.put("product_id", selectedAssetForMapping.id);
        payload.put("epc_codes", epcCodes);

        runOnUiThreadSafe(() -> {
            scannerBinding.scanStatusText.setText("Mapping tags...");
            scannerBinding.scanStatusText.setVisibility(View.VISIBLE);
        });

        api.tagMapping(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                                   Response<Map<String, Object>> response) {

                runOnUiThreadSafe(() ->
                        scannerBinding.scanStatusText.setVisibility(View.GONE));

                if (!response.isSuccessful() || response.body() == null) {
                    Snackbar.make(scannerBinding.scannedTagsRecycler,
                            "Mapping failed", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> body = response.body();
                String message = body.get("message") != null
                        ? String.valueOf(body.get("message"))
                        : "Mapping completed";

                clearScannedTags();

                Snackbar.make(scannerBinding.scannedTagsRecycler,
                        message, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                runOnUiThreadSafe(() -> {
                    scannerBinding.scanStatusText.setVisibility(View.GONE);
                    Snackbar.make(scannerBinding.scannedTagsRecycler,
                            "Network error: " + t.getMessage(),
                            Snackbar.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void clearScannedTags() {
        scannedSet.clear();
        scannedDeque.clear();
        newTagsQueue.clear();
        foundRfids.clear();

        runOnUiThreadSafe(() -> {
            adapterList.clear();
            tagListAdapter.notifyDataSetChanged();
            updateUiState();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 🔄 Global sync detection
        long globalVersion = SyncManager.getSyncVersion(this);
        if (globalVersion > lastSeenSyncVersion) {
            lastSeenSyncVersion = globalVersion;
            Log.d(TAG, "Global sync detected → auto refresh Tag Mapping");
            loadAssetsAndLocations();
        }

        // existing logic
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

    // Recycler adapter
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

    // ---------------- load & cache assets ----------------
    private void loadAssetsAndLocations() {
        runOnUiThreadSafe(() -> {
            scannerBinding.scanStatusText.setText("Loading assets...");
            scannerBinding.scanStatusText.setVisibility(View.VISIBLE);
        });

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        Call<Map<String, Object>> call = api.getAllAssetDetails();
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                runOnUiThreadSafe(() -> scannerBinding.scanStatusText.setVisibility(View.GONE));

                if (!response.isSuccessful() || response.body() == null) {
                    loadFromCache();
                    Snackbar.make(scannerBinding.scannedTagsRecycler, "Failed to fetch live assets — using cache", Snackbar.LENGTH_LONG).show();
                    return;
                }

                try {
                    Map<String, Object> body = response.body();
                    Object assetsObj = body.get("assets");

                    assetsByRfid.clear();
                    if (assetsObj instanceof List<?>) {
                        List<?> assets = (List<?>) assetsObj;
                        for (Object o : assets) {
                            if (o instanceof Map<?, ?>) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> row = (Map<String, Object>) o;

                                // Try to get RFID (prefer rfid field, fallback to asset_tag)
                                Object rfidObj = row.get("rfid");
                                String rfid = rfidObj != null ? String.valueOf(rfidObj) :
                                        (row.get("asset_tag") != null ? String.valueOf(row.get("asset_tag")) : null);

                                if (rfid != null && !rfid.isEmpty()) {
                                    // Use Gson to convert the map into AssetNetwork (if you have that POJO).
                                    AssetNetwork an = gson.fromJson(gson.toJson(row), AssetNetwork.class);
                                    // ensure rfid is set (some rows may have asset_tag only)
                                    assetsByRfid.put(rfid, an);
                                }
                            }
                        }
                    }

                    saveToCache();

                    Snackbar.make(scannerBinding.scannedTagsRecycler, "Fetch Successfully " + assetsByRfid.size() + " assets", Snackbar.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e(TAG, "Parsing assets failed: " + e.getMessage(), e);
                    loadFromCache();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                runOnUiThreadSafe(() -> scannerBinding.scanStatusText.setVisibility(View.GONE));
                Log.w(TAG, "getAllAssetDetails failed: " + t.getMessage(), t);
                loadFromCache();
                Snackbar.make(scannerBinding.scannedTagsRecycler, "Network error — using cached assets", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void saveToCache() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(PREF_KEY_ASSETS, gson.toJson(assetsByRfid));
            ed.apply();
            Log.d(TAG, "Saved assets to cache");
        } catch (Exception e) {
            Log.w(TAG, "saveToCache failed: " + e.getMessage(), e);
        }
    }

    private void loadFromCache() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String assetsJson = prefs.getString(PREF_KEY_ASSETS, null);

            assetsByRfid.clear();

            if (assetsJson != null) {
                TypeToken<java.util.Map<String, com.galla.rfidapp.model.AssetNetwork>> token =
                        new TypeToken<java.util.Map<String, com.galla.rfidapp.model.AssetNetwork>>(){};
                java.lang.reflect.Type mapType = token.getType();
                Map<String, AssetNetwork> cachedAssets = gson.fromJson(assetsJson, mapType);
                if (cachedAssets != null) assetsByRfid.putAll(cachedAssets);
            }

            Log.d(TAG, "Loaded assets from cache: assets=" + assetsByRfid.size());

        } catch (Exception e) {
            Log.e(TAG, "loadFromCache failed: " + e.getMessage(), e);
        } finally {
            runOnUiThreadSafe(() -> scannerBinding.scanStatusText.setVisibility(View.GONE));
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
    private void disableSearchInput() {
        runOnUiThreadSafe(() -> {
            scannerBinding.productSelect.clearFocus();
            scannerBinding.productSelect.setEnabled(false);
            scannerBinding.productSelect.setFocusable(false);
            scannerBinding.productSelect.setFocusableInTouchMode(false);
        });
    }

    private void enableSearchInput() {
        runOnUiThreadSafe(() -> {
            scannerBinding.productSelect.setEnabled(true);
            scannerBinding.productSelect.setFocusable(true);
            scannerBinding.productSelect.setFocusableInTouchMode(true);
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

    @Override
    protected void onSyncClicked() {
        Log.d(TAG, "Sync icon clicked → refreshing Tag Mapping assets");
        loadAssetsAndLocations();   // reload asset cache
    }

}
