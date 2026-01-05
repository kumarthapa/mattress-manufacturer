package com.galla.rfidapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.galla.rfidapp.util.SyncManager;
import com.google.android.material.card.MaterialCardView;

public class DashboardActivity extends BaseDrawerActivity {

    private MaterialCardView kpiTotalStock, kpiTotalSkus,
            kpiTotalAudited, kpiVariance;

    private TextView tvTotalStock, tvTotalSkus, tvTotalAudited, tvVariance;
    private TextView tvStockTitle, tvSkusTitle, tvAuditedTitle, tvVarianceTitle;
    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_dashboard;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViews();
        loadHardcodedData();
        setupCardClicks();
    }

    private void initViews() {

        kpiTotalStock = findViewById(R.id.kpiTotalStock);
        kpiTotalSkus = findViewById(R.id.kpiTotalSkus);
        kpiTotalAudited = findViewById(R.id.kpiAudited);
        kpiVariance = findViewById(R.id.kpiVariance);


        // VALUE
        tvTotalStock = kpiTotalStock.findViewById(R.id.tvValue);
        tvTotalSkus = kpiTotalSkus.findViewById(R.id.tvValue);
        tvTotalAudited = kpiTotalAudited.findViewById(R.id.tvValue);
        tvVariance = kpiVariance.findViewById(R.id.tvValue);

        // TITLE
        tvStockTitle = kpiTotalStock.findViewById(R.id.tvTitle);
        tvSkusTitle = kpiTotalSkus.findViewById(R.id.tvTitle);
        tvAuditedTitle = kpiTotalAudited.findViewById(R.id.tvTitle);
        tvVarianceTitle = kpiVariance.findViewById(R.id.tvTitle);
    }

    /** HARD-CODED DASHBOARD VALUES */
    private void loadHardcodedData() {

        // TOTAL STOCK
        tvStockTitle.setText("Total Stock");
        tvTotalStock.setText("500");

        // TOTAL SKUs
        tvSkusTitle.setText("Total SKUs");
        tvTotalSkus.setText("100");

        // AUDITED
        tvAuditedTitle.setText("Audited");
        tvTotalAudited.setText("100");

        // VARIANCE
        tvVarianceTitle.setText("Variance");
        tvVariance.setText("20");
    }

    /** OPTIONAL – KEEP EMPTY FOR NOW */
    private void setupCardClicks() {

        View.OnClickListener listener = v -> {
            // Future navigation
        };

        kpiTotalStock.setOnClickListener(listener);
        kpiTotalSkus.setOnClickListener(listener);
        kpiTotalAudited.setOnClickListener(listener);
        kpiVariance.setOnClickListener(listener);
    }



}
