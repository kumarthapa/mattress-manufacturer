package com.sleepcompany.rfidapp;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class ProductDetailsActivity extends AppCompatActivity {

    public static class Stage {
        String name;
        String status; // "completed", "in-progress", "pending"
        String timestamp;

        public Stage(String name, String status, String timestamp) {
            this.name = name;
            this.status = status;
            this.timestamp = timestamp;
        }
    }

    private MaterialTextView productNameTv, productIdTv, productSizeTv,
            productRfidTv, productStatusTv, productProgressTextTv;
    private LinearProgressIndicator productProgressBar;
    private MaterialCardView infoCard;
    private MaterialCardView stageItemCard;
    private com.google.android.material.textview.MaterialTextView stageNameTv;
    private com.google.android.material.textview.MaterialTextView stageDetailsTv;
    private com.google.android.material.textview.MaterialTextView stageSeparator;
    private com.google.android.material.textview.MaterialTextView stageSeparatorLine;
    private com.google.android.material.textview.MaterialTextView stageSpacer;
    private com.google.android.material.textview.MaterialTextView stageSpacing;



    private com.google.android.material.textview.MaterialTextView stageSeparatorBottom;
    private android.widget.LinearLayout stagesTimelineLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        // Enable back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Product Details");
        }

        // Bind views
        productNameTv        = findViewById(R.id.productName);
        productIdTv          = findViewById(R.id.productId);
        productSizeTv        = findViewById(R.id.productSize);
        productRfidTv        = findViewById(R.id.productRfid);
        productStatusTv      = findViewById(R.id.productStatus);
        productProgressBar   = findViewById(R.id.productProgressBar);
        productProgressTextTv= findViewById(R.id.productProgressText);
        stagesTimelineLayout = findViewById(R.id.stagesTimelineLayout);

        // Populate header data
        String idExtra    = getIntent().getStringExtra("productId");
        String nameExtra  = getIntent().getStringExtra("productName");
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
            MaterialTextView nameTv    = card.findViewById(R.id.stageName);
            MaterialTextView detailsTv = card.findViewById(R.id.stageDetails);

            nameTv.setText(stage.name);
            String status = stage.status.replace("-", " ");
            status = status.substring(0,1).toUpperCase() + status.substring(1);
            if (!stage.timestamp.isEmpty()) {
                status += " • " + stage.timestamp;
            }
            detailsTv.setText(status);

            int color;
            switch (stage.status) {
                case "completed":    color = getColor(R.color.green_700); break;
                case "in-progress":  color = getColor(R.color.amber_600); break;
                default:             color = getColor(R.color.grey_600);  break;
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
