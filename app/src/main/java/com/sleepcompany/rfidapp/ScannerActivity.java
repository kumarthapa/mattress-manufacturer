package com.sleepcompany.rfidapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaPlayer;
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
import com.sleepcompany.rfidapp.databinding.ActivityScannerBinding;
import com.sleepcompany.rfidapp.model.ProductNetwork;
import com.sleepcompany.rfidapp.network.ApiClient;
import com.sleepcompany.rfidapp.network.ApiService;
import com.sleepcompany.rfidapp.network.ProductsRequest;
import com.sleepcompany.rfidapp.network.ProductsResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ScannerActivity: bulk tag -> product mapping (updated UI & hardware trigger support)
 */
public class ScannerActivity extends BaseDrawerActivity {

    private static final String TAG = "ScannerActivity";

    // DataBinding instance for this activity (reads BaseDrawerActivity.binding)
    private ActivityScannerBinding scannerBinding;

    private UHFService mDevice;

    // Scanning state
    private volatile boolean isScanning = false;
    private Thread scanThread;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Scanned tags (dedup + ordered list)
    private final Set<String> scannedTags = new HashSet<>();
    private final List<String> scannedTagsList = new ArrayList<>();

    private TagListAdapter tagListAdapter;

    private ArrayAdapter<String> productDropAdapter;
    private final List<ProductNetwork> productSearchResults = new ArrayList<>();

    private ProductNetwork selectedProductForMapping = null;
    private MediaPlayer scanSound;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY_MS = 500;
    private long lastUiUpdateTime = 0;
    private long lastSoundTime = 0;
    private static final int UI_UPDATE_INTERVAL = 150; // ms
    private static final int SOUND_INTERVAL = 120;     // ms


