package com.treewalker.rfidapp;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailsActivity extends BaseDrawerActivity {

    /** Stage Model **/
    public static class Stage {
        String name;
        String status;   // "completed", "in-progress", "pending"
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
        return R.layout.activity_product_details;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();
        bindViews();
        loadIntentData();
        loadTimeline();
    }

    /** Toolbar setup **/
    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Product Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /** View Binding **/
    private void bindViews() {
        productNameTv = findViewById(R.id.productName);
        productIdTv = findViewById(R.id.productId);
        productProgressBar = findViewById(R.id.productProgressBar);
        productProgressTextTv = findViewById(R.id.productProgressText);
        stagesTimelineLayout = findViewById(R.id.stagesTimelineLayout);
    }

    /** Get passed values **/
    private void loadIntentData() {
        String id = getIntent().getStringExtra("productId");
        String name = getIntent().getStringExtra("productName");
        String stage = getIntent().getStringExtra("productStage");
        int progress = getIntent().getIntExtra("productProgress", 0);

        productIdTv.setText(id != null ? id : "-");
        productNameTv.setText(name != null ? name : "-");
        productStatusTv.setText(stage != null ? stage : "-");

        productProgressBar.setProgress(progress);
        productProgressTextTv.setText(progress + "%");
    }

    /** Sample or API-based timeline **/
    private void loadTimeline() {
        List<Stage> stages = new ArrayList<>();
        stages.add(new Stage("Raw Material", "completed", "2025-09-01 08:00"));
        stages.add(new Stage("Cutting", "completed", "2025-09-01 14:30"));
        stages.add(new Stage("Assembly", "in-progress", "2025-09-02 09:15"));
        stages.add(new Stage("QC Check", "pending", ""));
        stages.add(new Stage("Packaging", "pending", ""));
        stages.add(new Stage("Shipping", "pending", ""));

        populateStagesTimeline(stages);
    }

    /** Dynamic Timeline Population **/
    private void populateStagesTimeline(List<Stage> stages) {

        stagesTimelineLayout.removeAllViews();

        for (Stage stage : stages) {

            MaterialCardView card = (MaterialCardView)
                    getLayoutInflater().inflate(R.layout.item_stage, stagesTimelineLayout, false);

            MaterialTextView nameTv = card.findViewById(R.id.stageName);
            MaterialTextView detailsTv = card.findViewById(R.id.stageDetails);

            // Set stage name
            nameTv.setText(stage.name);

            // Format status text
            String formattedStatus = stage.status != null ?
                    stage.status.replace("-", " ") : "Pending";

            formattedStatus =
                    formattedStatus.substring(0, 1).toUpperCase() + formattedStatus.substring(1);

            if (stage.timestamp != null && !stage.timestamp.isEmpty()) {
                formattedStatus += " • " + stage.timestamp;
            }

            detailsTv.setText(formattedStatus);

            // Apply color based on status
            int color;
            switch (stage.status) {
                case "completed":
                    color = ContextCompat.getColor(this, R.color.green_700);
                    break;
                case "in-progress":
                    color = ContextCompat.getColor(this, R.color.amber_600);
                    break;
                default:
                    color = ContextCompat.getColor(this, R.color.grey_600);
            }

            nameTv.setTextColor(color);
            detailsTv.setTextColor(color);

            stagesTimelineLayout.addView(card);
        }
    }

    /** Back button support **/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
