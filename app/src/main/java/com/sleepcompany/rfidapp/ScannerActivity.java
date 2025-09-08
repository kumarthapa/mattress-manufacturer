package com.sleepcompany.rfidapp;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

public class ScannerActivity extends BaseDrawerActivity {

    private MaterialButton startScanBtn, stopScanBtn, updateStageBtn, logProcessBtn;
    private MaterialCardView scanInstructionCard, scanResultCard;
    private MaterialTextView scannedTag, scannedProduct, scannedStage;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_scanner; // Your layout with DrawerLayout root
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startScanBtn = findViewById(R.id.startScanBtn);
        stopScanBtn = findViewById(R.id.stopScanBtn);
        updateStageBtn = findViewById(R.id.updateStageBtn);
        logProcessBtn = findViewById(R.id.logProcessBtn);
        scanInstructionCard = findViewById(R.id.scanInstructionCard);
        scanResultCard = findViewById(R.id.scanResultCard);
        scannedTag = findViewById(R.id.scannedTag);
        scannedProduct = findViewById(R.id.scannedProduct);
        scannedStage = findViewById(R.id.scannedStage);

        stopScanBtn.setVisibility(MaterialButton.GONE);
        scanInstructionCard.setVisibility(MaterialCardView.VISIBLE);
        scanResultCard.setVisibility(MaterialCardView.GONE);

        startScanBtn.setOnClickListener(v -> {
            startScanBtn.setVisibility(MaterialButton.GONE);
            stopScanBtn.setVisibility(MaterialButton.VISIBLE);
            scanInstructionCard.setVisibility(MaterialCardView.GONE);
            // TODO: Integrate actual RFID start scanning logic
        });

        stopScanBtn.setOnClickListener(v -> {
            stopScanBtn.setVisibility(MaterialButton.GONE);
            startScanBtn.setVisibility(MaterialButton.VISIBLE);
            // TODO: Integrate actual RFID stop scanning logic
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
}