    // -------------------------------------------------------------------------
    // BaseDrawerActivity requirements
    // -------------------------------------------------------------------------
    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_scanner;
    }

    @Override
    protected boolean useDataBinding() {
        return true;
    }

    @Override
    protected String getToolbarTitle() {
        return "Tag Mapping";
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // IMPORTANT: read the binding created in BaseDrawerActivity (DataBindingUtil.setContentView)
        scannerBinding = (ActivityScannerBinding) super.binding;

        // Try initial UHF init (if fails, we stop activity)
        try {
            mDevice = UHFService.getInstance();
            if (mDevice == null) throw new Exception("UHF not present");
        } catch (Exception e) {
            // Use Snackbar to show the error and provide a way to open power settings
            showSnackbarWithSettingsAction("UHF not available on this device. Open power settings?");
            Log.e(TAG, "UHF init failed in onCreate: " + e.getMessage(), e);
            // Do not finish() — allow user to open settings and resume
        }

        initViews();
        setupAdapters();
        setupListeners();
        updateUiState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // try re-init if the hw service was lost earlier
        tryReinitUhfIfNeeded();

        // Try open device if available (best-effort)
        try {
            if (mDevice != null) {
                try { mDevice.open(); } catch (Exception ignored) {}
            }
        } catch (Throwable t) {
            Log.w(TAG, "onResume: failed to open mDevice: " + t.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Views / Adapters / Listeners
    // -------------------------------------------------------------------------
    private void initViews() {
        // Product dropdown should allow typing and show the full selected text (scroll horizontally)
        scannerBinding.productSelect.setInputType(InputType.TYPE_CLASS_TEXT);
        scannerBinding.productSelect.setThreshold(0);
        scannerBinding.productSelect.setSingleLine(true);
        scannerBinding.productSelect.setHorizontallyScrolling(true);

        scannerBinding.scanProgress.setVisibility(View.GONE);
        scannerBinding.scanStatusText.setVisibility(View.GONE);

        scannerBinding.scannedTagsRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupAdapters() {
        tagListAdapter = new TagListAdapter(scannedTagsList,
                // onTagClick: simple info show
                epc -> Toast.makeText(ScannerActivity.this, "Tag: " + epc, Toast.LENGTH_SHORT).show(),
                // onRemoveClick: remove the tag
                this::removeScannedTag
        );
        scannerBinding.scannedTagsRecycler.setAdapter(tagListAdapter);

        productDropAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        scannerBinding.productSelect.setAdapter(productDropAdapter);
    }

    private void setupListeners() {

        scannerBinding.startScanBtn.setOnClickListener(v -> {
            if (!isScanning) startRFIDScan();
            else stopRFIDScan();
        });

        // Map button: we rely on updateUiState() to enable/disable the button.
        // If disabled it won't receive clicks, but we still guard here and do nothing when invalid.
        scannerBinding.mapTagsBtn.setOnClickListener(v -> {
            if (scannedTags.isEmpty() || selectedProductForMapping == null) {
                // Do nothing (no noisy Toast or Snackbar) because button is disabled in normal flow.
                return;
            }
            confirmAndMapScannedTags();
        });

        scannerBinding.productSelect.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                final String q = s == null ? "" : s.toString().trim();
                searchRunnable = () -> searchProducts(q);
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // Reliable product selection handling
        scannerBinding.productSelect.setOnItemClickListener((parent, view, position, id) -> {
            String display = (String) parent.getItemAtPosition(position);
            selectedProductForMapping = null;
            for (ProductNetwork p : productSearchResults) {
                String label = p.getProductName() + " | " + p.getProductCode();
                if (label.equals(display) || p.getProductName().equalsIgnoreCase(display) || p.getProductCode().equalsIgnoreCase(display)) {
                    selectedProductForMapping = p;
                    break;
                }
            }

            // set text without filtering again (prevents dropdown re-filter/truncation)
            try {
                // second parameter 'false' prevents filtering
                scannerBinding.productSelect.setText(display, false);
                scannerBinding.productSelect.setSelection(display != null ? display.length() : 0);
            } catch (Throwable t) {
                // fallback (should rarely be needed)
                scannerBinding.productSelect.setText(display);
            }

            Log.d(TAG, "Product selected -> " + (selectedProductForMapping != null ? selectedProductForMapping.getProductCode() : "null"));
            // ensure UI updates on main thread
            mainHandler.post(this::updateUiState);
        });

        scannerBinding.clearTagsBtn.setOnClickListener(v -> {
            synchronized (scannedTags) {
                scannedTags.clear();
                scannedTagsList.clear();
            }
            tagListAdapter.notifyDataSetChanged();
            updateUiState();
        });
    }

    // -------------------------------------------------------------------------
    // remove tag helper
    // -------------------------------------------------------------------------
    private void removeScannedTag(String epc) {
        synchronized (scannedTags) {
            scannedTags.remove(epc);
            scannedTagsList.remove(epc);
        }
        tagListAdapter.notifyDataSetChanged();
        updateUiState();
        Snackbar.make(scannerBinding.scannedTagsRecycler, "Removed " + epc, Snackbar.LENGTH_SHORT).show();
    }

    // -------------------------------------------------------------------------
    // RFID scan loop (with recovery attempt)
    // -------------------------------------------------------------------------
    private void startRFIDScan() {
        if (mDevice == null) {
            showSnackbarWithSettingsAction("UHF not available. Open power settings?");
            tryReinitUhfIfNeeded();
            return;
        }

        isScanning = true;
        scannerBinding.startScanBtn.setText("Stop Scan");
        scannerBinding.scanProgress.setVisibility(View.VISIBLE);
        scannerBinding.scanStatusText.setText("Scanning...");
        scannerBinding.scanStatusText.setVisibility(View.VISIBLE);

        try { scanSound = MediaPlayer.create(this, R.raw.scan); } catch (Exception ignore) {}

        scanThread = new Thread(() -> {
            while (isScanning) {
                try {
                    EPC epc = new EPC();

                    if (mDevice.inventoryOnce(epc, 120)) {

                        String epcId = epc.getId();
                        if (epcId != null && !epcId.isEmpty()) {

                            boolean isNew;
                            synchronized (scannedTags) {
                                isNew = scannedTags.add(epcId);
                                if (isNew) scannedTagsList.add(0, epcId);
                            }

                            if (isNew) {
                                long now = System.currentTimeMillis();

                                // Throttle UI update
                                if (now - lastUiUpdateTime > UI_UPDATE_INTERVAL) {
                                    lastUiUpdateTime = now;
                                    mainHandler.post(() -> {
                                        tagListAdapter.notifyDataSetChanged();
                                        updateUiState();
                                    });
                                }

                                // Throttle sound
                                if (scanSound != null && now - lastSoundTime > SOUND_INTERVAL) {
                                    lastSoundTime = now;
                                    try { scanSound.start(); } catch (Exception ignore) {}
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "inventoryOnce error: " + e.getMessage(), e);

                    tryReinitUhfIfNeeded();

                    if (mDevice == null) {
                        mainHandler.post(() -> {
                            showSnackbarWithSettingsAction("UHF error — scanner stopped. Open power settings?");
                            stopRFIDScan();
                        });
                        break;
                    }

                    try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                }

                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }
        });

        scanThread.start();
    }


    private void stopRFIDScan() {
        isScanning = false;
        if (scanThread != null) {
            scanThread.interrupt();
            scanThread = null;
        }

        scannerBinding.startScanBtn.setText("Start Scan");
        scannerBinding.scanProgress.setVisibility(View.GONE);
        scannerBinding.scanStatusText.setVisibility(View.GONE);
        updateUiState();
    }

    /**
     * Hardware trigger support:
     * Listen for common keys fired by gun triggers and gamepad buttons.
     * Add any vendor-specific codes as needed.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        Log.d(TAG, "Hardware key pressed: " + keyCode);

        // Keep your device-specific code (142) and also common keys
        if (keyCode == 142
                || keyCode == KeyEvent.KEYCODE_F1
                || keyCode == KeyEvent.KEYCODE_F2
                || keyCode == KeyEvent.KEYCODE_BUTTON_R1
                || keyCode == KeyEvent.KEYCODE_BUTTON_A) {

            if (!isScanning) startRFIDScan();
            else stopRFIDScan();

            return true; // consume event
        }

        return super.onKeyDown(keyCode, event);
    }

    // -------------------------------------------------------------------------
    // Product search
    // -------------------------------------------------------------------------
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
                        // show dropdown and ensure width covers screen
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

    // -------------------------------------------------------------------------
    // Map tags -> product
    // -------------------------------------------------------------------------
    private void confirmAndMapScannedTags() {
        String productLabel = selectedProductForMapping != null ? selectedProductForMapping.getProductName() : "—";
        new AlertDialog.Builder(this)
                .setTitle("Confirm mapping")
                .setMessage("Map " + scannedTags.size() + " tags to product:\n" + productLabel)
                .setPositiveButton("Map", (d, w) -> mapScannedTagsToProduct())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void mapScannedTagsToProduct() {
        if (selectedProductForMapping == null || scannedTagsList.isEmpty()) {
            return;
        }

        // Convert scannedTagsList → epc_codes
        List<String> epcCodes = new ArrayList<>();
        synchronized (scannedTags) {
            for (String epc : scannedTagsList) {
                if (epc == null) continue;
                epc = epc.trim();
                if (epc.isEmpty()) continue;
                epcCodes.add(epc);
            }
        }

        if (epcCodes.isEmpty()) {
            Snackbar.make(scannerBinding.scannedTagsRecycler, "No valid tags to map", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Build payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("product_id", selectedProductForMapping.getId());
        payload.put("epc_codes", epcCodes);

        // Debug log
        try {
            String json = new com.google.gson.Gson().toJson(payload);
            Log.d(TAG, "tagMapping payload: " + json);
        } catch (Exception ignored) {}

        scannerBinding.scanStatusText.setText("Mapping tags...");
        scannerBinding.scanStatusText.setVisibility(View.VISIBLE);

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.tagMapping(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                scannerBinding.scanStatusText.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    Snackbar.make(scannerBinding.scannedTagsRecycler,
                            "Mapping failed: HTTP " + response.code(), Snackbar.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> body = response.body();

                // Top-level success
                boolean topSuccess = false;
                Object ok = body.get("success");
                if (ok instanceof Boolean) topSuccess = (Boolean) ok;
                else topSuccess = "1".equals(String.valueOf(ok));

                // Read server message
                String serverMessage = "";
                if (body.get("message") != null) {
                    serverMessage = String.valueOf(body.get("message"));
                }

                // Per-tag processing
                boolean anySuccess = false;
                boolean anyFailure = false;

                Object resultsObj = body.get("results");
                if (resultsObj instanceof List<?>) {
                    List<?> results = (List<?>) resultsObj;

                    for (Object item : results) {
                        if (item instanceof Map<?, ?>) {
                            Map<String, Object> row = (Map<String, Object>) item;

                            boolean rowSuccess = false;
                            Object rowOk = row.get("success");
                            if (rowOk instanceof Boolean) rowSuccess = (Boolean) rowOk;
                            else rowSuccess = "1".equals(String.valueOf(rowOk));

                            if (rowSuccess) {
                                anySuccess = true;
                            } else {
                                anyFailure = true;
                                String epc = String.valueOf(row.get("epc_code"));
                                String msg = String.valueOf(row.get("message"));
                                Snackbar.make(scannerBinding.scannedTagsRecycler,
                                        epc + ": " + msg, Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    }
                }

                // Decide final top message
                if (!anySuccess && anyFailure) {
                    // All failed
                    Snackbar.make(scannerBinding.scannedTagsRecycler,
                            "Tags are already mapped. ", Snackbar.LENGTH_LONG).show();
                    return;
                }

                if (anySuccess && anyFailure) {
                    // Mixed
                    Snackbar.make(scannerBinding.scannedTagsRecycler,
                            "Some tags mapped. Some duplicates skipped.", Snackbar.LENGTH_LONG).show();
                } else if (anySuccess) {
                    // All good
                    Snackbar.make(scannerBinding.scannedTagsRecycler,
                            serverMessage.isEmpty() ? "Mapping completed" : serverMessage,
                            Snackbar.LENGTH_SHORT).show();
                }

                // If there was any success → clear UI
                if (anySuccess) {
                    synchronized (scannedTags) {
                        scannedTags.clear();
                        scannedTagsList.clear();
                    }
                    tagListAdapter.notifyDataSetChanged();
                    updateUiState();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                scannerBinding.scanStatusText.setVisibility(View.GONE);
                Snackbar.make(scannerBinding.scannedTagsRecycler,
                        "Network error: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }


    // -------------------------------------------------------------------------
    // Lifecycle helpers
    // -------------------------------------------------------------------------
    @Override protected void onPause() {
        super.onPause();
        stopRFIDScan();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        stopRFIDScan();
        if (scanSound != null) {
            try { scanSound.release(); } catch (Exception ignore) {}
        }
    }

    private void updateUiState() {

        long now = System.currentTimeMillis();

        // 🔥 Debounce: update UI only once every 150ms during scanning
        if (isScanning && (now - lastUiUpdateTime) < 150) {
            return;
        }
        lastUiUpdateTime = now;

        // --- Normal UI update logic ---
        scannerBinding.tvScannedCount.setText(String.valueOf(scannedTags.size()));

        boolean enabled = !scannedTags.isEmpty() && selectedProductForMapping != null;

        // Prevent unnecessary re-enable/disable calls
        if (scannerBinding.mapTagsBtn.isEnabled() != enabled) {
            scannerBinding.mapTagsBtn.setEnabled(enabled);
        }

        boolean clearEnabled = !scannedTags.isEmpty();
        if (scannerBinding.clearTagsBtn.isEnabled() != clearEnabled) {
            scannerBinding.clearTagsBtn.setEnabled(clearEnabled);
        }

        Log.d(TAG, "updateUiState -> scannedTags=" + scannedTags.size()
                + ", selectedProduct=" + (selectedProductForMapping != null ? selectedProductForMapping.getProductCode() : "null")
                + ", mapEnabled=" + enabled);
    }


    // -------------------------------------------------------------------------
    // Recycler adapters
    // -------------------------------------------------------------------------
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

        @Override public int getItemCount() { return tags.size(); }
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

    // Add this logger-friendly helper to try reinitializing the hardware service
    private void tryReinitUhfIfNeeded() {
        try {
            if (mDevice == null) {
                Log.d(TAG, "Attempting to re-init UHFService...");
                // Simple re-get instance
                mDevice = UHFService.getInstance();
                if (mDevice != null) {
                    Log.d(TAG, "UHFService re-initialized successfully.");
                    Snackbar.make(scannerBinding.scannedTagsRecycler, "UHF re-initialized", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                // If still null, try to open power settings prompt
                Log.w(TAG, "UHFService.getInstance() returned null during re-init.");
                showSnackbarWithSettingsAction("UHF service not running. Open power settings?");
            } else {
                // mDevice exists: you may call vendor-specific checks here (wrapped in try/catch)
                try {
                    // Some SDKs provide a test call; we wrap it and ignore if unavailable
                    EPC e = new EPC();
                    boolean ok = mDevice.inventoryOnce(e, 50);
                    Log.d(TAG, "UHF test inventoryOnce -> " + ok);
                } catch (Throwable t) {
                    Log.w(TAG, "UHFService test call failed, attempting re-get instance: " + t.getMessage(), t);
                    // try re-get instance
                    try {
                        mDevice = UHFService.getInstance();
                        if (mDevice != null) Log.d(TAG, "UHFService recovered after test failure.");
                        else showSnackbarWithSettingsAction("UHF error — open power settings?");
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

    /**
     * Show a snackbar that suggests opening battery / power settings and provides an action.
     */
    private void showSnackbarWithSettingsAction(String message) {
        mainHandler.post(() -> {
            Snackbar snackbar = Snackbar.make(scannerBinding.scannedTagsRecycler, message, Snackbar.LENGTH_LONG);
            snackbar.setAction("Open power settings", v -> openPowerSettings());
            snackbar.show();
        });
    }

    /**
     * Open the device's battery optimization / power settings page to let user whitelist the app.
     */
    private void openPowerSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Open the system battery optimization settings screen
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            } else {
                // Fallback: open app details
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open power settings: " + e.getMessage(), e);
            Snackbar.make(scannerBinding.scannedTagsRecycler, "Unable to open settings", Snackbar.LENGTH_SHORT).show();
        }
    }
}
