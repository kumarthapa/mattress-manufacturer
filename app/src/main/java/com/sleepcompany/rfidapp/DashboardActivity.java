package com.sleepcompany.rfidapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

public class DashboardActivity extends BaseDrawerActivity {

    // Dashboard cards and buttons
    private MaterialCardView totalProductionCard, efficiencyCard, defectCard, satisfactionCard;
    private MaterialButton btnScanRFID, btnViewProducts, btnQCCheck;

    // KPI TextViews
    private MaterialTextView tvTotalProduction, tvEfficiency, tvDefects, tvSatisfaction;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_dashboard; // Your actual dashboard layout resource
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize dashboard UI components only, no drawer setup needed here
        initializeViews();
        setupDashboardCards();
        setupQuickActions();
        loadDashboardData();
    }

    @SuppressLint("WrongViewCast")
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
        btnQCCheck = findViewById(R.id.btnQCCheck);
    }

    private void setupDashboardCards() {
        totalProductionCard.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, ProductsActivity.class)));

        efficiencyCard.setOnClickListener(v ->
                Snackbar.make(v, "Production efficiency details", Snackbar.LENGTH_SHORT).show());

        defectCard.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, QcActivity.class)));

        satisfactionCard.setOnClickListener(v ->
                Snackbar.make(v, "Customer satisfaction metrics", Snackbar.LENGTH_SHORT).show());
    }

    private void setupQuickActions() {
        btnScanRFID.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, ScannerActivity.class)));

        btnViewProducts.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, ProductsActivity.class)));

        btnQCCheck.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, QcActivity.class)));
    }

    private void loadDashboardData() {
        tvTotalProduction.setText("1,245");
        tvEfficiency.setText("87%");
        tvDefects.setText("3.2%");
        tvSatisfaction.setText("48%");
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // On dashboard, just close drawer
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        // Delegate other navigation options to base class logic
        return super.onNavigationItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
