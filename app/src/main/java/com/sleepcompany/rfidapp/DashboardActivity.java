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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;
import com.sleepcompany.rfidapp.adapter.RecentActivityAdapter;
import com.sleepcompany.rfidapp.model.DashboardResponse;
import com.sleepcompany.rfidapp.model.Kpis;
import com.sleepcompany.rfidapp.model.RecentActivity;
import com.sleepcompany.rfidapp.network.ApiClient;
import com.sleepcompany.rfidapp.network.ApiService;
import com.sleepcompany.rfidapp.network.UpdateCheckRequest;
import com.sleepcompany.rfidapp.network.UpdateCheckResponse;
import com.sleepcompany.rfidapp.util.PrefHelper;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends BaseDrawerActivity {

    private static final String LOGTAG = "DashboardActivity";

    private MaterialCardView totalProductionCard, efficiencyCard, defectCard, satisfactionCard;
    private MaterialButton btnScanRFID, btnViewProducts, btnWriteTag;
    private MaterialTextView tvTotalProduction, tvEfficiency, tvDefects, tvSatisfaction;

    private ChipGroup chipGroupStages;
    private RecyclerView rvRecentActivities;
    private RecentActivityAdapter recentActivityAdapter;

    private TextView tvUpdateBanner; // <-- update banner

    private final Handler handler = new Handler();
    private final int POLL_INTERVAL_MS = 8000;

    private Call<DashboardResponse> currentSummaryCall = null;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadDashboardData();
            handler.postDelayed(this, POLL_INTERVAL_MS);
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
        setupQuickActions();
        setupRecycler();

        loadDashboardData();
    }

    private void initializeViews() {
        totalProductionCard = findViewById(R.id.totalProductionCard);
        efficiencyCard = findViewById(R.id.efficiencyCard);
        defectCard = findViewById(R.id.defectCard);
        satisfactionCard = findViewById(R.id.satisfactionCard);

        tvTotalProduction = findViewById(R.id.tvTotalProduction);
        tvEfficiency = findViewById(R.id.tvEfficiency);
        tvDefects = findViewById(R.id.tvDefects);
        tvSatisfaction = findViewById(R.id.tvSatisfaction);

        btnScanRFID = findViewById(R.id.btnScanRFID);
        btnViewProducts = findViewById(R.id.btnViewProducts);
        btnWriteTag = findViewById(R.id.btnWriteTag);

        chipGroupStages = findViewById(R.id.chipGroupStages);
        rvRecentActivities = findViewById(R.id.rvRecentActivities);

        // Update banner
        tvUpdateBanner = findViewById(R.id.tvUpdateBanner);

        if (tvUpdateBanner == null) {
            Log.w(LOGTAG, "tvUpdateBanner is null — check your activity_dashboard layout (id: tvUpdateBanner).");
        } else {
            // ensure hidden by default
            tvUpdateBanner.setVisibility(View.GONE);

            tvUpdateBanner.setOnClickListener(v -> {
                // Launch LauncherActivity to handle update. Pass current screen so Launcher can return here.
                Intent i = new Intent(DashboardActivity.this, LauncherActivity.class);
                i.putExtra(LauncherActivity.EXTRA_NEXT_SCREEN, DashboardActivity.class.getName());
                startActivity(i);
            });
        }
    }

    private void setupRecycler() {
        recentActivityAdapter = new RecentActivityAdapter();
        rvRecentActivities.setLayoutManager(new LinearLayoutManager(this));
        rvRecentActivities.setAdapter(recentActivityAdapter);
    }

    /**
     * ---------- DASHBOARD CARDS ----------
     */
    private void setupDashboardCards() {

        // direct navigation (fast)
        totalProductionCard.setOnClickListener(v ->
                navigateTo(ProductsActivity.class));

        efficiencyCard.setOnClickListener(v ->
                Snackbar.make(findViewById(R.id.drawer_layout),
                        "Production efficiency details", Snackbar.LENGTH_SHORT).show());

        defectCard.setOnClickListener(v ->
                navigateTo(QcActivity.class));

        satisfactionCard.setOnClickListener(v ->
                Snackbar.make(findViewById(R.id.drawer_layout),
                        "Quality control metrics", Snackbar.LENGTH_SHORT).show());
    }

    /**
     * ---------- QUICK ACTION BUTTONS ----------
     */
    private void setupQuickActions() {

        // Direct navigation (fast)
        btnScanRFID.setOnClickListener(v ->
                navigateTo(ScannerActivity.class));

        btnViewProducts.setOnClickListener(v ->
                navigateTo(ProductsActivity.class));

        btnWriteTag.setOnClickListener(v -> {
            boolean canWrite = PrefHelper.hasPermission(DashboardActivity.this, "write.bonding");
            if (!canWrite) {
                Snackbar.make(findViewById(R.id.drawer_layout),
                        "You don't have permission to write tags", Snackbar.LENGTH_SHORT).show();
                return;
            }
            navigateTo(WriteTagsActivity.class);
        });

        boolean canWriteInitially = PrefHelper.hasPermission(this, "write.bonding");
        btnWriteTag.setEnabled(canWriteInitially);
        btnWriteTag.setAlpha(canWriteInitially ? 1f : 0.56f);
    }

    /**
     * ---------- DASHBOARD DATA UPDATE ----------
     */
    private void updateKpis(Kpis kpis) {
        if (kpis == null) return;

        tvTotalProduction.setText(String.valueOf(kpis.total_today != null ? kpis.total_today : 0));
        tvEfficiency.setText(kpis.efficiency_percent != null
                ? String.format("%.2f%%", kpis.efficiency_percent)
                : "—");
        tvDefects.setText(kpis.defect_rate_percent != null
                ? String.format("%.2f%%", kpis.defect_rate_percent)
                : "—");
        tvSatisfaction.setText(kpis.pass_today != null
                ? kpis.pass_today + " pass"
                : "—");
    }

    private void updateStages(Map<String, Integer> stages) {
        chipGroupStages.removeAllViews();
        if (stages == null || stages.isEmpty()) return;

        for (Map.Entry<String, Integer> e : stages.entrySet()) {
            final String stage = e.getKey();
            Integer count = e.getValue();

            Chip chip = new Chip(this);
            chip.setText(stage + " (" + (count != null ? count : 0) + ")");
            chip.setCheckable(false);
            chip.setOnClickListener(v ->
                    Snackbar.make(findViewById(R.id.drawer_layout),
                            "Stage: " + stage, Snackbar.LENGTH_SHORT).show());
            chipGroupStages.addView(chip);
        }
    }

    private void updateRecent(List<RecentActivity> list) {
        recentActivityAdapter.setItems(list);
    }

    private void loadDashboardData() {

        if (currentSummaryCall != null && !currentSummaryCall.isCanceled()) {
            currentSummaryCall.cancel();
            currentSummaryCall = null;
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
                if (Boolean.TRUE.equals(body.success) && body.data != null) {
                    updateKpis(body.data.kpis);
                    updateStages(body.data.stages);
                } else {
                    Snackbar.make(findViewById(R.id.drawer_layout),
                            "Dashboard error: " + body.message, Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DashboardResponse> call, Throwable t) {
                currentSummaryCall = null;
                if (call.isCanceled()) return;

                Snackbar.make(findViewById(R.id.drawer_layout),
                        "Network error: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Lightweight update check that only shows the banner (non-blocking).
     */
    private void checkUpdateForDashboard() {
        String androidId = com.sleepcompany.rfidapp.LauncherActivity.getAndroidId(this);
        int currentVersion = BuildConfig.VERSION_CODE;

        UpdateCheckRequest req = new UpdateCheckRequest(androidId, currentVersion);
        ApiService api = ApiClient.getPublicClient().create(ApiService.class);

        Log.d(LOGTAG, "checkUpdateForDashboard: device=" + androidId + " currentVersion=" + currentVersion);

        api.checkUpdate(req).enqueue(new Callback<UpdateCheckResponse>() {
            @Override
            public void onResponse(Call<UpdateCheckResponse> call, Response<UpdateCheckResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    UpdateCheckResponse body = response.body();

                    // SHOW if the server requests update (no version compare)
                    final boolean show = body.isUpdate_required();
                    Log.d(LOGTAG, "update check result: update_required=" + body.isUpdate_required()
                            + " latest_version=" + body.getLatest_version_code() + " -> showBanner=" + show);

                    runOnUiThread(() -> {
                        if (tvUpdateBanner == null) {
                            Log.w(LOGTAG, "tvUpdateBanner is null when trying to set visibility");
                            return;
                        }
                        tvUpdateBanner.setVisibility(show ? View.VISIBLE : View.GONE);
                    });
                } else {
                    Log.w(LOGTAG, "checkUpdateForDashboard response not successful or empty");
                    // keep banner hidden on error
                    runOnUiThread(() -> {
                        if (tvUpdateBanner != null) tvUpdateBanner.setVisibility(View.GONE);
                    });
                }
            }

            @Override
            public void onFailure(Call<UpdateCheckResponse> call, Throwable t) {
                Log.w(LOGTAG, "checkUpdateForDashboard failed: " + (t != null ? t.getMessage() : "unknown"));
                // ignore — do not block user; hide banner
                runOnUiThread(() -> {
                    if (tvUpdateBanner != null) tvUpdateBanner.setVisibility(View.GONE);
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);

        boolean canWrite = PrefHelper.hasPermission(this, "write.bonding");
        btnWriteTag.setEnabled(canWrite);
        btnWriteTag.setAlpha(canWrite ? 1f : 0.56f);

        // Run lightweight update check (shows banner if available)
        checkUpdateForDashboard();
    }

    @Override
    protected void onPause() {
        super.onPause();

        handler.removeCallbacks(pollRunnable);

        if (currentSummaryCall != null && !currentSummaryCall.isCanceled()) {
            currentSummaryCall.cancel();
            currentSummaryCall = null;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        return super.onNavigationItemSelected(item);
    }
}
