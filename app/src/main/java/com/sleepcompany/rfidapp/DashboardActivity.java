package com.sleepcompany.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class DashboardActivity extends AppCompatActivity {

    private MaterialCardView totalProductsCard, inProgressCard, completedCard;
    private MaterialButton btnScanRFID, btnViewProducts, btnQCCheck;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Enable back button (optional - remove if this is your main dashboard)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Dashboard");
        }

        // Initialize Material components
        totalProductsCard = findViewById(R.id.totalProductsCard);
        inProgressCard = findViewById(R.id.inProgressCard);
        completedCard = findViewById(R.id.completedCard);

        btnScanRFID = findViewById(R.id.btnScanRFID);
        btnViewProducts = findViewById(R.id.btnViewProducts);
        btnQCCheck = findViewById(R.id.btnQCCheck);

        // Set click listeners for navigation
        btnViewProducts.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, ProductsActivity.class)));

        btnScanRFID.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, ScannerActivity.class)));

        btnQCCheck.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, QcActivity.class)));

        // Optional: Add click listeners to stat cards for more detailed views
        totalProductsCard.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, ProductsActivity.class)));
    }

    // Handle back button click (optional)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
