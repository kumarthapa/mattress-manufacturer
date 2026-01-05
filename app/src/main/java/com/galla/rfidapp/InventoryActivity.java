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
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textview.MaterialTextView;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;
import com.galla.rfidapp.databinding.ActivityInventoryBinding;
import com.galla.rfidapp.network.ApiClient;
import com.galla.rfidapp.network.ApiService;
import com.galla.rfidapp.util.SyncManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
/**
 * InventoryActivity - Modified for auditing:
 * - loads assets & locations on start
 * - saves them locally (SharedPreferences)
 * - requires location selection before scanning
 * - removes inward/outward and provides Update Audit button
 */
public class InventoryActivity extends BaseDrawerActivity {

    private long lastSeenSyncVersion = 0;

    private static final String TAG = "InventoryActivity";
    private static final String PREFS_NAME = "galla_inventory_cache";
    private static final String PREF_KEY_ASSETS = "cached_assets_v1";
    private static final String PREF_KEY_LOCATIONS = "cached_locations_v1";

    private ActivityInventoryBinding binding;
    private UHFService mDevice;

    // scanning state
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private Thread scanThread;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // continuous mode flag (thread-visible)
    private volatile boolean continuousMode = false;

    // sound
    private SoundPool soundPool;
    private int soundScanId = 0;
    private long lastSoundTime = 0;

    // membership set (fast checks)
    private final HashSet<String> scannedSet = new HashSet<>();

    // adapter backing list (ordered newest-first) - scanned tags
    private final List<String> scannedList = new ArrayList<>();

    // queue for new tags produced by scan thread (lock-free)
    private final Queue<String> newTagsQueue = new ConcurrentLinkedQueue<>();

    private TagListAdapter tagListAdapter;

    // master assets and locations loaded from API (or cache)
    // assetsByRfid: key = rfid string (EPC), value = asset record map
    private final Map<String, Map<String, Object>> assetsByRfid = new HashMap<>();
    // locations list (order preserved)
    private final List<Map<String, Object>> locationsList = new ArrayList<>();
    // simple array of location names for dropdown
    private final List<String> locationNames = new ArrayList<>();

    // current selected location id (null if none)
    private Integer selectedLocationId = null;

    // tuning constants
    private static final int UI_UPDATE_INTERVAL_MS = 100;
    private static final int INVENTORY_TIMEOUT_MS = 50;
    private static final int SCAN_LOOP_SLEEP_MS = 2;
    private static final int SOUND_THROTTLE_MS = 60;

