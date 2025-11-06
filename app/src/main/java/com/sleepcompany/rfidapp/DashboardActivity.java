package com.sleepcompany.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;

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
import com.sleepcompany.rfidapp.util.PrefHelper;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DashboardActivity - pulls dashboard summary from API and updates UI.
 * Uses ApiClient (with token management) to create ApiService.
 */
public class DashboardActivity extends BaseDrawerActivity {

    private MaterialCardView totalProductionCard, efficiencyCard, defectCard, satisfactionCard;
    private MaterialButton btnScanRFID, btnViewProducts, btnWriteTag;
    private MaterialTextView tvTotalProduction, tvEfficiency, tvDefects, tvSatisfaction;

    private ChipGroup chipGroupStages;
    private RecyclerView rvRecentActivities;
    private RecentActivityAdapter recentActivityAdapter;

    private final Handler handler = new Handler();
    private final int POLL_INTERVAL_MS = 8000;

    // Keep reference to current call so we can cancel when activity pauses
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

        // initial load
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
    }

    private void setupRecycler() {
        recentActivityAdapter = new RecentActivityAdapter();
        rvRecentActivities.setLayoutManager(new LinearLayoutManager(this));
        rvRecentActivities.setAdapter(recentActivityAdapter);
    }

    private void setupDashboardCards() {
        totalProductionCard.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, ProductsActivity.class)));

        efficiencyCard.setOnClickListener(v ->
                Snackbar.make(findViewById(R.id.drawer_layout),
                        "Production efficiency details", Snackbar.LENGTH_SHORT).show());

        defectCard.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, QcActivity.class)));

        satisfactionCard.setOnClickListener(v ->
                Snackbar.make(findViewById(R.id.drawer_layout),
                        "Quality control metrics", Snackbar.LENGTH_SHORT).show());
    }

    private void setupQuickActions() {
        // Scan and view always available (assuming)
        btnScanRFID.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, ScannerActivity.class)));

        btnViewProducts.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, ProductsActivity.class)));

        // Configure write button with permission check
        // Set click listener regardless — the listener will guard on permission and show a message.
        btnWriteTag.setOnClickListener(v -> {
            boolean canWrite = PrefHelper.hasPermission(DashboardActivity.this, "write.bonding");
            if (!canWrite) {
                Snackbar.make(findViewById(R.id.drawer_layout),
                        "You don't have permission to write tags", Snackbar.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(DashboardActivity.this, WriteTagsActivity.class));
        });

        // Apply initial enabled state (in case called during onCreate)
        boolean canWriteInitially = PrefHelper.hasPermission(this, "write.bonding");
        btnWriteTag.setEnabled(canWriteInitially);
        btnWriteTag.setAlpha(canWriteInitially ? 1f : 0.56f); // visually indicate disabled
    }


    private void updateKpis(Kpis kpis) {
        if (kpis == null) return;

        // Defensive null-safe rendering
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

    /**
     * Load dashboard data via ApiClient -> ApiService
     */
    private void loadDashboardData() {
        // Cancel previous call if still running
        if (currentSummaryCall != null && !currentSummaryCall.isCanceled()) {
            currentSummaryCall.cancel();
            currentSummaryCall = null;
        }

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        currentSummaryCall = api.getDashboardSummary(5); // ask backend to cache for 5s

        currentSummaryCall.enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(Call<DashboardResponse> call, Response<DashboardResponse> response) {
                // clear reference (finished)
                currentSummaryCall = null;

                if (!response.isSuccessful() || response.body() == null) {
                    Snackbar.make(findViewById(R.id.drawer_layout),
                            "Dashboard load failed: " + response.code(), Snackbar.LENGTH_SHORT).show();
                    return;
                }

                DashboardResponse body = response.body(); // top-level DashboardResponse
                if (Boolean.TRUE.equals(body.success) && body.data != null) {
                    updateKpis(body.data.kpis);
                    updateStages(body.data.stages);
//                    updateRecent(body.data.recent_activities);
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

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);

        // Re-evaluate write permission each time activity resumes
        boolean canWrite = PrefHelper.hasPermission(this, "write.bonding");
        btnWriteTag.setEnabled(canWrite);
        btnWriteTag.setAlpha(canWrite ? 1f : 0.56f);
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
