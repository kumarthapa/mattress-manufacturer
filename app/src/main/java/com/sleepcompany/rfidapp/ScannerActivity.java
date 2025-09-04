package com.sleepcompany.rfidapp;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

public class ScannerActivity extends AppCompatActivity {

    private MaterialButton startScanBtn, stopScanBtn, updateStageBtn, logProcessBtn;
    private MaterialCardView scanInstructionCard, scanResultCard;
    private MaterialTextView scannedTag, scannedProduct, scannedStage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        // Enable back button in ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("RFID Scanner");
        }

        // Bind Material components
        startScanBtn         = findViewById(R.id.startScanBtn);
        stopScanBtn          = findViewById(R.id.stopScanBtn);
        updateStageBtn       = findViewById(R.id.updateStageBtn);
        logProcessBtn        = findViewById(R.id.logProcessBtn);
        scanInstructionCard  = findViewById(R.id.scanInstructionCard);
        scanResultCard       = findViewById(R.id.scanResultCard);
        scannedTag           = findViewById(R.id.scannedTag);
        scannedProduct       = findViewById(R.id.scannedProduct);
        scannedStage         = findViewById(R.id.scannedStage);

        // Initial state
        stopScanBtn.setVisibility(MaterialButton.GONE);
        scanInstructionCard.setVisibility(MaterialCardView.VISIBLE);
        scanResultCard.setVisibility(MaterialCardView.GONE);

        startScanBtn.setOnClickListener(v -> {
            startScanBtn.setVisibility(MaterialButton.GONE);
            stopScanBtn.setVisibility(MaterialButton.VISIBLE);
            scanInstructionCard.setVisibility(MaterialCardView.GONE);
            // TODO: Integrate actual RFID start logic
        });

        stopScanBtn.setOnClickListener(v -> {
            stopScanBtn.setVisibility(MaterialButton.GONE);
            startScanBtn.setVisibility(MaterialButton.VISIBLE);
            // TODO: Integrate actual RFID stop logic
        });

        updateStageBtn.setOnClickListener(v -> simulateScanResult());

        logProcessBtn.setOnClickListener(v ->
                Snackbar.make(findViewById(android.R.id.content),
                        "Process logged successfully", Snackbar.LENGTH_SHORT).show()
        );
    }

    private void simulateScanResult() {
        scannedTag.setText("RF1234567890");
        scannedProduct.setText("Premium Memory Foam Mattress");
        scannedStage.setText("Assembly");
        scanResultCard.setVisibility(MaterialCardView.VISIBLE);
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
