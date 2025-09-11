package com.sleepcompany.rfidapp;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textview.MaterialTextView;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;
import com.sleepcompany.rfidapp.databinding.ActivityScannerBinding;
import com.sleepcompany.rfidapp.model.Product;
import com.sleepcompany.rfidapp.model.UpdateStageRequest;
import com.sleepcompany.rfidapp.network.ApiClient;
import com.sleepcompany.rfidapp.network.ApiService;
import com.sleepcompany.rfidapp.network.TagResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScannerActivity extends BaseDrawerActivity {

    private ActivityScannerBinding binding;
    private UHFService mDevice;
    private ActionBarDrawerToggle toggle;

    private MaterialButton startScanBtn, updateStageBtn, clearBtn;
    private CircularProgressIndicator scanProgress;
    private MaterialTextView scanStatusText;
    private MaterialTextView scannedTag;
    private MaterialTextView scannedProduct;
    private MaterialAutoCompleteTextView scannedStage, scannedQcStatus;
    private MaterialTextView scannedSize;
    private MaterialCardView scanResultCard;

    private boolean isScanning = false;
    private Thread scanThread;

    private Product currentProduct;
    private UpdateStageRequest currentTagId;
    private String lastScannedTagId;

    private static final int[] HARDWARE_KEYS = {142, KeyEvent.KEYCODE_F1, KeyEvent.KEYCODE_F2};

    private final String[] stages = {
            "Bonding", "Tapedge", "Zip Cover", "QC",
            "Packing", "Ready for Shipment", "Shipped", "Returned", "Cancelled"
    };

    private final String[] qcStatuses = {"PENDING", "PASS", "FAILED"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mDevice = UHFService.getInstance();

        setupToolbar();
        setupDrawer();
        initializeViews();
        setupDropdowns();
        setupClickListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupDrawer() {
        toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void initializeViews() {
        startScanBtn = binding.startScanBtn;
        updateStageBtn = binding.updateStageBtn;
        clearBtn = binding.clearBtn;

        scanProgress = binding.scanProgress;
        scanStatusText = binding.scanStatusText;
        scannedTag = binding.scannedTag;
        scannedProduct = binding.scannedProduct;
        scannedStage = (MaterialAutoCompleteTextView) binding.scannedStage;
        scannedQcStatus = (MaterialAutoCompleteTextView) binding.scannedQcStatus;
        scannedSize = binding.scannedSize;
        scanResultCard = binding.scanResultCard;

        updateStageBtn.setEnabled(false);
        clearBtn.setEnabled(false);
    }

    private void setupDropdowns() {
        ArrayAdapter<String> stageAdapter = new ArrayAdapter<>(this, R.layout.list_item, stages);
        ArrayAdapter<String> qcAdapter = new ArrayAdapter<>(this, R.layout.list_item, qcStatuses);

        scannedStage.setAdapter(stageAdapter);
        scannedQcStatus.setAdapter(qcAdapter);
    }

    private void setupClickListeners() {
        startScanBtn.setOnClickListener(v -> {
            if (!isScanning) startRFIDScan();
            else stopRFIDScan();
        });

        updateStageBtn.setOnClickListener(v -> {
            // Check if a product is currently selected
            Log.d("ScannerActivity", "Updating stage for product: " + currentProduct);
            if (currentProduct != null) {
                String selectedStage = scannedStage.getText().toString();
                String selectedQc = scannedQcStatus.getText().toString();
                updateProductStage(currentProduct.getTagId(), selectedStage, selectedQc);

            }
        });

        clearBtn.setOnClickListener(v -> clearUI());
    }

    private void startRFIDScan() {
        isScanning = true;
        updateScanUI(true);

        scanThread = new Thread(() -> {
            while (isScanning) {
                EPC epc = new EPC();
                if (mDevice.inventoryOnce(epc, 100)) {
                    String tagId = epc.getId();
                    if (tagId != null && !tagId.isEmpty()) {
                        runOnUiThread(() -> {
                            stopRFIDScan();
                            lastScannedTagId = tagId;
                            Log.d("RFID_SCAN", "Scanned Tag ID: " + tagId);
                            checkProductWithAPI(tagId);
                        });
                        break;
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        scanThread.start();
    }

    private void stopRFIDScan() {
        isScanning = false;
        if (scanThread != null) scanThread.interrupt();
        updateScanUI(false);
    }

    private void updateScanUI(boolean scanning) {
        if (scanning) {
            startScanBtn.setText("Stop Scan");
            scanProgress.setVisibility(android.view.View.VISIBLE);
            scanStatusText.setText("Scanning...");
            scanStatusText.setVisibility(android.view.View.VISIBLE);
            scanResultCard.setVisibility(android.view.View.GONE);
        } else {
            startScanBtn.setText("Start Scan");
            scanProgress.setVisibility(android.view.View.GONE);
            scanStatusText.setVisibility(android.view.View.GONE);
        }
    }

    private void checkProductWithAPI(String tagId) {
        scanStatusText.setText("Checking product...");
        scanStatusText.setVisibility(android.view.View.VISIBLE);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.getProductDetailsByTagId(tagId).enqueue(new Callback<TagResponse>() {
            @Override
            public void onResponse(Call<TagResponse> call, Response<TagResponse> response) {
                scanStatusText.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    currentProduct = response.body().getProduct();
                    displayProductDetails(tagId, currentProduct);
                } else {
                    showNoProductFound(tagId);
                }
            }

            @Override
            public void onFailure(Call<TagResponse> call, Throwable t) {
                scanStatusText.setVisibility(android.view.View.GONE);
                Toast.makeText(ScannerActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("API_ERROR", "Failed to fetch product", t);
            }
        });
    }

    private void displayProductDetails(String tagId, Product product) {
        scannedTag.setText(tagId);
        scannedProduct.setText(product.getProductName() + " (" + product.getSku() + ")");
        scannedSize.setText(product.getSize());

        // Set dropdown values
        scannedStage.setText(product.getCurrentStage(), false);
        scannedQcStatus.setText(product.getQcStatus(), false);

        scanResultCard.setVisibility(android.view.View.VISIBLE);
        updateStageBtn.setEnabled(true);
        clearBtn.setEnabled(true);
    }

    private void updateProductStage(String tagId, String stage, String qcStatus) {
        scanStatusText.setText("Updating stage...");
        scanStatusText.setVisibility(View.VISIBLE);

        String comments = "Updated via RFID app"; // Or get comments from UI
        String machineNo = null; // Or get machine no from UI if you have

        UpdateStageRequest request = new UpdateStageRequest(tagId, stage, qcStatus, comments, machineNo);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.updateProductStage(request).enqueue(new Callback<TagResponse>() {
            @Override
            public void onResponse(Call<TagResponse> call, Response<TagResponse> response) {
                scanStatusText.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    currentProduct = response.body().getProduct();
                    displayProductDetails(tagId, currentProduct);
                    Toast.makeText(ScannerActivity.this, "Stage updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ScannerActivity.this, "Failed to update stage", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<TagResponse> call, Throwable t) {
                scanStatusText.setVisibility(View.GONE);
                Toast.makeText(ScannerActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("API_ERROR", "Failed to update stage", t);
            }
        });
    }




    private void showNoProductFound(String tagId) {
        scannedTag.setText(tagId);
        scannedProduct.setText("No product found");
        scannedSize.setText("Unknown");
        scannedStage.setText("Unknown", false);
        scannedQcStatus.setText("Unknown", false);
        scanResultCard.setVisibility(android.view.View.VISIBLE);

        updateStageBtn.setEnabled(false);
        clearBtn.setEnabled(true);

        Toast.makeText(this, "No product found with this RFID tag", Toast.LENGTH_LONG).show();
    }

    private void clearUI() {
        scannedTag.setText("-");
        scannedProduct.setText("-");
        scannedStage.setText("", false);
        scannedQcStatus.setText("", false);
        scannedSize.setText("-");
        scanResultCard.setVisibility(android.view.View.GONE);

        updateStageBtn.setEnabled(false);
        clearBtn.setEnabled(false);

        currentProduct = null;
        lastScannedTagId = null;
        Toast.makeText(this, "Cleared product details", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        for (int hwKey : HARDWARE_KEYS) {
            if (keyCode == hwKey) {
                if (!isScanning) startRFIDScan();
                else stopRFIDScan();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return toggle != null && toggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_scanner;
    }

    @Override
    protected boolean useDataBinding() {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDevice != null && !mDevice.open()) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRFIDScan();
        if (mDevice != null) mDevice.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRFIDScan();
    }
}
