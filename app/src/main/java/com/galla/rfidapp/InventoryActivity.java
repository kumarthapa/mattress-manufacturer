package com.galla.rfidapp;

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
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;
import com.galla.rfidapp.databinding.ActivityInventoryBinding;
import com.galla.rfidapp.network.ApiClient;
import com.galla.rfidapp.network.ApiService;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * InventoryActivity - High-throughput scanning with batched UI updates
 */
public class InventoryActivity extends BaseDrawerActivity {

    private static final String TAG = "InventoryActivity";

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

    // adapter backing list (ordered newest-first)
    private final List<String> scannedList = new ArrayList<>();

    // queue for new tags produced by scan thread (lock-free)
    private final Queue<String> newTagsQueue = new ConcurrentLinkedQueue<>();

    private TagListAdapter tagListAdapter;

    // tuning constants - tweak per device for max throughput
    private static final int UI_UPDATE_INTERVAL_MS = 100;    // main flush interval (ms)
    private static final int INVENTORY_TIMEOUT_MS = 50;     // inventoryOnce timeout (ms)
    private static final int SCAN_LOOP_SLEEP_MS = 2;        // tiny sleep to yield CPU (ms)
    private static final int SOUND_THROTTLE_MS = 60;        // beep throttle (ms)

    private long lastUiUpdateTime = 0;
    private final Gson gson = new Gson();

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_inventory; }

    @Override
    protected boolean useDataBinding() { return true; }

    @Override
    protected String getToolbarTitle() { return "Inventory"; }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = (ActivityInventoryBinding) super.binding;

        initUhfService();
        initSound();
        initViews();
        setupAdapters();
        setupListeners();

        // start UI flusher (runs periodically; no-op when queue empty)
        mainHandler.post(uiFlushRunnable);

        updateUiState();
    }

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
    }

    private void setupAdapters() {
        // Adapter uses the scannedList reference directly
        tagListAdapter = new TagListAdapter(scannedList,
                epc -> Toast.makeText(InventoryActivity.this, "Tag: " + epc, Toast.LENGTH_SHORT).show(),
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
                // quick restart so mode change applies immediately
                stopRFIDScan();
                startRFIDScan();
            }
            Snackbar.make(binding.scannedTagsRecycler,
                    isChecked ? "Continuous Mode Enabled" : "Normal Mode Enabled",
                    Snackbar.LENGTH_SHORT).show();
        });

        binding.startScanBtn.setOnClickListener(v -> {
            if (!isScanning.get()) startRFIDScan();
            else stopRFIDScan();
        });

        binding.clearTagsBtn.setOnClickListener(v -> {
            clearAllScanned();
        });

        binding.inwardBtn.setOnClickListener(v -> {
            if (scannedList.isEmpty()) return;
            confirmAndPerformInventory("inward");
        });

        binding.outwardBtn.setOnClickListener(v -> {
            if (scannedList.isEmpty()) return;
            confirmAndPerformInventory("outward");
        });
    }

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

    /**
     * Remove a single tag
     */
    private void removeScannedTag(String epc) {
        synchronized (scannedSet) {
            scannedSet.remove(epc);
            int pos = scannedList.indexOf(epc);
            if (pos >= 0) {
                scannedList.remove(pos);
                // notify single remove for smoother animation
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

        // If continuous mode -> start hardware continuous scanning
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

    /**
     * Producer: very lightweight scanning loop
     * - only updates scannedSet and enqueues new EPC into newTagsQueue
     * - avoids touching adapter/backing list on background thread
     */
    private void scanLoop() {
        try {
            EPC reusable = new EPC();
            while (isScanning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    if (continuousMode) {
                        // CONTINUOUS MODE: use getTagIDs() for batches
                        List<EPC> list = null;
                        try {
                            list = mDevice.getTagIDs();
                        } catch (Throwable t) {
                            Log.w(TAG, "getTagIDs failed: " + t.getMessage(), t);
                            tryReinitUhfIfNeeded();
                        }
                        if (list != null && !list.isEmpty()) {
                            for (EPC tag : list) {
                                final String epcId = tag.getId();
                                if (epcId == null || epcId.isEmpty()) continue;
                                boolean isNew = false;
                                synchronized (scannedSet) {
                                    if (!scannedSet.contains(epcId)) {
                                        scannedSet.add(epcId);
                                        isNew = true;
                                    }
                                }
                                if (isNew) newTagsQueue.add(epcId);
                            }
                        }
                        // small sleep to prevent 100% CPU (tweak as needed)
                        try { Thread.sleep(30); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }

                    } else {
                        // SINGLE-SCAN MODE: inventoryOnce
                        boolean found = mDevice.inventoryOnce(reusable, INVENTORY_TIMEOUT_MS);
                        if (found) {
                            final String epcId = reusable.getId();
                            if (epcId != null && !epcId.isEmpty()) {
                                boolean isNew = false;
                                synchronized (scannedSet) {
                                    if (!scannedSet.contains(epcId)) {
                                        scannedSet.add(epcId);
                                        isNew = true;
                                    }
                                }
                                if (isNew) {
                                    newTagsQueue.add(epcId); // enqueue only
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "scan error: " + t.getMessage(), t);
                    tryReinitUhfIfNeeded();
                    // small backoff on errors
                    try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }

                // tiny sleep makes loop friendly while keeping throughput high (for single-scan; continuous already sleeps)
                if (!continuousMode) {
                    try { Thread.sleep(SCAN_LOOP_SLEEP_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        } finally {
            // make sure hardware scan is stopped when thread exits
            if (continuousMode) {
                try { mDevice.inventoryStop(); } catch (Exception ignored) {}
            }

            runOnUiThreadSafe(() -> {
                binding.scanProgress.setVisibility(View.GONE);
                binding.scanStatusText.setVisibility(View.GONE);
                binding.startScanBtn.setText("Start Scan");
                updateUiState();
            });
        }
    }

    /**
     * Consumer / UI flusher:
     * drains newTagsQueue periodically, adds items to scannedList once,
     * then notifies the adapter with a single notifyItemRangeInserted call.
     */
    private final Runnable uiFlushRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                // drain queue into a small batch
                if (!newTagsQueue.isEmpty()) {
                    List<String> batch = new ArrayList<>();
                    long now = System.currentTimeMillis();
                    while (!newTagsQueue.isEmpty()) {
                        String epc = newTagsQueue.poll();
                        if (epc == null) break;
                        batch.add(epc);
                        // safety cap per flush to avoid huge UI bursts (tweakable)
                        if (batch.size() >= 200) break;
                    }

                    if (!batch.isEmpty()) {
                        // insert at top in reverse order so oldest of batch becomes later index
                        synchronized (scannedList) {
                            Collections.reverse(batch); // because we want newest-first; scan detected newest first so we reverse to preserve order at top
                            scannedList.addAll(0, batch);
                        }

                        // play one beep for the batch (throttled)
                        if (soundPool != null && soundScanId != 0 && now - lastSoundTime > SOUND_THROTTLE_MS) {
                            try { soundPool.play(soundScanId, 1f, 1f, 1, 0, 1f); } catch (Exception ignore) {}
                            lastSoundTime = now;
                        }

                        final int inserted = batch.size();
                        runOnUiThreadSafe(() -> {
                            try {
                                // adapter uses scannedList reference; notify range inserted at top
                                tagListAdapter.notifyItemRangeInserted(0, inserted);
                                binding.scannedTagsRecycler.scrollToPosition(0);
                            } catch (Exception e) {
                                tagListAdapter.notifyDataSetChanged();
                            }
                            updateUiState();
                        });
                    }
                } else {
                    // occasional UI refresh of counts while scanning
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
                // reschedule
                mainHandler.postDelayed(this, UI_UPDATE_INTERVAL_MS);
            }
        }
    };

    private void stopRFIDScan() {
        isScanning.set(false);

        // proactively stop hardware continuous scan if running
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "Hardware key pressed: " + keyCode);

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

    // confirmation dialog
    private void confirmAndPerformInventory(String direction) {
        if (scannedList.isEmpty()) {
            Snackbar.make(binding.scannedTagsRecycler, "No tags to process", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String title = direction.equalsIgnoreCase("inward") ? "Confirm INWARD" : "Confirm OUTWARD";
        String message = "Are you sure you want to perform " + direction.toUpperCase() + " for " + scannedList.size() + " tags?";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Proceed", (dialog, which) -> {
                    dialog.dismiss();
                    performInventoryOperation(direction);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // performInventoryOperation (unchanged behavior; included for completeness)
    private void performInventoryOperation(String direction) {

        // Build reads list (each item: epc, qty, read_time)
        List<Map<String, Object>> reads = new ArrayList<>();
        synchronized (scannedList) {
            for (String epc : scannedList) {
                if (epc == null) continue;
                String e = epc.trim();
                if (e.isEmpty()) continue;
                Map<String, Object> item = new HashMap<>();
                item.put("epc", e);
                item.put("qty", 1);
                item.put("read_time", isoUtcNow());
                reads.add(item);
            }
        }

        if (reads.isEmpty()) {
            Snackbar.make(binding.scannedTagsRecycler, "No valid tags to submit", Snackbar.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("android_id", getDeviceAndroidId());
        payload.put("defaultMovementAction", direction); // 'inward' or 'outward'
        payload.put("min_scan_interval", 2);
        payload.put("reads", reads);

        // Debug
        try { Log.d(TAG, "inventory payload: " + gson.toJson(payload)); } catch (Exception ignored) {}

        // Disable UI while submitting
        runOnUiThreadSafe(() -> {
            binding.scanStatusText.setText("Submitting...");
            binding.scanStatusText.setVisibility(View.VISIBLE);
            binding.inwardBtn.setEnabled(false);
            binding.outwardBtn.setEnabled(false);
            binding.startScanBtn.setEnabled(false);
            binding.clearTagsBtn.setEnabled(false);
        });

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        Call<Map<String, Object>> call = api.handReaderScan(payload);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                runOnUiThreadSafe(() -> binding.scanStatusText.setVisibility(View.GONE));

                if (!response.isSuccessful() || response.body() == null) {
                    final int code = response.code();
                    runOnUiThreadSafe(() -> {
                        Snackbar.make(binding.scannedTagsRecycler, "Operation failed: HTTP " + code, Snackbar.LENGTH_SHORT).show();
                        restoreButtonsAfterSubmit();
                    });
                    return;
                }

                Map<String, Object> body = response.body();
                boolean anySuccess = false;
                boolean anyFailure = false;
                String serverMessage = body.get("message") != null ? String.valueOf(body.get("message")) : "";

                Object resultsObj = body.get("results");
                List<String> failMessages = new ArrayList<>();

                if (resultsObj instanceof List<?>) {
                    List<?> results = (List<?>) resultsObj;
                    for (Object item : results) {
                        if (item instanceof Map<?, ?>) {
                            Map<?, ?> row = (Map<?, ?>) item;
                            boolean rowSuccess = false;
                            Object rowOk = row.get("success");
                            if (rowOk instanceof Boolean) rowSuccess = (Boolean) rowOk;
                            else rowSuccess = "1".equals(String.valueOf(rowOk));

                            if (rowSuccess) anySuccess = true;
                            else {
                                anyFailure = true;
                                String epc = row.get("epc_code") != null ? String.valueOf(row.get("epc_code"))
                                        : (row.get("epc") != null ? String.valueOf(row.get("epc")) : "(unknown)");
                                String msg = row.get("message") != null ? String.valueOf(row.get("message")) : "Failed";
                                failMessages.add(epc + ": " + msg);
                            }
                        }
                    }

                    // Show top 3 failure messages
                    if (!failMessages.isEmpty()) {
                        final StringBuilder sb = new StringBuilder();
                        int limit = Math.min(3, failMessages.size());
                        for (int i = 0; i < limit; i++) {
                            sb.append(failMessages.get(i));
                            if (i < limit - 1) sb.append("\n");
                        }
                        final String preview = sb.toString();
                        runOnUiThreadSafe(() -> Snackbar.make(binding.scannedTagsRecycler, preview, Snackbar.LENGTH_LONG).show());
                    }
                }

                if (!anySuccess && anyFailure) {
                    runOnUiThreadSafe(() -> Snackbar.make(binding.scannedTagsRecycler, "All tags failed.", Snackbar.LENGTH_LONG).show());
                    restoreButtonsAfterSubmit();
                    return;
                }

                if (anySuccess && anyFailure) {
                    runOnUiThreadSafe(() -> Snackbar.make(binding.scannedTagsRecycler, "Some items processed, some failed.", Snackbar.LENGTH_LONG).show());
                } else if (anySuccess) {
                    final String msg = serverMessage.isEmpty() ? "Operation completed" : serverMessage;
                    runOnUiThreadSafe(() -> Snackbar.make(binding.scannedTagsRecycler, msg, Snackbar.LENGTH_SHORT).show());
                }

                if (!serverMessage.isEmpty()) {
                    runOnUiThreadSafe(() -> Snackbar.make(binding.scannedTagsRecycler, serverMessage, Snackbar.LENGTH_LONG).show());
                }

                if (anySuccess) {
                    synchronized (scannedSet) {
                        scannedSet.clear();
                    }
                    synchronized (scannedList) {
                        scannedList.clear();
                    }
                    newTagsQueue.clear();

                    runOnUiThreadSafe(() -> {
                        tagListAdapter.notifyDataSetChanged();
                        updateUiState();
                    });
                }

                restoreButtonsAfterSubmit();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                runOnUiThreadSafe(() -> {
                    binding.scanStatusText.setVisibility(View.GONE);
                    Snackbar.make(binding.scannedTagsRecycler, "Network error: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
                    restoreButtonsAfterSubmit();
                });
            }
        });
    }

    private void restoreButtonsAfterSubmit() {
        runOnUiThreadSafe(() -> {
            binding.inwardBtn.setEnabled(!scannedList.isEmpty());
            binding.outwardBtn.setEnabled(!scannedList.isEmpty());
            binding.startScanBtn.setEnabled(true);
            binding.clearTagsBtn.setEnabled(!scannedList.isEmpty());
        });
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
        if (isScanning.get() && (now - lastUiUpdateTime) < UI_UPDATE_INTERVAL_MS) {
            return;
        }
        lastUiUpdateTime = now;

        runOnUiThreadSafe(() -> {
            binding.tvScannedCount.setText(String.valueOf(scannedList.size()));

            boolean hasTags = !scannedList.isEmpty();
            binding.inwardBtn.setEnabled(hasTags);
            binding.outwardBtn.setEnabled(hasTags);

            boolean clearEnabled = hasTags;
            if (binding.clearTagsBtn.isEnabled() != clearEnabled) {
                binding.clearTagsBtn.setEnabled(clearEnabled);
            }
        });
    }

    // TagListAdapter uses the same list reference (scannedList)
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

        @NonNull
        @Override
        public TagViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scanned_tag, parent, false);
            return new TagViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
            String epc = tags.get(position);
            holder.bind(epc, () -> clickListener.onTagClicked(epc), () -> removeListener.onRemoveClicked(epc));
        }

        @Override
        public int getItemCount() { return tags.size(); }
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
}
