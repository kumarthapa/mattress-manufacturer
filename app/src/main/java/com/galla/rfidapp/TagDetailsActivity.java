package com.galla.rfidapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.galla.rfidapp.util.PrefHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;
import com.galla.rfidapp.network.ApiClient;
import com.galla.rfidapp.network.ApiService;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FINAL VERSION – Beautiful Material UI dropdowns + Tag scanning logic
 */
public class TagDetailsActivity extends BaseDrawerActivity {

    private static final String TAG = "TagDetailsActivity";

    private static final int[] HW_KEYS = {
            142,
            KeyEvent.KEYCODE_F1,
            KeyEvent.KEYCODE_F2,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_A
    };

    // dropdown options
    private static final String[] STAGES = {
            "Collected","Sorting","Washing","Rinsing","Hydro Extract","Drying",
            "Pressing","Folding","Packing"
    };

    private static final String[] STATUS = {
            "Pending","In-Process","Pass","Fail","Damage","Lost","Rewash Required"
    };

    private UHFService mDevice;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MaterialButton scanBtn, btnUpdateStage;
    private ProgressBar progress;
    private MaterialCardView detailsCard;
    private TextView tvProductName, tvSku, tvTagEpc;
    private ImageButton btnCloseCard;

    private AutoCompleteTextView acStage, acStatus;
    private MediaPlayer scanSound;