    private long lastUiUpdateTime = 0;
    private final Gson gson = new Gson();

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_inventory; }

    @Override
    protected boolean useDataBinding() { return true; }

    @Override
    protected String getToolbarTitle() { return "Assets Audit"; }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = (ActivityInventoryBinding) super.binding;

        initUhfService();
        initSound();
        initViews();
        setupAdapters();
        setupListeners();

        // load assets and locations (from API, fallback to cache)
        loadAssetsAndLocations();

        // start UI flusher (runs periodically; no-op when queue empty)
        mainHandler.post(uiFlushRunnable);

        updateUiState();
    }

    // ---------------- initialization ----------------

    private void initUhfService() {
        try {
            mDevice = UHFService.getInstance();
            if (mDevice == null) throw new Exception("UHF not present");
        } catch (Exception e) {
            showSnackbarWithSettingsAction("UHF not available on this device. Open power settings?");
            Log.e(TAG, "UHF init failed in onCreate: " + e.getMessage(), e);
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
        binding.scanProgress.setVisibility(View.GONE);
        binding.scanStatusText.setVisibility(View.GONE);
        binding.scannedTagsRecycler.setLayoutManager(new LinearLayoutManager(this));
        // ensure switch default
        binding.modeSwitch.setChecked(false);

        // hide inward & outward (audit flow)
//        try { binding.inwardBtn.setVisibility(View.GONE); } catch (Exception ignored) {}
//        try { binding.outwardBtn.setVisibility(View.GONE); } catch (Exception ignored) {}

        // ensure updateAuditBtn exists (added in layout) and initially disabled
        try {
            binding.updateAuditBtn.setVisibility(View.VISIBLE);
            binding.updateAuditBtn.setEnabled(false);
        } catch (Exception ignored) {}
    }

    private void setupAdapters() {
        // Adapter uses the scannedList reference directly
        tagListAdapter = new TagListAdapter(
                scannedList,
                assetsByRfid,   // ✅ REQUIRED
                epc -> Toast.makeText(this, "Tag: " + epc, Toast.LENGTH_SHORT).show(),
                this::removeScannedTag
        );
        binding.scannedTagsRecycler.setAdapter(tagListAdapter);
    }


    private void setupListeners() {
        // CONTINUOUS MODE SWITCH
        binding.modeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            continuousMode = isChecked;
            // restart scanning with new mode if currently scanning
            if (isScanning.get()) {
                stopRFIDScan();
                startRFIDScan();
            }
            Snackbar.make(binding.scannedTagsRecycler,
                    isChecked ? "Continuous Mode Enabled" : "Normal Mode Enabled",
                    Snackbar.LENGTH_SHORT).show();
        });

        binding.startScanBtn.setOnClickListener(v -> {
            // require location selected before scanning
            if (selectedLocationId == null || selectedLocationId <= 0) {
                Snackbar.make(binding.scannedTagsRecycler, "Select a location first", Snackbar.LENGTH_SHORT).show();
                return;
            }

            if (!isScanning.get()) startRFIDScan();
            else stopRFIDScan();
        });

        binding.clearTagsBtn.setOnClickListener(v -> clearAllScanned());

        // Update audit button - replaces inward/outward
        try {
            binding.updateAuditBtn.setOnClickListener(v -> {
                if (scannedList.isEmpty()) {
                    Snackbar.make(binding.scannedTagsRecycler, "No tags to audit", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                confirmAndPerformAudit();
            });
        } catch (Exception ignored) {}

        // Location dropdown listener (MaterialAutoCompleteTextView)
        try {
            MaterialAutoCompleteTextView dropdown = binding.locationDropdown;
            dropdown.setOnItemClickListener((parent, view, position, id) -> {
                String name = (String) parent.getItemAtPosition(position);
                selectedLocationId = findLocationIdByName(name);
                runOnUiThreadSafe(this::updateUiState);
                Snackbar.make(binding.scannedTagsRecycler, "Selected: " + name, Snackbar.LENGTH_SHORT).show();
            });
        } catch (Exception ignored) {}
    }

    private int findLocationIdByName(String name) {
        for (Map<String, Object> loc : locationsList) {
            Object oName = loc.get("name");
            if (oName != null && name.equals(String.valueOf(oName))) {
                Object idObj = loc.get("id");
                if (idObj instanceof Number) return ((Number) idObj).intValue();
                try { return Integer.parseInt(String.valueOf(idObj)); } catch (Exception ex) { return -1; }
            }
        }
        return -1;
    }

    // ---------------- load & cache assets/locations ----------------

    private void loadAssetsAndLocations() {
        runOnUiThreadSafe(() -> {
            binding.scanStatusText.setText("Loading assets & locations...");
            binding.scanStatusText.setVisibility(View.VISIBLE);
        });

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        Call<Map<String, Object>> call = api.getAllAssetDetails();
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                runOnUiThreadSafe(() -> binding.scanStatusText.setVisibility(View.GONE));

                if (!response.isSuccessful() || response.body() == null) {
                    loadFromCache();
                    Snackbar.make(binding.scannedTagsRecycler, "Failed to fetch live data — using cache", Snackbar.LENGTH_LONG).show();
                    return;
                }

                try {
                    Map<String, Object> body = response.body();
                    Object assetsObj = body.get("assets");
                    Object locationsObj = body.get("locations");

                    // parse and store assets
                    assetsByRfid.clear();
                    if (assetsObj instanceof List<?>) {
                        List<?> assets = (List<?>) assetsObj;
                        for (Object o : assets) {
                            if (o instanceof Map<?, ?>) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> row = (Map<String, Object>) o;
                                Object rfidObj = row.get("rfid");
                                String rfid = rfidObj != null ? String.valueOf(rfidObj) :
                                        (row.get("asset_tag") != null ? String.valueOf(row.get("asset_tag")) : null);
                                if (rfid != null && !rfid.isEmpty()) {
                                    assetsByRfid.put(rfid, row);
                                }
                            }
                        }
                    }

                    // parse and store locations
                    locationsList.clear();
                    locationNames.clear();
                    if (locationsObj instanceof List<?>) {
                        List<?> locations = (List<?>) locationsObj;
                        for (Object o : locations) {
                            if (o instanceof Map<?, ?>) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> loc = (Map<String, Object>) o;
                                locationsList.add(loc);
                                Object nameObj = loc.get("name");
                                locationNames.add(nameObj != null ? String.valueOf(nameObj) : "Unknown");
                            }
                        }
                    }

                    // save to cache
                    saveToCache();

                    // bind locations to dropdown
                    runOnUiThreadSafe(() -> bindLocationsDropdown());

                    Snackbar.make(binding.scannedTagsRecycler, "Fetch Successfully " + assetsByRfid.size() + " assets and " + locationsList.size() + " locations", Snackbar.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e(TAG, "Parsing assets/locations failed: " + e.getMessage(), e);
                    loadFromCache();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                runOnUiThreadSafe(() -> binding.scanStatusText.setVisibility(View.GONE));
                Log.w(TAG, "getAllAssetDetails failed: " + t.getMessage(), t);
                loadFromCache();
                Snackbar.make(binding.scannedTagsRecycler, "Network error — using cached data", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void bindLocationsDropdown() {
        try {
            MaterialAutoCompleteTextView dropdown = binding.locationDropdown;
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, locationNames);
            dropdown.setAdapter(adapter);
            dropdown.setThreshold(1);
            dropdown.setText("", false); // no pre-selection
            selectedLocationId = null;
            updateUiState();
        } catch (Exception e) {
            Log.w(TAG, "bindLocationsDropdown failed: " + e.getMessage(), e);
        }
    }

    private void saveToCache() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(PREF_KEY_ASSETS, gson.toJson(assetsByRfid));
            ed.putString(PREF_KEY_LOCATIONS, gson.toJson(locationsList));
            ed.apply();
            Log.d(TAG, "Saved assets & locations to cache");
        } catch (Exception e) {
            Log.w(TAG, "saveToCache failed: " + e.getMessage(), e);
        }
    }

    private void loadFromCache() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String assetsJson = prefs.getString(PREF_KEY_ASSETS, null);
            String locationsJson = prefs.getString(PREF_KEY_LOCATIONS, null);

            assetsByRfid.clear();
            locationsList.clear();
            locationNames.clear();

            if (assetsJson != null) {
                Type mapType = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
                Map<String, Map<String, Object>> cachedAssets = gson.fromJson(assetsJson, mapType);
                if (cachedAssets != null) assetsByRfid.putAll(cachedAssets);
            }

            if (locationsJson != null) {
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> cachedLocations = gson.fromJson(locationsJson, listType);
                if (cachedLocations != null) {
                    locationsList.addAll(cachedLocations);
                    for (Map<String, Object> loc : cachedLocations) {
                        Object nameObj = loc.get("name");
                        locationNames.add(nameObj != null ? String.valueOf(nameObj) : "Unknown");
                    }
                }
            }

            // bind dropdown after loading cache
            runOnUiThreadSafe(this::bindLocationsDropdown);

            Log.d(TAG, "Loaded assets & locations from cache: assets=" + assetsByRfid.size() + " locs=" + locationsList.size());
        } catch (Exception e) {
            Log.e(TAG, "loadFromCache failed: " + e.getMessage(), e);
        } finally {
            runOnUiThreadSafe(() -> binding.scanStatusText.setVisibility(View.GONE));
        }
    }

    // ---------------- scanning / queue / ui flush ----------------

    private void clearAllScanned() {
        synchronized (scannedSet) {
            scannedSet.clear();
            scannedList.clear();
            newTagsQueue.clear();
        }
        runOnUiThreadSafe(() -> {
            tagListAdapter.notifyDataSetChanged();
            updateUiState();
        });
    }

    private void removeScannedTag(String epc) {
        synchronized (scannedSet) {
            scannedSet.remove(epc);
            int pos = scannedList.indexOf(epc);
            if (pos >= 0) {
                scannedList.remove(pos);
                final int removedPos = pos;
                runOnUiThreadSafe(() -> {
                    try {
                        tagListAdapter.notifyItemRemoved(removedPos);
                        updateUiState();
                    } catch (Exception e) {
                        tagListAdapter.notifyDataSetChanged();
                        updateUiState();
                    }
                    Snackbar.make(binding.scannedTagsRecycler, "Removed " + epc, Snackbar.LENGTH_SHORT).show();
                });
                return;
            }
        }

        runOnUiThreadSafe(() -> {
            tagListAdapter.notifyDataSetChanged();
            updateUiState();
            Snackbar.make(binding.scannedTagsRecycler, "Removed " + epc, Snackbar.LENGTH_SHORT).show();
        });
    }

    private void startRFIDScan() {
        if (mDevice == null) {
            showSnackbarWithSettingsAction("UHF not available. Open power settings?");
            initUhfService();
            return;
        }

        isScanning.set(true);
        runOnUiThreadSafe(() -> {
            binding.startScanBtn.setText("Stop Scan");
            binding.scanProgress.setVisibility(View.VISIBLE);
            binding.scanStatusText.setText("Scanning...");
            binding.scanStatusText.setVisibility(View.VISIBLE);
        });

        if (continuousMode) {
            try {
                mDevice.inventoryStart();
                Log.d(TAG, "Continuous inventoryStart invoked");
            } catch (Exception e) {
                Log.e(TAG, "inventoryStart failed: " + e.getMessage(), e);
            }
        }

        scanThread = new Thread(this::scanLoop, "InventoryScanThread");
        scanThread.start();
    }

    private void scanLoop() {

        final long SCAN_TIMEOUT_MS = 3000L; // 🔥 3 seconds hard limit
        final long endTime = System.currentTimeMillis() + SCAN_TIMEOUT_MS;

        EPC reusable = new EPC();

        try {
            while (isScanning.get()
                    && System.currentTimeMillis() < endTime
                    && !Thread.currentThread().isInterrupted()) {

                try {

                    if (continuousMode) {

                        List<EPC> list = null;
                        try {
                            list = mDevice.getTagIDs();
                        } catch (Throwable t) {
                            Log.w(TAG, "getTagIDs failed: " + t.getMessage(), t);
                            tryReinitUhfIfNeeded();
                        }

                        if (list != null && !list.isEmpty()) {
                            for (EPC tag : list) {
                                String epcId = tag.getId();
                                if (epcId == null || epcId.isEmpty()) continue;

                                boolean isNew;
                                synchronized (scannedSet) {
                                    isNew = scannedSet.add(epcId); // ✅ VALID for HashSet
                                }

                                if (isNew) {
                                    newTagsQueue.add(epcId);
                                }
                            }
                        }

                        Thread.sleep(30);

                    } else {

                        boolean found = mDevice.inventoryOnce(reusable, INVENTORY_TIMEOUT_MS);
                        if (found) {
                            String epcId = reusable.getId();
                            if (epcId != null && !epcId.isEmpty()) {

                                boolean isNew;
                                synchronized (scannedSet) {
                                    isNew = scannedSet.add(epcId); // ✅ VALID
                                }

                                if (isNew) {
                                    newTagsQueue.add(epcId);
                                }
                            }
                        }

                        Thread.sleep(SCAN_LOOP_SLEEP_MS);
                    }

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;

                } catch (Throwable t) {
                    Log.e(TAG, "scan error: " + t.getMessage(), t);
                    tryReinitUhfIfNeeded();

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

        } finally {

            try {
                if (continuousMode && mDevice != null) {
                    mDevice.inventoryStop();
                }
            } catch (Exception ignored) {}

            isScanning.set(false);

            runOnUiThreadSafe(() -> {
                binding.scanProgress.setVisibility(View.GONE);
                binding.scanStatusText.setVisibility(View.GONE);
                binding.startScanBtn.setText("Start Scan");
                updateUiState();

                Snackbar.make(
                        binding.scannedTagsRecycler,
                        "Scan stopped (3s timeout)",
                        Snackbar.LENGTH_SHORT
                ).show();
            });
        }
    }




    private final Runnable uiFlushRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (!newTagsQueue.isEmpty()) {
                    List<String> batch = new ArrayList<>();
                    long now = System.currentTimeMillis();
                    while (!newTagsQueue.isEmpty()) {
                        String epc = newTagsQueue.poll();
                        if (epc == null) break;
                        batch.add(epc);
                        if (batch.size() >= 200) break;
                    }

                    if (!batch.isEmpty()) {
                        synchronized (scannedList) {
                            Collections.reverse(batch);
                            scannedList.addAll(0, batch);
                        }

                        // mark found items if they are present in assetsByRfid
                        for (String epc : batch) {
                            if (assetsByRfid.containsKey(epc)) {
                                Map<String, Object> asset = assetsByRfid.get(epc);
                                asset.put("_found", true);
                            }
                        }

                        if (soundPool != null && soundScanId != 0 && now - lastSoundTime > SOUND_THROTTLE_MS) {
                            try { soundPool.play(soundScanId, 1f, 1f, 1, 0, 1f); } catch (Exception ignore) {}
                            lastSoundTime = now;
                        }

                        final int inserted = batch.size();
                        runOnUiThreadSafe(() -> {
                            try {
                                tagListAdapter.notifyItemRangeInserted(0, inserted);
                                binding.scannedTagsRecycler.scrollToPosition(0);
                            } catch (Exception e) {
                                tagListAdapter.notifyDataSetChanged();
                            }
                            updateUiState();
                        });
                    }
                } else {
                    if (isScanning.get()) {
                        long now = System.currentTimeMillis();
                        if (now - lastUiUpdateTime > UI_UPDATE_INTERVAL_MS * 4) {
                            runOnUiThreadSafe(InventoryActivity.this::updateUiState);
                        }
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "uiFlushRunnable error: " + t.getMessage(), t);
            } finally {
                mainHandler.postDelayed(this, UI_UPDATE_INTERVAL_MS);
            }
        }
    };

    private void stopRFIDScan() {
        isScanning.set(false);

        try {
            if (continuousMode && mDevice != null) {
                mDevice.inventoryStop();
                Log.d(TAG, "inventoryStop invoked in stopRFIDScan");
            }
        } catch (Exception e) {
            Log.w(TAG, "inventoryStop failed in stopRFIDScan: " + e.getMessage());
        }

        if (scanThread != null) {
            scanThread.interrupt();
            scanThread = null;
        }

        runOnUiThreadSafe(() -> {
            binding.startScanBtn.setText("Start Scan");
            binding.scanProgress.setVisibility(View.GONE);
            binding.scanStatusText.setVisibility(View.GONE);
            updateUiState();
        });
    }

    // ---------------- audit submission ----------------

    private void confirmAndPerformAudit() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Audit")
                .setMessage("Submit audit for " + scannedList.size() + " scanned tags at selected location?")
                .setCancelable(false)
                .setPositiveButton("Proceed", (dialog, which) -> {
                    dialog.dismiss();
                    performAuditOperation();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performAuditOperation() {

        // 1️⃣ Validate location
        if (selectedLocationId == null || selectedLocationId <= 0) {
            Snackbar.make(binding.scannedTagsRecycler,
                    "Select a valid location first",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        // 2️⃣ Build ARRAY payload (backend expects array)
        List<Map<String, Object>> payloadList = new ArrayList<>();

        synchronized (scannedList) {
            for (String epc : scannedList) {
                if (epc == null) continue;

                String rfid = epc.trim();
                if (rfid.isEmpty()) continue;

                // asset_tag is REQUIRED by backend
                if (!assetsByRfid.containsKey(rfid)) {
                    Log.w(TAG, "RFID not found in cache: " + rfid);
                    continue;
                }

                Object assetTag = assetsByRfid.get(rfid).get("asset_tag");
                if (assetTag == null) continue;

                Map<String, Object> item = new HashMap<>();
                item.put("asset_tag", String.valueOf(assetTag));
                item.put("rfid", rfid);
                item.put("location_id", selectedLocationId);

                payloadList.add(item);
            }
        }

        if (payloadList.isEmpty()) {
            Snackbar.make(binding.scannedTagsRecycler,
                    "No valid assets to submit",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Audit payload FINAL: " + gson.toJson(payloadList));

        // 3️⃣ Lock UI
        runOnUiThreadSafe(() -> {
            binding.scanStatusText.setText("Submitting audit...");
            binding.scanStatusText.setVisibility(View.VISIBLE);
            binding.updateAuditBtn.setEnabled(false);
            binding.startScanBtn.setEnabled(false);
            binding.clearTagsBtn.setEnabled(false);
        });

        // 4️⃣ Call API (ARRAY BODY)
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        Call<Map<String, Object>> call = api.assetsAudit(payloadList);

        call.enqueue(new Callback<Map<String, Object>>() {

            @Override
            public void onResponse(Call<Map<String, Object>> call,
                                   Response<Map<String, Object>> response) {

                runOnUiThreadSafe(() ->
                        binding.scanStatusText.setVisibility(View.GONE));

                if (!response.isSuccessful() || response.body() == null) {
                    Snackbar.make(binding.scannedTagsRecycler,
                            "Audit failed: HTTP " + response.code(),
                            Snackbar.LENGTH_SHORT).show();
                    restoreButtonsAfterSubmit();
                    return;
                }

                Map<String, Object> body = response.body();
                boolean success =
                        "success".equalsIgnoreCase(String.valueOf(body.get("status")));

                if (success) {
                    scannedSet.clear();
                    scannedList.clear();
                    newTagsQueue.clear();

                    runOnUiThreadSafe(() -> {
                        tagListAdapter.notifyDataSetChanged();
                        updateUiState();
                        Snackbar.make(binding.scannedTagsRecycler,
                                "Audit completed successfully",
                                Snackbar.LENGTH_SHORT).show();
                    });
                } else {
                    String msg = body.get("message") != null
                            ? String.valueOf(body.get("message"))
                            : "Audit failed";
                    Snackbar.make(binding.scannedTagsRecycler,
                            msg,
                            Snackbar.LENGTH_LONG).show();
                }

                restoreButtonsAfterSubmit();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                runOnUiThreadSafe(() -> {
                    binding.scanStatusText.setVisibility(View.GONE);
                    Snackbar.make(binding.scannedTagsRecycler,
                            "Network error: " + t.getMessage(),
                            Snackbar.LENGTH_SHORT).show();
                    restoreButtonsAfterSubmit();
                });
            }
        });
    }


    private void restoreButtonsAfterSubmit() {
        runOnUiThreadSafe(() -> {
            boolean hasTags = !scannedList.isEmpty();
            boolean locationSelected = (selectedLocationId != null && selectedLocationId > 0);
            try { binding.updateAuditBtn.setEnabled(hasTags && locationSelected); } catch (Exception ignored) {}
            binding.startScanBtn.setEnabled(locationSelected);
            binding.clearTagsBtn.setEnabled(hasTags);
        });
    }

    // ---------------- lifecycle / helpers ----------------

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



    private void updateUiState() {
        long now = System.currentTimeMillis();
        if (isScanning.get() && (now - lastUiUpdateTime) < UI_UPDATE_INTERVAL_MS) {
            return;
        }
        lastUiUpdateTime = now;

        runOnUiThreadSafe(() -> {
            binding.tvScannedCount.setText(String.valueOf(scannedList.size()));

            boolean hasTags = !scannedList.isEmpty();
            boolean locationSelected = (selectedLocationId != null && selectedLocationId > 0);

            binding.startScanBtn.setEnabled(locationSelected);
            try { binding.updateAuditBtn.setEnabled(hasTags && locationSelected); } catch (Exception ignored) {}

            boolean clearEnabled = hasTags;
            if (binding.clearTagsBtn.isEnabled() != clearEnabled) {
                binding.clearTagsBtn.setEnabled(clearEnabled);
            }
        });
    }

    // TagListAdapter uses the same list reference (scannedList)
    private static class TagListAdapter
            extends RecyclerView.Adapter<TagViewHolder> {

        interface OnTagClick {
            void onTagClicked(String tag);
        }

        interface OnRemoveClick {
            void onRemoveClicked(String tag);
        }

        private final List<String> tags; // RFID list
        private final Map<String, Map<String, Object>> assetsByRfid; // RFID → asset map
        private final OnTagClick clickListener;
        private final OnRemoveClick removeListener;

        TagListAdapter(List<String> tags,
                       Map<String, Map<String, Object>> assetsByRfid,
                       OnTagClick clickListener,
                       OnRemoveClick removeListener) {

            this.tags = tags;
            this.assetsByRfid = assetsByRfid;
            this.clickListener = clickListener;
            this.removeListener = removeListener;
        }

        @NonNull
        @Override
        public TagViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.assets_auditing_tag, parent, false);
            return new TagViewHolder(v);
        }

        @Override
        public void onBindViewHolder(
                @NonNull TagViewHolder holder,
                int position
        ) {
            String epc = tags.get(position);               // RFID
            Map<String, Object> asset = assetsByRfid.get(epc); // may be null

            holder.bind(
                    epc,
                    asset,
                    () -> clickListener.onTagClicked(epc),
                    () -> removeListener.onRemoveClicked(epc)
            );
        }

        @Override
        public int getItemCount() {
            return tags.size();
        }
    }

    private static class TagViewHolder extends RecyclerView.ViewHolder {

        private final MaterialTextView tvAssetTag;
        private final MaterialTextView tvRfid;
        private final ImageButton btnRemove;

        TagViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAssetTag = itemView.findViewById(R.id.tvAssetTag);
            tvRfid = itemView.findViewById(R.id.tvRfid);
            btnRemove = itemView.findViewById(R.id.btnRemoveTag);
        }

        void bind(String epc,
                  Map<String, Object> asset,
                  Runnable onClick,
                  Runnable onRemove) {

            // ✅ Safe asset_tag extraction with visual indicator
            String assetTag;
            if (asset != null && asset.get("asset_tag") != null) {
                assetTag = String.valueOf(asset.get("asset_tag"));
                tvAssetTag.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), android.R.color.black)
                );
            } else {
                assetTag = "Tag Not Mapped";
                tvAssetTag.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark)
                );
            }

            tvAssetTag.setText("Asset Tag : " + assetTag);
            tvRfid.setText("Scan Tag : " + (epc != null ? epc : "--"));

            // ✅ Click listeners (null-safe)
            itemView.setOnClickListener(v -> {
                if (onClick != null) onClick.run();
            });

            btnRemove.setOnClickListener(v -> {
                if (onRemove != null) onRemove.run();
            });
        }
    }




    // Re-init helper
    private void tryReinitUhfIfNeeded() {
        try {
            if (mDevice == null) {
                Log.d(TAG, "Attempting to re-init UHFService...");
                mDevice = UHFService.getInstance();
                if (mDevice != null) {
                    Log.d(TAG, "UHFService re-initialized successfully.");
                    Snackbar.make(binding.scannedTagsRecycler, "UHF re-initialized", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                Log.w(TAG, "UHFService.getInstance() returned null during re-init.");
                showSnackbarWithSettingsAction("UHF service not running. Open power settings?");
            } else {
                try {
                    EPC e = new EPC();
                    boolean ok = mDevice.inventoryOnce(e, 50);
                    Log.d(TAG, "UHF test inventoryOnce -> " + ok);
                } catch (Throwable t) {
                    Log.w(TAG, "UHFService test call failed, attempting re-get instance: " + t.getMessage(), t);
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

    private void showSnackbarWithSettingsAction(String message) {
        mainHandler.post(() -> {
            Snackbar snackbar = Snackbar.make(binding.scannedTagsRecycler, message, Snackbar.LENGTH_LONG);
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
            Snackbar.make(binding.scannedTagsRecycler, "Unable to open settings", Snackbar.LENGTH_SHORT).show();
        }
    }

    // Helpers
    private String getDeviceAndroidId() {
        try {
            String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            return id != null ? id : "unknown_android_id";
        } catch (Exception e) {
            Log.w(TAG, "Failed to read android id: " + e.getMessage());
            return "unknown_android_id";
        }
    }

    private String isoUtcNow() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.format(new Date());
        } catch (Exception e) {
            return String.valueOf(System.currentTimeMillis());
        }
    }

    private void runOnUiThreadSafe(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) r.run();
        else mainHandler.post(r);
    }

    @Override
    protected void onSyncClicked() {
        Log.d(TAG, "Sync icon clicked → refreshing Inventory");
        loadAssetsAndLocations();
    }

    @Override
    protected void onResume() {
        super.onResume();

        long globalVersion = SyncManager.getSyncVersion(this);
        if (globalVersion > lastSeenSyncVersion) {
            lastSeenSyncVersion = globalVersion;
            Log.d(TAG, "Global sync detected → auto refresh Inventory");
            loadAssetsAndLocations();
        }
    }
}
