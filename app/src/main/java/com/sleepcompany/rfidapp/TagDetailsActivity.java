package com.sleepcompany.rfidapp;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;
import com.sleepcompany.rfidapp.network.ApiClient;
import com.sleepcompany.rfidapp.network.ApiService;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * TagDetailsActivity
 * - scans a single tag (one-shot)
 * - fetches tag details from server
 * - shows card with Product name, SKU, EPC, Status
 */
public class TagDetailsActivity extends BaseDrawerActivity {
    private static final String TAG = "TagDetailsActivity";

    // Hardware trigger codes (extend if needed)
    private static final int[] HW_KEYS = {
            142,
            KeyEvent.KEYCODE_F1,
            KeyEvent.KEYCODE_F2,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_A
    };

    private UHFService mDevice;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MaterialButton scanBtn;
    private ProgressBar progress;
    private MaterialCardView detailsCard;
    private TextView tvProductName, tvSku, tvTagEpc, tvStatus;
    private ImageButton btnCloseCard;
    private MediaPlayer scanSound;

    private volatile boolean scanningInProgress = false;

    // zero-width space to allow wrapping without changing logical value
    private static final char ZWSP = '\u200B';

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_tag_details;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Let BaseDrawerActivity inflate layout (do NOT call setContentView here)
        setupToolbar();      // show hamburger/back icon + title
        bindViews();
        initUhf();
        setupListeners();
    }

    private void setupToolbar() {
        // Matches ProductsActivity behaviour so drawer icon appears
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Scan Tag");
        }
    }

    private void bindViews() {
        scanBtn = findViewById(R.id.btnScanSingleTag);
        progress = findViewById(R.id.progressScan);
        detailsCard = findViewById(R.id.cardDetails);
        tvProductName = findViewById(R.id.tvProductName);
        tvSku = findViewById(R.id.tvSku);
        tvTagEpc = findViewById(R.id.tvTagEpc);
        tvStatus = findViewById(R.id.tvStatus);
        btnCloseCard = findViewById(R.id.btnCloseCard);

        detailsCard.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);

        // Make EPC selectable and easier to copy
        tvTagEpc.setTextIsSelectable(true);
        // Prefer monospace for EPC readability (optional)
        tvTagEpc.setTypeface(android.graphics.Typeface.MONOSPACE);

        try { scanSound = MediaPlayer.create(this, R.raw.scan); } catch (Exception ignored) {}
    }

    private void initUhf() {
        try {
            mDevice = UHFService.getInstance();
            if (mDevice == null) {
                showSettingsSnackbar("UHF service not available. Open power settings?");
            }
        } catch (Throwable t) {
            Log.w(TAG, "UHFService not available: " + t.getMessage(), t);
        }
    }

    private void setupListeners() {
        scanBtn.setOnClickListener(v -> {
            if (scanningInProgress) return; // guard
            scanSingleTag();
        });

        btnCloseCard.setOnClickListener(v -> {
            detailsCard.setVisibility(View.GONE);
            tvTagEpc.setText("-");
        });
    }

    // ------------------ Scan flow ------------------
    private void scanSingleTag() {
        if (mDevice == null) {
            showSettingsSnackbar("UHF not available. Open power settings?");
            tryReinitUhfIfNeeded();
            return;
        }

        scanningInProgress = true;
        progress.setVisibility(View.VISIBLE);
        scanBtn.setEnabled(false);

        new Thread(() -> {
            try {
                // best-effort open device
                try { mDevice.open(); } catch (Exception ignored) {}

                EPC epc = new EPC();
                boolean ok = mDevice.inventoryOnce(epc, 300); // 300ms timeout

                if (!ok) {
                    mainHandler.post(() -> {
                        Snackbar.make(scanBtn, "No tag read. Try again.", Snackbar.LENGTH_SHORT).show();
                        doneScanning();
                    });
                    return;
                }

                final String epcId = epc.getId();
                if (epcId == null || epcId.trim().isEmpty()) {
                    mainHandler.post(() -> {
                        Snackbar.make(scanBtn, "Invalid tag read. Try again.", Snackbar.LENGTH_SHORT).show();
                        doneScanning();
                    });
                    return;
                }

                // If same tag is already visible -> treat as "remove" (hide card)
                mainHandler.post(() -> {
                    String shownEpc = String.valueOf(tvTagEpc.getText());
                    shownEpc = removeWrap(shownEpc); // strip ZWSPs before comparing
                    if (detailsCard.getVisibility() == View.VISIBLE && epcId.equals(shownEpc)) {
                        detailsCard.setVisibility(View.GONE);
                        tvTagEpc.setText("-");
                        Snackbar.make(scanBtn, "Tag card removed", Snackbar.LENGTH_SHORT).show();
                        doneScanning();
                    } else {
                        // play beep and fetch details
                        if (scanSound != null) {
                            try { scanSound.start(); } catch (Exception ignore) {}
                        }
                        fetchTagDetailsFromServer(epcId);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "scanSingleTag error: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    Snackbar.make(scanBtn, "Scan failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    doneScanning();
                });
            }
        }).start();
    }

    private void doneScanning() {
        scanningInProgress = false;
        progress.setVisibility(View.GONE);
        scanBtn.setEnabled(true);
    }

    // ------------------ Network ------------------
    private void fetchTagDetailsFromServer(String epcId) {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        Call<Map<String, Object>> call = api.getTagDetails(epcId);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                mainHandler.post(() -> {
                    doneScanning();
                    if (!response.isSuccessful() || response.body() == null) {
                        Snackbar.make(scanBtn, "Server error: HTTP " + response.code(), Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    Map<String, Object> body = response.body();
                    Object successObj = body.get("success");
                    boolean success = (successObj instanceof Boolean) ? (Boolean) successObj : "1".equals(String.valueOf(successObj));

                    if (!success) {
                        Object msg = body.get("message");
                        Snackbar.make(scanBtn, (msg != null ? String.valueOf(msg) : "Failed to load tag details"), Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    Object invObj = body.get("inventory");
                    Object prodObj = body.get("product"); // <-- restore product parsing

                    String productName = "-";
                    String productSku = "-";
                    String tagStatus = "-";

                    // Prefer reading product info from product object (if present)
                    if (prodObj instanceof Map) {
                        Map<?, ?> p = (Map<?, ?>) prodObj;
                        productName = p.get("product_name") != null ? String.valueOf(p.get("product_name")) :
                                p.get("name") != null ? String.valueOf(p.get("name")) : "-";
                        productSku = p.get("product_code") != null ? String.valueOf(p.get("product_code")) :
                                p.get("sku") != null ? String.valueOf(p.get("sku")) : "-";
                    } else if (invObj instanceof Map) {
                        // fallback: some APIs include product data inside inventory
                        Map<?, ?> p = (Map<?, ?>) invObj;
                        productName = p.get("product_name") != null ? String.valueOf(p.get("product_name")) :
                                p.get("name") != null ? String.valueOf(p.get("name")) : productName;
                        productSku = p.get("sku") != null ? String.valueOf(p.get("sku")) : productSku;
                    }

                    if (invObj instanceof Map) {
                        Map<?, ?> inv = (Map<?, ?>) invObj;
                        if (inv.get("status_text") != null) tagStatus = String.valueOf(inv.get("status_text"));
                        else if (inv.get("status") != null) tagStatus = String.valueOf(inv.get("status"));
                    }

                    // Populate UI and show card (use wrapped EPC for display)
                    tvProductName.setText(productName);
                    tvSku.setText(productSku);
                    tvTagEpc.setText(wrapEpc(epcId));   // show wrapped (display-only)
                    tvStatus.setText(tagStatus);

                    detailsCard.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                mainHandler.post(() -> {
                    doneScanning();
                    Snackbar.make(scanBtn, "Network error: " + t.getMessage(), Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    // ------------------ helpers: wrapping for UI but not for comparison ------------------
    private String wrapEpc(String epc) {
        if (epc == null) return "";
        if (epc.length() <= 8) return epc; // no need to wrap very short strings
        StringBuilder out = new StringBuilder(epc.length() + epc.length() / 4);
        for (int i = 0; i < epc.length(); i++) {
            out.append(epc.charAt(i));
            // insert ZWSP every 4 characters to allow wrap points
            if ((i + 1) % 4 == 0 && i + 1 < epc.length()) out.append(ZWSP);
        }
        return out.toString();
    }

    private String removeWrap(String s) {
        if (s == null) return null;
        return s.replace(String.valueOf(ZWSP), "");
    }

    // ------------------ UHF helpers ------------------
    private void tryReinitUhfIfNeeded() {
        try {
            if (mDevice == null) {
                mDevice = UHFService.getInstance();
                if (mDevice != null) {
                    Snackbar.make(scanBtn, "UHF re-initialized", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                Snackbar.make(scanBtn, "UHF service not running. Open power settings?", Snackbar.LENGTH_LONG)
                        .setAction("Open", v -> openPowerSettings()).show();
            } else {
                // optional quick test read (best-effort)
                try {
                    EPC e = new EPC();
                    mDevice.inventoryOnce(e, 50);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            Log.w(TAG, "tryReinitUhfIfNeeded: " + t.getMessage(), t);
            mDevice = null;
        }
    }

    private void showSettingsSnackbar(String msg) {
        Snackbar.make(scanBtn, msg, Snackbar.LENGTH_LONG).setAction("Open", v -> openPowerSettings()).show();
    }

    private void openPowerSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } else {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "openPowerSettings: " + e.getMessage(), e);
            Snackbar.make(scanBtn, "Unable to open settings", Snackbar.LENGTH_SHORT).show();
        }
    }

    // ------------------ Hardware trigger ------------------
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        for (int k : HW_KEYS) {
            if (keyCode == k) {
                if (!scanningInProgress) scanSingleTag();
                return true; // consume
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    // ------------------ Lifecycle ------------------
    @Override
    protected void onResume() {
        super.onResume();
        if (mDevice != null) {
            try { mDevice.open(); } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanningInProgress = false;
        if (mDevice != null) {
            try { mDevice.close(); } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanSound != null) {
            try { scanSound.release(); } catch (Exception ignore) {}
        }
    }

    // toolbar back/hamburger behavior
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