    private volatile boolean scanningInProgress = false;
    private static final char ZWSP = '\u200B';

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_tag_details;
    }

    @Override
    protected String getToolbarTitle() {
        return "Laundry Process";
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bindViews();
        setupDropdowns();
        initUhf();
        setupListeners();
    }

    private void bindViews() {
        scanBtn = findViewById(R.id.btnScanSingleTag);
        progress = findViewById(R.id.progressScan);
        detailsCard = findViewById(R.id.cardDetails);

        tvProductName = findViewById(R.id.tvProductName);
        tvSku = findViewById(R.id.tvSku);
        tvTagEpc = findViewById(R.id.tvTagEpc);


        btnCloseCard = findViewById(R.id.btnCloseCard);
        btnUpdateStage = findViewById(R.id.btnUpdateStage);

        acStage = findViewById(R.id.acStage);
        acStatus = findViewById(R.id.acStatus);

        detailsCard.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);

        tvTagEpc.setTextIsSelectable(true);
    }

    private void setupDropdowns() {

        ArrayAdapter<String> stageAdapter =
                new ArrayAdapter<>(this, R.layout.item_dropdown, R.id.tvItem, STAGES);

        ArrayAdapter<String> statusAdapter =
                new ArrayAdapter<>(this, R.layout.item_dropdown, R.id.tvItem, STATUS);

        acStage.setAdapter(stageAdapter);
        acStatus.setAdapter(statusAdapter);

        disableKeyboard(acStage);
        disableKeyboard(acStatus);
    }

    private void disableKeyboard(AutoCompleteTextView view) {
        view.setInputType(0);
        view.setFocusable(false);
        view.setOnClickListener(v -> view.showDropDown());
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) view.showDropDown();
        });
    }

    private void setupListeners() {

        scanBtn.setOnClickListener(v -> {
            if (!scanningInProgress) scanSingleTag();
        });

        btnCloseCard.setOnClickListener(v -> {
            detailsCard.setVisibility(View.GONE);
            tvTagEpc.setText("-");
        });

        btnUpdateStage.setOnClickListener(v -> {

            String stage = acStage.getText().toString().trim();
            String status = acStatus.getText().toString().trim();

            if (stage.isEmpty()) {
                Snackbar.make(scanBtn, "Please select a stage", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (status.isEmpty()) {
                Snackbar.make(scanBtn, "Please select a status", Snackbar.LENGTH_SHORT).show();
                return;
            }


            applyStatusColor(status);

            Snackbar.make(scanBtn, "Updated!", Snackbar.LENGTH_SHORT).show();
        });

        try { scanSound = MediaPlayer.create(this, R.raw.scan); } catch (Exception ignored) {}
    }

    private void applyStatusColor(String status) {

        int color;

        switch (status.toLowerCase()) {
            case "pass": color = Color.parseColor("#4CAF50"); break;
            case "fail": color = Color.parseColor("#F44336"); break;
            case "pending": color = Color.parseColor("#9E9E9E"); break;
            case "in-process": color = Color.parseColor("#FFC107"); break;
            case "damage": color = Color.parseColor("#E65100"); break;
            case "lost": color = Color.parseColor("#795548"); break;
            case "rewash required": color = Color.parseColor("#0097A7"); break;
            default: color = Color.parseColor("#607D8B");
        }

    }

    private void initUhf() {
        try {
            mDevice = UHFService.getInstance();
        } catch (Throwable t) {
            Log.e(TAG, "UHF error: " + t.getMessage());
        }
    }

    private void scanSingleTag() {
        if (mDevice == null) {
            Snackbar.make(scanBtn, "UHF not available!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        scanningInProgress = true;
        progress.setVisibility(View.VISIBLE);
        scanBtn.setEnabled(false);

        new Thread(() -> {
            try { mDevice.open(); } catch (Exception ignored) {}

            EPC epc = new EPC();
            boolean ok = mDevice.inventoryOnce(epc, 300);

            if (!ok) {
                mainHandler.post(() -> {
                    Snackbar.make(scanBtn, "No tag detected", Snackbar.LENGTH_SHORT).show();
                    done();
                });
                return;
            }

            String epcId = epc.getId();

            mainHandler.post(() -> fetchTagDetailsFromServer(epcId));

        }).start();
    }

    private void done() {
        scanningInProgress = false;
        progress.setVisibility(View.GONE);
        scanBtn.setEnabled(true);
    }

    private void fetchTagDetailsFromServer(String epcId) {

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        Call<Map<String, Object>> call = api.getTagDetails(epcId);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {

                done();

                if (!response.isSuccessful() || response.body() == null) {
                    Snackbar.make(scanBtn, "Server error: " + response.code(), Snackbar.LENGTH_LONG).show();
                    return;
                }

                Map<String, Object> body = response.body();

                // API RETURNS ONLY "inventory", NOT "product"
                Object invObj = body.get("inventory");

                String productName = "Tag Not Mapped";
                String sku = "NIL";
                String productCode = "-";
                String tagStatus = "-";

                // -------------------------------
                // ✅ INVENTORY PARSING (CORRECT)
                // -------------------------------
                if (invObj instanceof Map) {
                    Map<?, ?> inv = (Map<?, ?>) invObj;

                    if (inv.get("product_name") != null)
                        productName = String.valueOf(inv.get("product_name"));

                    if (inv.get("sku") != null)
                        sku = String.valueOf(inv.get("sku"));

                    if (inv.get("product_code") != null)
                        productCode = String.valueOf(inv.get("product_code"));

                    if (inv.get("status_text") != null)
                        tagStatus = String.valueOf(inv.get("status_text"));
                    else if (inv.get("status") != null)
                        tagStatus = String.valueOf(inv.get("status"));
                }

                // -------------------------------
                // ✅ UPDATE UI
                // -------------------------------
                tvProductName.setText(productName);
                tvSku.setText(!sku.isEmpty() ? sku : productCode); // if sku missing, show product_code
                tvTagEpc.setText(wrapEpc(epcId));

                // Set dropdown selection
                acStatus.setText(tagStatus, false);

                // Default stage selection
                acStage.setText(STAGES[0], false); // "Collected"

                detailsCard.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                done();
                Snackbar.make(scanBtn, "Network error: " + t.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }


    private String wrapEpc(String epc) {
        if (epc == null || epc.length() <= 8) return epc;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < epc.length(); i++) {
            sb.append(epc.charAt(i));
            if ((i + 1) % 4 == 0) sb.append(ZWSP);
        }
        return sb.toString();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        for (int k : HW_KEYS) {
            if (keyCode == k) {
                if (!scanningInProgress) scanSingleTag();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
