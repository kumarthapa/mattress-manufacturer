package com.sleepcompany.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;
import com.sleepcompany.rfidapp.model.DashboardResponse;
import com.sleepcompany.rfidapp.model.Kpis;
import com.sleepcompany.rfidapp.network.ApiClient;
import com.sleepcompany.rfidapp.network.ApiService;
import com.sleepcompany.rfidapp.network.UpdateCheckRequest;
import com.sleepcompany.rfidapp.network.UpdateCheckResponse;
import com.sleepcompany.rfidapp.util.PrefHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends BaseDrawerActivity {

    private static final String LOGTAG = "DashboardActivity";

    private MaterialCardView totalProductionCard, efficiencyCard, monthTotalCard, inventoryCard;
    private MaterialButton btnScanRFID, btnViewProducts, tagMapping;
    private MaterialTextView tvTotalProduction, tvEfficiency;
    private TextView tvTotalMonth, tvInventorySummary;

    private TextView tvUpdateBanner;

    private final Handler handler = new Handler();
    private final int POLL_INTERVAL_MS = 8000;

    private Call<DashboardResponse> currentSummaryCall = null;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadDashboardData();
            handler.postDelayed(pollRunnable, POLL_INTERVAL_MS); // safe, handler exists now
        }
    };


    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_dashboard;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeViews();
        setupDashboardCards();
        setupQuickNavigation();

        loadDashboardData();
    }

    private void initializeViews() {
        totalProductionCard = findViewById(R.id.totalProductionCard);
        efficiencyCard = findViewById(R.id.efficiencyCard);
        monthTotalCard = findViewById(R.id.monthTotalCard);
        inventoryCard = findViewById(R.id.inventoryCard);

        tvTotalProduction = findViewById(R.id.tvTotalProduction);   // TOTAL TAGS
        tvEfficiency = findViewById(R.id.tvEfficiency);             // INWARD / OUTWARD
        tvTotalMonth = findViewById(R.id.tvTotalMonth);             // TOTAL PRODUCTS
        tvInventorySummary = findViewById(R.id.tvInventorySummary); // MAPPED / UNMAPPED

        btnScanRFID = findViewById(R.id.btnScanRFID);
        btnViewProducts = findViewById(R.id.btnViewProducts);
        tagMapping = findViewById(R.id.tagMapping);

        tvUpdateBanner = findViewById(R.id.tvUpdateBanner);
        if (tvUpdateBanner != null) {
            tvUpdateBanner.setVisibility(View.GONE);
            tvUpdateBanner.setOnClickListener(v -> {
                Intent i = new Intent(DashboardActivity.this, LauncherActivity.class);
                i.putExtra(LauncherActivity.EXTRA_NEXT_SCREEN, DashboardActivity.class.getName());
                startActivity(i);
            });
        }
    }

    /** ---------- DASHBOARD CARD CLICKS ---------- */
    private void setupDashboardCards() {
        totalProductionCard.setOnClickListener(v ->
                Snackbar.make(findViewById(R.id.drawer_layout),
                        "Total Tags", Snackbar.LENGTH_SHORT).show());

        efficiencyCard.setOnClickListener(v ->
                Snackbar.make(findViewById(R.id.drawer_layout),
                        "Inward / Outward Summary", Snackbar.LENGTH_SHORT).show());

        monthTotalCard.setOnClickListener(v ->
                Snackbar.make(findViewById(R.id.drawer_layout),
                        "Total Products", Snackbar.LENGTH_SHORT).show());

        inventoryCard.setOnClickListener(v ->
                Snackbar.make(findViewById(R.id.drawer_layout),
                        "Mapped / Unmapped Tags", Snackbar.LENGTH_SHORT).show());
    }

    /** ---------- QUICK NAVIGATION ---------- */
    private void setupQuickNavigation() {

        btnScanRFID.setOnClickListener(v -> navigateTo(TagDetailsActivity.class));
        tagMapping.setOnClickListener(v -> navigateTo(ScannerActivity.class));
        btnViewProducts.setOnClickListener(v -> navigateTo(ProductsActivity.class));

        boolean canWrite = PrefHelper.hasPermission(this, "write.inventory");
        tagMapping.setEnabled(canWrite);
        tagMapping.setAlpha(canWrite ? 1f : 0.56f);
    }

    /** ---------- UPDATE KPI VALUES ---------- */
    private void updateKpis(Kpis kpis) {
        if (kpis == null) return;

        // TOTAL TAGS
        tvTotalProduction.setText(String.valueOf(kpis.total_tags != null ? kpis.total_tags : 0));

        // INWARD / OUTWARD
        int inward = kpis.total_inward != null ? kpis.total_inward : 0;
        int outward = kpis.total_outward != null ? kpis.total_outward : 0;
        tvEfficiency.setText(inward + " / " + outward);

        // TOTAL PRODUCTS
        tvTotalMonth.setText(String.valueOf(kpis.total_products != null ? kpis.total_products : 0));

        // MAPPED / UNMAPPED
        int mapped = kpis.total_tags_mapped != null ? kpis.total_tags_mapped : 0;
        int unmapped = kpis.total_tags_unmapped != null ? kpis.total_tags_unmapped : 0;

        tvInventorySummary.setText(mapped + " mapped / " + unmapped + " unmapped");
    }

    /** ---------- LOAD DASHBOARD FROM API ---------- */
    private void loadDashboardData() {

        if (currentSummaryCall != null && !currentSummaryCall.isCanceled()) {
            currentSummaryCall.cancel();
        }

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        currentSummaryCall = api.getDashboardSummary(5);

        currentSummaryCall.enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(Call<DashboardResponse> call, Response<DashboardResponse> response) {
                currentSummaryCall = null;

                if (!response.isSuccessful() || response.body() == null) {
                    Snackbar.make(findViewById(R.id.drawer_layout),
                            "Dashboard load failed: " + response.code(), Snackbar.LENGTH_SHORT).show();
                    return;
                }

                DashboardResponse body = response.body();
                if (Boolean.TRUE.equals(body.success) && body.data != null && body.data.kpis != null) {
                    updateKpis(body.data.kpis);
                }
            }

            @Override
            public void onFailure(Call<DashboardResponse> call, Throwable t) {
                currentSummaryCall = null;
                if (!call.isCanceled()) {
                    Snackbar.make(findViewById(R.id.drawer_layout),
                            "Network error: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    /** ---------- UPDATE CHECK ---------- */
    private void checkUpdateForDashboard() {
        String androidId = LauncherActivity.getAndroidId(this);
        int currentVersion = BuildConfig.VERSION_CODE;

        UpdateCheckRequest req = new UpdateCheckRequest(androidId, currentVersion);
        ApiService api = ApiClient.getPublicClient().create(ApiService.class);

        api.checkUpdate(req).enqueue(new Callback<UpdateCheckResponse>() {
            @Override
            public void onResponse(Call<UpdateCheckResponse> call, Response<UpdateCheckResponse> response) {
                if (!isFinishing() && response.isSuccessful() && response.body() != null) {
                    boolean show = response.body().isUpdate_required();
                    if (tvUpdateBanner != null) tvUpdateBanner.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<UpdateCheckResponse> call, Throwable t) {
                if (tvUpdateBanner != null) tvUpdateBanner.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        handler.removeCallbacks(pollRunnable);
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS); // start loop here

        boolean canWrite = PrefHelper.hasPermission(this, "write.inventory");
        tagMapping.setEnabled(canWrite);
        tagMapping.setAlpha(canWrite ? 1f : 0.56f);

        checkUpdateForDashboard();
    }


    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(pollRunnable);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_dashboard) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        return super.onNavigationItemSelected(item);
    }
}
