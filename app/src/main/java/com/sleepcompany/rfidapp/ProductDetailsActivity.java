package com.sleepcompany.rfidapp;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class ProductDetailsActivity extends BaseDrawerActivity {

    public static class Stage {
        String name;
        String status; // "completed", "in-proMyApp.javagress", "pending"
        String timestamp;

        public Stage(String name, String status, String timestamp) {
            this.name = name;
            this.status = status;
            this.timestamp = timestamp;
        }
    }

    private MaterialTextView productNameTv, productIdTv, productStatusTv, productProgressTextTv;
    private LinearProgressIndicator productProgressBar;
    private android.widget.LinearLayout stagesTimelineLayout;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_product_details; // Layout with DrawerLayout, Toolbar, NavigationView
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable back arrow in the toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Product Details");
        }

        // View bindings
        productNameTv = findViewById(R.id.productName);
        productIdTv = findViewById(R.id.productId);
        productStatusTv = findViewById(R.id.productStatus);
        productProgressBar = findViewById(R.id.productProgressBar);
        productProgressTextTv = findViewById(R.id.productProgressText);
        stagesTimelineLayout = findViewById(R.id.stagesTimelineLayout);

        // Retrieve Intent extras
        String idExtra = getIntent().getStringExtra("productId");
        String nameExtra = getIntent().getStringExtra("productName");
        String stageExtra = getIntent().getStringExtra("productStage");
        int progressExtra = getIntent().getIntExtra("productProgress", 0);

        productIdTv.setText(idExtra != null ? idExtra : "-");
        productNameTv.setText(nameExtra != null ? nameExtra : "-");
        productStatusTv.setText(stageExtra != null ? stageExtra : "-");
        productProgressBar.setProgress(progressExtra);
        productProgressTextTv.setText(progressExtra + "%");

        // Example stages
        List<Stage> stages = List.of(
                new Stage("Raw Material", "completed", "2025-09-01 08:00"),
                new Stage("Cutting", "completed", "2025-09-01 14:30"),
                new Stage("Assembly", "in-progress", "2025-09-02 09:15"),
                new Stage("QC Check", "pending", ""),
                new Stage("Packaging", "pending", ""),
                new Stage("Shipping", "pending", "")
        );
        populateStagesTimeline(stages);
    }

    private void populateStagesTimeline(List<Stage> stages) {
        stagesTimelineLayout.removeAllViews();
        for (Stage stage : stages) {
            MaterialCardView card = (MaterialCardView) getLayoutInflater()
                    .inflate(R.layout.item_stage, stagesTimelineLayout, false);
            MaterialTextView nameTv = card.findViewById(R.id.stageName);
            MaterialTextView detailsTv = card.findViewById(R.id.stageDetails);

            nameTv.setText(stage.name);
            String status = stage.status.replace("-", " ");
            status = status.substring(0, 1).toUpperCase() + status.substring(1);
            if (!stage.timestamp.isEmpty()) {
                status += " • " + stage.timestamp;
            }
            detailsTv.setText(status);

            int color;
            switch (stage.status) {
                case "completed":
                    color = getColor(R.color.green_700);
                    break;
                case "in-progress":
                    color = getColor(R.color.amber_600);
                    break;
                default:
                    color = getColor(R.color.grey_600);
                    break;
            }
            nameTv.setTextColor(color);
            detailsTv.setTextColor(color);

            stagesTimelineLayout.addView(card);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
