package com.sleepcompany.rfidapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;
import com.sleepcompany.rfidapp.databinding.ActivityScannerBinding;
import com.sleepcompany.rfidapp.model.Product;
import com.sleepcompany.rfidapp.model.StagesStatusRequest;
import com.sleepcompany.rfidapp.model.UpdateProductDetailsRequest;
import com.sleepcompany.rfidapp.model.UpdateStageRequest;
import com.sleepcompany.rfidapp.network.ApiClient;
import com.sleepcompany.rfidapp.network.ApiService;
import com.sleepcompany.rfidapp.network.StagesStatusResponse;
import com.sleepcompany.rfidapp.network.TagResponse;
import com.sleepcompany.rfidapp.util.PrefHelper;
import com.sleepcompany.rfidapp.util.PrinterManager;
import com.sleepcompany.rfidapp.util.SnackbarHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ScannerActivity - updated UI + reject flow + remarks + defects selection + failed-card
 */
public class ScannerActivity extends AppCompatActivity {

    private ActivityScannerBinding binding;
    private UHFService mDevice;

    private MaterialButton startScanBtn, updateStageBtn, rejectBtn;
    private CircularProgressIndicator scanProgress;
    private MaterialTextView scanStatusText;

    private MaterialTextView qaCode;
    private MaterialTextView scannedProduct;
    private MaterialTextView scannedProductSKU;

    // ADD these SKU edit fields:
    private ImageButton ivEditProductSKU;
    private com.google.android.material.textfield.TextInputLayout productSkuEditLayout;
    private TextInputEditText etProductSkuEdit;
    private LinearLayout productSkuEditButtons;
    private MaterialButton btnSaveSku, btnCancelSkuEdit;


    private MaterialAutoCompleteTextView scannedStage, scannedQcStatus;
    private MaterialTextView scannedSize;
    private MaterialTextView scannedStatus;
    private MaterialCardView scanResultCard;

    // failed card views
    private MaterialCardView failedResultCard;
    private MaterialTextView failedQaCode, failedProduct, failedSize, failedStage, failedQcStatus, failedRemarks;
    private LinearLayout llFailedDefectsContainer;
    private ImageButton ivFailedCardPrint, ivFailedCardClear;

    // extra UI (editable card)
    private TextInputEditText etRemarks;
    private LinearLayout llDefectsContainer;
    private MaterialCardView defectsCard;
    private ImageButton ivCardClear; // small clear icon on editable card
    private ImageButton ivCardPrint; // small print icon on editable card

    private boolean isScanning = false;
    private Thread scanThread;

    private Product selectedProduct;
    private String lastScannedTagId;
    private Button ivFailedCardRework;

    // Keep maps returned by backend (key -> displayValue)
    private Map<String, String> lastStagesMap = null;
    private Map<String, String> lastStatusMap = null;

    // Keep defect points returned from backend keyed by stage key
    private Map<String, List<Map<String, String>>> lastDefectsMap = new HashMap<>();

    private static final int[] HARDWARE_KEYS = {142, KeyEvent.KEYCODE_F1, KeyEvent.KEYCODE_F2};

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private BluetoothAdapter btAdapter;

    private final static int REQUEST_BT_ENABLE = 0;
    private final static int REQUEST_BT_ADDR = 1;
    private boolean mBtOpenSilent = true;

    // Printer manager (reusable)
    private PrinterManager printerManager;

    // ---------------------- Inline product edit fields --------------
    private ImageButton ivEditProduct;
    private com.google.android.material.textfield.TextInputLayout productEditLayout;
    private TextInputEditText etProductNameEdit;
    private LinearLayout productEditButtons;
    private MaterialButton btnSaveProduct, btnCancelProductEdit;


    // Executor for background printer connect (so we don't block UI)
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    // Keep the allowed-stage keys returned by backend for the currently loaded product
    private List<String> lastAllowedStageKeys = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        initializeViews();
        setupDropdowns();
        setupClickListeners();

        // Initialize RFID hardware safely
        try {
            mDevice = UHFService.getInstance();
            if (mDevice == null) {
                throw new Exception("UHF hardware not available on this device");
            }
            initializeRFIDPower();
        } catch (Exception e) {
            // Handle unsupported device
            mDevice = null;
            Toast.makeText(this,
                    "⚠ This device does not support UHF RFID scanning.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        printerManager = new PrinterManager(this, this, new PrinterManager.PrinterCallback() {
            @Override
            public void onPrinterConnected(@NonNull String deviceInfo) {
                Toast.makeText(ScannerActivity.this, "Printer connected: " + deviceInfo, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrinterDisconnected() {
                Toast.makeText(ScannerActivity.this, "Printer disconnected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrintSuccess() {
                Toast.makeText(ScannerActivity.this, "Print succeeded", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrintError(@NonNull String error) {
                Toast.makeText(ScannerActivity.this, "Print error: " + error, Toast.LENGTH_LONG).show();
            }
        });

        openBuletooth();
        autoConnectSavedPrinterIfNeeded();
        // init printer manager


        // Try to auto-connect to saved printer (if any)
        //autoConnectSavedPrinterIfNeeded();
    }

    private void autoConnectSavedPrinterIfNeeded() {
        final String savedMac = PrefHelper.getPrinterMac(this);
        if (savedMac == null || savedMac.isEmpty()) {
            Log.d("PRINTER", "No saved printer mac to auto-connect.");
            return;
        }

        // If already connected, nothing to do
        if (printerManager != null && printerManager.isConnected()) {
            Log.d("PRINTER", "Already connected to printer: " + printerManager.getDeviceInfo());
            return;
        }

        // If Bluetooth not available/enabled, try enabling silently (or notify)
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Log.d("PRINTER", "No Bluetooth hardware available; cannot auto connect.");
            return;
        }

        if (!btAdapter.isEnabled()) {
            // Try to enable silently if allowed
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                boolean enabled = btAdapter.enable();
                Log.d("PRINTER", "Requested Bluetooth enable: " + enabled);
                // Wait a little for BT stack to spin up before connecting
                mainHandler.postDelayed(this::connectToSavedPrinterBackground, 800);
                return;
            } else {
                // no permission to enable; inform user
                Toast.makeText(this, "Enable Bluetooth to connect saved printer", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Bluetooth enabled -> connect in background
        connectToSavedPrinterBackground();
    }
    private void initializeRFIDPower() {
        if (mDevice == null) return;
        int savedPower = PrefHelper.getRfidPower(this);
        try {
            Method m = mDevice.getClass().getMethod("setPower", int.class);
            m.invoke(mDevice, savedPower);
            Log.d("RFID_POWER", "RFID Power applied: " + savedPower + " dBm");
        } catch (Exception e) {
            Log.w("RFID_POWER", "Failed to apply saved power: " + e.getMessage());
        }
    }

    private void connectToSavedPrinterBackground() {
        if (printerManager == null) return;
        bgExecutor.execute(() -> {
            Log.d("PRINTER", "Auto connecting to saved printer...");
            // mainHandler.post(() -> Toast.makeText(ScannerActivity.this, "Connecting to saved printer...", Toast.LENGTH_SHORT).show());
            try {
                printerManager.connectBluetooth();
            } catch (Exception e) {
                Log.e("PRINTER", "Auto connect exception: " + e.getMessage(), e);
                mainHandler.post(() -> Toast.makeText(ScannerActivity.this, "Auto connect failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void openBuletooth()
    {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d("Bluetooth", "btAdapter: " + btAdapter);
        if (btAdapter == null) {
            Toast.makeText(this, "No Found Bluetooth Hardware", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // If BT not enabled, either request or rely on BtConfigActivity to do so.
        if (!btAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth Openint", Toast.LENGTH_SHORT).show();
            if (!mBtOpenSilent)
            {
                Intent mIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(mIntent,REQUEST_BT_ENABLE );
            }
            else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // Caller should request BLUETOOTH_CONNECT on Android 12+ if needed.
                    return;
                }
                btAdapter.enable();
                Toast.makeText(this, "Bluetooth Open", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Bluetooth Open", Toast.LENGTH_SHORT).show();
        }
    }

    private BroadcastReceiver dynamicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("cp31.printerstatus")){
                String msg = intent.getStringExtra("msg");
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @SuppressLint("WrongConstant")
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
            {
                Toast.makeText(context,"Bluetooth Disconnect",Toast.LENGTH_SHORT).show();
            }
        };
    };

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            getSupportActionBar().setTitle("Scan RFID Tag");
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        startScanBtn = binding.startScanBtn;
        updateStageBtn = binding.updateStageBtn;
        rejectBtn = binding.rejectBtn;

        scanProgress = binding.scanProgress;
        scanStatusText = binding.scanStatusText;

        qaCode = binding.qaCode;
        scannedProduct = binding.scannedProduct;
        scannedProductSKU = binding.scannedProductSKU;


        scannedStage = (MaterialAutoCompleteTextView) binding.scannedStage;
        scannedQcStatus = (MaterialAutoCompleteTextView) binding.scannedQcStatus;

        scannedSize = binding.scannedSize;
        scannedStatus = binding.scannedStatus;
        scanResultCard = binding.scanResultCard;

        // failed card bindings
        failedResultCard = binding.failedResultCard;
        failedQaCode = binding.failedQaCode;
        failedProduct = binding.failedProduct;
        failedSize = binding.failedSize;
        failedStage = binding.failedStage;
        failedQcStatus = binding.failedQcStatus;
        failedRemarks = binding.failedRemarks;
        llFailedDefectsContainer = binding.llFailedDefectsContainer;
        ivFailedCardPrint = binding.ivFailedCardPrint;
        ivFailedCardClear = binding.ivFailedCardClear;

        ivFailedCardRework = binding.ivFailedCardRework;
        ivFailedCardRework.setVisibility(View.GONE);


        etRemarks = binding.etRemarks;
        llDefectsContainer = binding.llDefectsContainer;
        defectsCard = binding.defectsCard;
        ivCardClear = binding.ivCardClear; // new clear icon
        ivCardPrint = binding.ivCardPrint; // new print icon
        ivFailedCardClear = binding.ivFailedCardClear; // new clear icon
        // prevent keyboard for dropdowns
        scannedStage.setInputType(InputType.TYPE_NULL);
        scannedQcStatus.setInputType(InputType.TYPE_NULL);

        updateStageBtn.setEnabled(false);
        rejectBtn.setEnabled(false);

        // initially hide icons and defects card
        ivCardPrint.setVisibility(View.GONE);
        ivCardClear.setVisibility(View.GONE);
        ivFailedCardClear.setVisibility(View.GONE);
        defectsCard.setVisibility(View.GONE);


        // product edit views
        ivEditProduct = binding.ivEditProduct;
        productEditLayout = binding.productEditLayout;
        etProductNameEdit = binding.etProductNameEdit;
        productEditButtons = binding.productEditButtons;
        btnSaveProduct = binding.btnSaveProduct;
        btnCancelProductEdit = binding.btnCancelProductEdit;

        // SKU edit views (ensure IDs exist in your layout)
        ivEditProductSKU = binding.ivEditProductSKU;
        productSkuEditLayout = binding.productSkuEditLayout;
        etProductSkuEdit = binding.etProductSkuEdit;
        productSkuEditButtons = binding.productSkuEditButtons;
        btnSaveSku = binding.btnSaveSku;
        btnCancelSkuEdit = binding.btnCancelSkuEdit;

        // initial state -- KUMAR----------------
        productEditLayout.setVisibility(View.GONE);
        productEditButtons.setVisibility(View.GONE);
        // initially hide failed card
        if (failedResultCard != null) failedResultCard.setVisibility(View.GONE);
        if (ivFailedCardPrint != null) ivFailedCardPrint.setVisibility(View.GONE);

        if (productSkuEditLayout != null) productSkuEditLayout.setVisibility(View.GONE);
        if (productSkuEditButtons != null) productSkuEditButtons.setVisibility(View.GONE);

        // listeners for selection to enable update and control defects/reject behavior
        scannedStage.setOnItemClickListener((parent, view, position, id) -> {
            evaluateUpdateButtonState();
            handleQcStageSelection();
        });
        scannedQcStatus.setOnItemClickListener((parent, view, position, id) -> {
            evaluateUpdateButtonState();
            handleQcStageSelection();
        });

        // failed card button handlers (print/clear)
        if (ivFailedCardPrint != null) {
            ivFailedCardPrint.setOnClickListener(v -> {
                if (printerManager != null && printerManager.isConnected()) {
                    showConfirmPrintDialog(selectedProduct);
                } else {
                    Toast.makeText(ScannerActivity.this, "Printer not connected", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (ivFailedCardClear != null) {
            ivFailedCardClear.setOnClickListener(v -> clearUI());
        }
    }

    private void setupDropdowns() {
        // load full config on start
        fetchStagesAndStatuses(null, null, null);
    }

    private void setupClickListeners() {
        startScanBtn.setOnClickListener(v -> {
            if (!isScanning) startRFIDScan();
            else stopRFIDScan();
        });

        updateStageBtn.setOnClickListener(v -> onUpdateClicked());

        // at end of setupClickListeners() --- kumar
        setupProductEditorListeners();

        if (ivFailedCardRework != null) {
            ivFailedCardRework.setOnClickListener(v -> onReworkClicked());
        }


        // Make Reject behave like Update but force FAIL and then clear card
        rejectBtn.setOnClickListener(v -> onRejectClicked());

        ivCardClear.setOnClickListener(v -> clearUI());
        ivFailedCardClear.setOnClickListener(v -> clearUI());

        ivCardPrint.setOnClickListener(v -> {

            if (printerManager != null && printerManager.isConnected()) {
                showConfirmPrintDialog(selectedProduct);
            } else {
                Log.d("PRINTER", "Printer not connected");
                Toast.makeText(this, "Printer not connected", Toast.LENGTH_SHORT).show();
            }
        });

        // Touch handlers to show dropdowns
        scannedStage.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP && !isFinishing() && !isDestroyed()) {
                scannedStage.post(() -> {
                    if (!isFinishing() && !isDestroyed()) scannedStage.showDropDown();
                });
            }
            return false;
        });
        scannedQcStatus.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP && !isFinishing() && !isDestroyed()) {
                scannedQcStatus.post(() -> {
                    if (!isFinishing() && !isDestroyed()) scannedQcStatus.showDropDown();
                });
            }
            return false;
        });
    }

//    --------------- kumar----------------
private void setupProductEditorListeners() {
    // PRODUCT NAME editor (existing)
    if (ivEditProduct != null) {
        ivEditProduct.setOnClickListener(v -> {
            String current = scannedProduct == null ? "" : scannedProduct.getText().toString();
            etProductNameEdit.setText(current.equals("-") ? "" : current);
            productEditLayout.setVisibility(View.VISIBLE);
            productEditButtons.setVisibility(View.VISIBLE);
            etProductNameEdit.requestFocus();
            showKeyboard(etProductNameEdit);

            // hide SKU editor if visible
            if (productSkuEditLayout != null) productSkuEditLayout.setVisibility(View.GONE);
            if (productSkuEditButtons != null) productSkuEditButtons.setVisibility(View.GONE);
        });
    }

    if (btnCancelProductEdit != null) {
        btnCancelProductEdit.setOnClickListener(v -> hideProductEditor());
    }

    if (btnSaveProduct != null) {
        btnSaveProduct.setOnClickListener(v -> {
            String newName = etProductNameEdit.getText() == null ? "" : etProductNameEdit.getText().toString().trim();

            if (newName.isEmpty()) {
                productEditLayout.setError("Product name cannot be empty");
                etProductNameEdit.requestFocus();
                return;
            }

            String current = scannedProduct == null ? "" : scannedProduct.getText().toString();
            if (newName.equals(current)) {
                hideProductEditor();
                return;
            }

            btnSaveProduct.setEnabled(false);
            productEditLayout.setError(null);

            // Use unified API call: pass productName, null sku
            saveProductFields(lastScannedTagId, newName, null, () -> {
                // onSuccess callback - already handled inside saveProductFields; nothing needed
            }, (errorMsg) -> {
                // onError callback - re-enable button
                runOnUiThread(() -> {
                    btnSaveProduct.setEnabled(true);
                    productEditLayout.setError(errorMsg);
                });
            });
        });
    }

    // IME action for product name
    etProductNameEdit.setOnEditorActionListener((v, actionId, event) -> {
        if (btnSaveProduct != null) btnSaveProduct.performClick();
        return true;
    });

    // ---------------------------
    // SKU editor listeners
    // ---------------------------
    if (ivEditProductSKU != null) {
        ivEditProductSKU.setOnClickListener(v -> {
            String currentSku = scannedProductSKU == null ? "" : scannedProductSKU.getText().toString();
            etProductSkuEdit.setText(currentSku.equals("-") ? "" : currentSku);
            productSkuEditLayout.setVisibility(View.VISIBLE);
            productSkuEditButtons.setVisibility(View.VISIBLE);
            etProductSkuEdit.requestFocus();
            showKeyboard(etProductSkuEdit);

            // hide product name editor if visible
            if (productEditLayout != null) productEditLayout.setVisibility(View.GONE);
            if (productEditButtons != null) productEditButtons.setVisibility(View.GONE);
        });
    }

    if (btnCancelSkuEdit != null) {
        btnCancelSkuEdit.setOnClickListener(v -> {
            if (productSkuEditLayout != null) productSkuEditLayout.setVisibility(View.GONE);
            if (productSkuEditButtons != null) productSkuEditButtons.setVisibility(View.GONE);
            if (etProductSkuEdit != null) etProductSkuEdit.setText("");
            hideKeyboard(etProductSkuEdit);
            if (btnSaveSku != null) btnSaveSku.setEnabled(true);
        });
    }

    if (btnSaveSku != null) {
        btnSaveSku.setOnClickListener(v -> {
            String newSku = etProductSkuEdit.getText() == null ? "" : etProductSkuEdit.getText().toString().trim();

            if (newSku.isEmpty()) {
                productSkuEditLayout.setError("SKU cannot be empty");
                etProductSkuEdit.requestFocus();
                return;
            }

            String currentSku = scannedProductSKU == null ? "" : scannedProductSKU.getText().toString();
            if (newSku.equals(currentSku)) {
                // nothing changed
                if (productSkuEditLayout != null) productSkuEditLayout.setVisibility(View.GONE);
                if (productSkuEditButtons != null) productSkuEditButtons.setVisibility(View.GONE);
                return;
            }

            btnSaveSku.setEnabled(false);
            productSkuEditLayout.setError(null);

            // Use unified API call: pass sku, null productName
            saveProductFields(lastScannedTagId, null, newSku, () -> {
                // on success handled inside saveProductFields
            }, (errorMsg) -> {
                // on error re-enable ui
                runOnUiThread(() -> {
                    btnSaveSku.setEnabled(true);
                    productSkuEditLayout.setError(errorMsg);
                });
            });
        });

        // IME action for SKU edit -> press done triggers save
        etProductSkuEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (btnSaveSku != null) btnSaveSku.performClick();
            return true;
        });
    }
}

    private void startRFIDScan() {
        if (mDevice == null) {
            Toast.makeText(this, "UHF device not available", Toast.LENGTH_SHORT).show();
            return;
        }
        // Try to sync power right before scanning (in case another app changed it)
        syncRfidPowerBeforeScan();
        isScanning = true;
        updateScanUI(true);

        scanThread = new Thread(() -> {
            while (isScanning) {
                try {
                    EPC epc = new EPC();
                    if (mDevice != null && mDevice.inventoryOnce(epc, 150)) {
                        String tagId = epc.getId();
                        if (tagId != null && !tagId.isEmpty()) {
                            runOnUiThread(() -> {
                                stopRFIDScan();
                                lastScannedTagId = tagId;
                                Log.d("RFID_SCAN", "Scanned Tag ID: " + tagId);

                                // ✅ Play success sound
                                playScanSuccessSound();

                                // Proceed with API check
                                checkProductWithAPI(tagId);
                            });
                            break;
                        }
                    }
                } catch (Exception e) {
                    Log.e("RFID_SCAN", "inventory exception", e);
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        });
        scanThread.start();
        // 🚀 AUTO STOP AFTER 5 SECONDS
        mainHandler.postDelayed(() -> {
            if (isScanning) {
                Log.d("RFID_SCAN", "Auto-stop: Timeout reached");
                stopRFIDScan();
                Toast.makeText(ScannerActivity.this, "Scan timeout (4 seconds)", Toast.LENGTH_SHORT).show();
            }
        }, 4000);
        autoConnectSavedPrinterIfNeeded();
    }
    private void playScanSuccessSound() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.scan);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e("RFID_SCAN", "Error playing sound: " + e.getMessage());
        }
    }


    private void stopRFIDScan() {
        isScanning = false;
        if (scanThread != null) scanThread.interrupt();
        updateScanUI(false);
    }

    private void updateScanUI(boolean scanning) {
        if (scanning) {
            startScanBtn.setText("Stop Scan");
            scanProgress.setVisibility(View.VISIBLE);
            scanStatusText.setText("Scanning...");
            scanStatusText.setVisibility(View.VISIBLE);
            scanResultCard.setVisibility(View.GONE);
            if (failedResultCard != null) failedResultCard.setVisibility(View.GONE);
            ivCardPrint.setVisibility(View.GONE);
            ivCardClear.setVisibility(View.GONE);
            if (ivFailedCardPrint != null) ivFailedCardPrint.setVisibility(View.GONE);
            if (ivFailedCardClear != null) ivFailedCardClear.setVisibility(View.GONE);
        } else {
            startScanBtn.setText("Start Scan");
            scanProgress.setVisibility(View.GONE);
            scanStatusText.setVisibility(View.GONE);
        }
    }

    private void checkProductWithAPI(String tagId) {
        scanStatusText.setText("Checking product...");
        scanStatusText.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.getProductDetailsByTagId(tagId).enqueue(new Callback<TagResponse>() {
            @Override
            public void onResponse(Call<TagResponse> call, Response<TagResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                scanStatusText.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    selectedProduct = response.body().getProduct();
                    lastScannedTagId = tagId;

                    // Determine status (guard nulls)
                    String latestStatus = selectedProduct.getLatestStatus();
                    latestStatus = latestStatus == null ? "" : latestStatus.trim();

                    // If FAIL -> show readonly failed card, else show interactive product details
                    if ("FAIL".equalsIgnoreCase(latestStatus)) {
                        displayFailedProductDetails(tagId, selectedProduct);
                    } else {
                        displayProductDetails(tagId, selectedProduct);
                    }
                } else {
                    showNoProductFound(tagId);
                }
            }

            @Override
            public void onFailure(Call<TagResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                scanStatusText.setVisibility(View.GONE);
                Toast.makeText(ScannerActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("API_ERROR", "Failed to fetch product", t);
            }
        });
    }


    private void displayProductDetails(String tagId, Product product) {
        if (product == null) return;

        // ensure failed card hidden
        if (failedResultCard != null) failedResultCard.setVisibility(View.GONE);

        // Basic fields
        if (qaCode != null) qaCode.setText(product.getQAcode());
        if (scannedProduct != null) scannedProduct.setText(product.getProductName());
        if (scannedProductSKU != null) scannedProductSKU.setText(product.getSku());
        if (scannedSize != null) scannedSize.setText(product.getSize() == null ? "-" : product.getSize());
        if (scannedStatus != null) scannedStatus.setText(product.getLatestStatus() == null ? "-" : product.getLatestStatus());

        // Locking by stage (packaging etc)
        applyLockingByStage(product.getLatestStage());

        // Also hide / show inline edit icons depending on stage (packaging => hide)
        updateProductEditIconsVisibility(product.getLatestStage());

        String qc_status = product.getLatestStatus() == null ? "PENDING" : product.getLatestStatus();
        String remarks = product.getLatestRemarks() == null ? "" : product.getLatestRemarks();
        if (etRemarks != null) {
            etRemarks.setText(remarks);
            etRemarks.setEnabled(true); // editable for non-failed
        }

        // Refresh dropdowns and defects for this product (fetch will call handleQcStageSelection after adapters set)
        fetchStagesAndStatuses(product.getLatestStage(), qc_status, remarks);

        // Show card & icons
        if (scanResultCard != null) scanResultCard.setVisibility(View.VISIBLE);
        if (ivCardPrint != null) ivCardPrint.setVisibility(View.VISIBLE);
        if (ivCardClear != null) ivCardClear.setVisibility(View.VISIBLE);

        // Ensure update/reject are visible and interactive for non-failed products
        if (updateStageBtn != null) {
            updateStageBtn.setVisibility(View.VISIBLE);
            updateStageBtn.setEnabled(!isStageLocked(product.getLatestStage()) &&
                    (scannedStage.getText() != null && scannedStage.getText().toString().trim().length() > 0));
        }
        if (rejectBtn != null) {
            rejectBtn.setVisibility(View.VISIBLE);
            // actual enabled/disabled logic will be handled by handleQcStageSelection()
            rejectBtn.setEnabled(true);
        }

        // Make dropdowns interactive again and restore their touch listeners
        scannedStage.setEnabled(true);
        scannedQcStatus.setEnabled(true);

        scannedStage.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP && !isFinishing() && !isDestroyed()) {
                scannedStage.post(() -> {
                    if (!isFinishing() && !isDestroyed()) scannedStage.showDropDown();
                });
            }
            return false;
        });
        scannedQcStatus.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP && !isFinishing() && !isDestroyed()) {
                scannedQcStatus.post(() -> {
                    if (!isFinishing() && !isDestroyed()) scannedQcStatus.showDropDown();
                });
            }
            return false;
        });
    }


    private void displayFailedProductDetails(String tagId, Product product) {
        if (product == null) return;

        // Keep selectedProduct and lastScannedTagId so print/clear still work
        selectedProduct = product;
        lastScannedTagId = tagId;

        // hide editable card and show failed card
        if (scanResultCard != null) scanResultCard.setVisibility(View.GONE);
        if (failedResultCard != null) failedResultCard.setVisibility(View.VISIBLE);


        // Make sure print/clear icons are visible for failed card
        if (ivFailedCardPrint != null) {
            ivFailedCardPrint.setVisibility(printerManager != null && printerManager.isConnected() ? View.VISIBLE : View.GONE);
        }
        if (ivFailedCardClear != null) ivFailedCardClear.setVisibility(View.VISIBLE);

// show rework button for failed flow
        if (ivFailedCardRework != null) ivFailedCardRework.setVisibility(View.VISIBLE);


        // Populate failed card fields
        populateFailedCard(product);

        // Make sure print/clear icons are visible for failed card
        if (ivFailedCardPrint != null) {
            // show if printer available
            ivFailedCardPrint.setVisibility(printerManager != null && printerManager.isConnected() ? View.VISIBLE : View.GONE);
        }
        if (ivFailedCardClear != null) ivFailedCardClear.setVisibility(View.VISIBLE);

        // fetch mappings so names / defects mapping is up-to-date
        String qc_status = product.getLatestStatus() == null ? "PENDING" : product.getLatestStatus();
        fetchStagesAndStatuses(product.getLatestStage(), qc_status, product.getLatestRemarks());
    }

    /**
     * Fill the failed result card (read-only)
     */
    private void populateFailedCard(Product product) {
        if (product == null) return;

        if (failedQaCode != null) failedQaCode.setText(product.getQAcode() == null ? "-" : product.getQAcode());
        if (failedProduct != null) failedProduct.setText(product.getProductName() == null ? "-" : product.getProductName());
        if (failedSize != null) failedSize.setText(product.getSize() == null ? "-" : product.getSize());

        // Stage label mapping if available
        String displayStage = product.getLatestStage();
        if (product.getLatestStage() != null && lastStagesMap != null) {
            String mapped = getValueForKey(lastStagesMap, product.getLatestStage());
            if (mapped != null) displayStage = mapped;
        }
        if (failedStage != null) failedStage.setText(displayStage == null ? "-" : displayStage);

        // QC status label mapping if available
        String displayQc = product.getLatestStatus();
        if (product.getLatestStatus() != null && lastStatusMap != null) {
            String mapped = getValueForKey(lastStatusMap, product.getLatestStatus());
            if (mapped != null) displayQc = mapped;
        }
        if (failedQcStatus != null) failedQcStatus.setText(displayQc == null ? "-" : displayQc);

        if (failedRemarks != null) failedRemarks.setText(product.getLatestRemarks() == null ? "-" : product.getLatestRemarks());

        // Fill defects into failed defects container (read-only)
        populateFailedDefects(product.getLatestStage(), product.getLatestDefectsPoints());
    }

    /**
     * Populate read-only defect list for failed card
     */
    private void populateFailedDefects(String stageKey, List<String> defectKeys) {
        if (llFailedDefectsContainer == null) return;
        llFailedDefectsContainer.removeAllViews();

        List<String> names = new ArrayList<>();

        // Map keys -> friendly names using lastDefectsMap
        if (defectKeys != null && !defectKeys.isEmpty()) {
            for (String dKey : defectKeys) {
                String foundName = null;
                if (stageKey != null && lastDefectsMap.containsKey(stageKey)) {
                    List<Map<String, String>> defs = lastDefectsMap.get(stageKey);
                    if (defs != null) {
                        for (Map<String, String> m : defs) {
                            String value = m.get("value");
                            String name = m.get("name");
                            if (value != null && value.equalsIgnoreCase(dKey)) {
                                foundName = name;
                                break;
                            }
                        }
                    }
                }
                names.add(foundName != null ? foundName : dKey);
            }
        } else {
            // fallback: show configured defect names for stage
            if (stageKey != null && lastDefectsMap.containsKey(stageKey)) {
                List<Map<String, String>> defs = lastDefectsMap.get(stageKey);
                if (defs != null) {
                    for (Map<String, String> m : defs) {
                        String name = m.get("name");
                        if (name != null) names.add(name);
                    }
                }
            }
        }

        if (names.isEmpty()) {
            MaterialTextView tv = new MaterialTextView(this);
            tv.setText("No defect points recorded for this product.");
            tv.setPadding(0, dpToPx(4), 0, dpToPx(4));
            llFailedDefectsContainer.addView(tv);
            return;
        }

        for (String n : names) {
            MaterialTextView tv = new MaterialTextView(this);
            tv.setText("• " + n);
            tv.setTextSize(14);
            tv.setPadding(0, dpToPx(4), 0, dpToPx(4));
            llFailedDefectsContainer.addView(tv);
        }
    }

    private void applyLockingByStage(String stage) {
        boolean locked = isStageLocked(stage);
        if (scannedStage != null) scannedStage.setEnabled(!locked);
        if (scannedQcStatus != null) scannedQcStatus.setEnabled(!locked);
        if (etRemarks != null) etRemarks.setEnabled(!locked);
        if (locked) {
            if (scannedStage != null) scannedStage.setHint("Locked after packaging");
            if (scannedQcStatus != null) scannedQcStatus.setHint("Locked");
        } else {
            if (scannedStage != null) scannedStage.setHint("");
            if (scannedQcStatus != null) scannedQcStatus.setHint("");
        }
    }

    private boolean isStageLocked(String stage) {
        if (stage == null) return false;
        String s = stage.trim().toLowerCase();
        return s.equals("packaging") || s.equals("ready for shipment") || s.equals("shipped");
    }

    private void handleQcStageSelection() {
        if (selectedProduct == null) {
            if (defectsCard != null) defectsCard.setVisibility(View.GONE);
            if (llDefectsContainer != null) llDefectsContainer.removeAllViews();
            if (rejectBtn != null) rejectBtn.setEnabled(false);
            evaluateUpdateButtonState();
            return;
        }

        String selectedStageDisplay = scannedStage.getText() == null ? "" : scannedStage.getText().toString().trim();
        String selectedQcDisplay = scannedQcStatus.getText() == null ? "" : scannedQcStatus.getText().toString().trim();

        boolean qcIsFail = false;
        if ("FAIL".equalsIgnoreCase(selectedQcDisplay)) qcIsFail = true;
        String qcKey = getKeyForValue(lastStatusMap, selectedQcDisplay);
        if (!qcIsFail && qcKey != null && "FAIL".equalsIgnoreCase(qcKey)) qcIsFail = true;

        String stageKey = getKeyForValue(lastStagesMap, selectedStageDisplay);

        boolean locked = isStageLocked(selectedProduct.getLatestStage());

        if (qcIsFail && stageKey != null) {
            populateDefectsForStage(stageKey);
            if (defectsCard != null) defectsCard.setVisibility(View.VISIBLE);
            if (rejectBtn != null) rejectBtn.setEnabled(true);
            if (updateStageBtn != null) updateStageBtn.setEnabled(false);
        } else {
            if (defectsCard != null) defectsCard.setVisibility(View.GONE);
            if (llDefectsContainer != null) llDefectsContainer.removeAllViews();
            if (rejectBtn != null) rejectBtn.setEnabled(false);
            evaluateUpdateButtonState();
        }
    }

    private void onRejectClicked() {
        if (selectedProduct == null) {
            Toast.makeText(this, "No product loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedStageDisplay = scannedStage.getText() == null ? "" : scannedStage.getText().toString().trim();

        String stageToSend = getKeyForValue(lastStagesMap, selectedStageDisplay);
        if (stageToSend == null) {
            stageToSend = selectedStageDisplay.length() > 0 ? selectedStageDisplay : null;
        }

        String qcToSend = "FAIL";
        if (scannedQcStatus != null) scannedQcStatus.setText("FAIL", false);
        List<String> selectedDefects = new ArrayList<>();
        if (llDefectsContainer != null) {
            int childCount = llDefectsContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View v = llDefectsContainer.getChildAt(i);
                if (v instanceof CheckBox) {
                    CheckBox cb = (CheckBox) v;
                    if (cb.isChecked()) {
                        Object tag = cb.getTag();
                        if (tag != null) selectedDefects.add(tag.toString());
                    }
                }
            }
        }

        String remarks = etRemarks == null ? "" : (etRemarks.getText() == null ? "" : etRemarks.getText().toString().trim());
        //Please select atleast one defect points
        if (selectedDefects == null || selectedDefects.isEmpty()) {
            SnackbarHelper.showTopCenter(ScannerActivity.this, "❌ Please select atleast one defect points", false);
            return;
        }
        updateProductStageWithPayload(lastScannedTagId, stageToSend, qcToSend, selectedDefects, remarks,false);
    }

    private void populateDefectsForStage(String stageKey) {
        if (llDefectsContainer != null) llDefectsContainer.removeAllViews();

        List<Map<String, String>> defects = lastDefectsMap.get(stageKey);
        if (defects == null) defects = new ArrayList<>();

        List<String> existingDefects = null;
        if (selectedProduct != null && selectedProduct.getLatestDefectsPoints() != null) {
            existingDefects = selectedProduct.getLatestDefectsPoints();
        }

        if (defects.isEmpty()) {
            MaterialTextView tv = new MaterialTextView(this);
            tv.setText("No defect points configured for this stage.");
            if (llDefectsContainer != null) llDefectsContainer.addView(tv);
            return;
        }

        for (Map<String, String> d : defects) {
            String name = d.get("name");
            String value = d.get("value");
            CheckBox cb = new CheckBox(this);
            cb.setText(name);
            cb.setTag(value);

            if (existingDefects != null && value != null) {
                for (String ex : existingDefects) {
                    if (ex != null && ex.equalsIgnoreCase(value)) {
                        cb.setChecked(true);
                        break;
                    }
                }
            }
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> { /* no-op here */ });

            if (llDefectsContainer != null) llDefectsContainer.addView(cb);
        }
    }

    private void onUpdateClicked() {
        if (selectedProduct == null) {
            Toast.makeText(this, "No product loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedStageDisplay = scannedStage.getText() == null ? "" : scannedStage.getText().toString().trim();
        String selectedQcDisplay = scannedQcStatus.getText() == null ? "" : scannedQcStatus.getText().toString().trim();

        String stageToSend = getKeyForValue(lastStagesMap, selectedStageDisplay);
        if (stageToSend == null) {
            stageToSend = selectedStageDisplay.length() > 0 ? selectedStageDisplay : null;
        }

        String qcToSend = getKeyForValue(lastStatusMap, selectedQcDisplay);
        if (qcToSend == null) {
            qcToSend = selectedQcDisplay.length() > 0 ? selectedQcDisplay : null;
        }

        List<String> selectedDefects = new ArrayList<>();
        if (llDefectsContainer != null) {
            int childCount = llDefectsContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View v = llDefectsContainer.getChildAt(i);
                if (v instanceof CheckBox) {
                    CheckBox cb = (CheckBox) v;
                    if (cb.isChecked()) {
                        Object tag = cb.getTag();
                        if (tag != null) selectedDefects.add(tag.toString());
                    }
                }
            }
        }

        String remarks = etRemarks == null ? "" : (etRemarks.getText() == null ? "" : etRemarks.getText().toString().trim());

        updateProductStageWithPayload(lastScannedTagId, stageToSend, qcToSend, selectedDefects, remarks,false);
    }

    private void updateProductStageWithPayload(String tagId, String stageKey, String qcKey, List<String> defectsPoints, String remarks, boolean isReworkUpdate) {
        String qcText = qcKey == null ? "PENDING" : qcKey;
        // PENDING status cannot update
        if (qcText.equalsIgnoreCase("PENDING")) {
            SnackbarHelper.showTopCenter(ScannerActivity.this, "❌ Already in pending status", false);
            return;
        }

        if (!isReworkUpdate && qcText.equalsIgnoreCase("REWORK")) {
            SnackbarHelper.showTopCenter(ScannerActivity.this, "❌ REWORK cannot be updated", false);
            return;
        }

        if (scanStatusText != null) {
            scanStatusText.setText("Updating stage...");
            scanStatusText.setVisibility(View.VISIBLE);
        }

        UpdateStageRequest request = new UpdateStageRequest(tagId, stageKey, qcKey, defectsPoints, remarks);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.updateProductStage(request).enqueue(new Callback<TagResponse>() {
            @Override
            public void onResponse(Call<TagResponse> call, Response<TagResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (scanStatusText != null) scanStatusText.setVisibility(View.GONE);

                try {
                    // Case A: HTTP 2xx and body present
                    if (response.isSuccessful() && response.body() != null) {
                        TagResponse body = response.body();
                        if (body.isSuccess()) {
                            // ✅ Success path
                            selectedProduct = body.getProduct();

                            // If this update was a REWORK from the failed card, clear UI immediately
                            if (qcKey != null && qcKey.equalsIgnoreCase("REWORK")) {
                                SnackbarHelper.showTopCenter(ScannerActivity.this, "✔ Status updated to REWORK", true);
                                // ensure any rework button re-enable is handled (clearUI hides icons anyway)
                                mainHandler.post(() -> clearUI());
                                return;
                            }

                            // default behavior for other statuses: show updated product
                            displayProductDetails(tagId, selectedProduct);
                            SnackbarHelper.showTopCenter(ScannerActivity.this, "✔ Stage updated successfully", true);

                            // Print label now that update succeeded (only for packaging)
                            List<String> printStages = Arrays.asList(
                                    "packaging",
                                    "tape_edge_qc",
                                    "zip_cover_qc"
                            );

                            if (stageKey != null && printStages.contains(stageKey.toLowerCase())) {
                                try {
                                    if (printerManager != null && printerManager.isConnected()) {
                                        showConfirmPrintDialog(selectedProduct);
                                    } else {
                                        Toast.makeText(ScannerActivity.this,
                                                "Printer not connected — attempting to connect...",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        Log.d("PRINT_ERROR", "Printer not connected; attempting to connect now.");

                                        if (printerManager != null) {
                                            bgExecutor.execute(() -> {
                                                printerManager.connectBluetooth();
                                                if (printerManager.isConnected()) {
                                                    runOnUiThread(() -> showConfirmPrintDialog(selectedProduct));
                                                } else {
                                                    Log.d("PRINT_ERROR", "Reconnect attempt failed");
                                                }
                                            });
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e("PRINT_ERROR", "Failed to print label: " + e.getMessage(), e);
                                    Toast.makeText(ScannerActivity.this,
                                            "Failed to print label: " + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }

                            if (updateStageBtn != null) updateStageBtn.setEnabled(false);
                            mainHandler.postDelayed(() -> clearUI(), 1000);
                            return;
                        } else {
                            // success=false returned inside 2xx response (common in your backend)
                            String serverMessage = body.getMessage() != null ? body.getMessage() : "❌ Stage update rejected by server";
                            SnackbarHelper.showTopCenter(ScannerActivity.this, "⚠ " + serverMessage, false);
                            Log.e("API_ERROR", "updateProductStage rejected by server: " + serverMessage);

                            // re-enable rework button if present (so user can retry)
                            if (ivFailedCardRework != null) ivFailedCardRework.setEnabled(true);
                            return;
                        }
                    }

                    // Case B: Non-2xx or response without body -> try to parse errorBody
                    String parsedMessage = "❌ Invalid stage";
                    if (response.errorBody() != null) {
                        try {
                            String errorJson = response.errorBody().string();
                            if (!errorJson.isEmpty()) {
                                // Try parse typical JSON: { success:false, message:"...", errors: {...} }
                                try {
                                    JSONObject json = new JSONObject(errorJson);
                                    // prefer "message"
                                    if (json.has("message") && !json.isNull("message")) {
                                        parsedMessage = "⚠ " + json.getString("message");
                                    } else if (json.has("errors") && !json.isNull("errors")) {
                                        // collect validation errors
                                        JSONObject errors = json.getJSONObject("errors");
                                        StringBuilder sb = new StringBuilder();
                                        Iterator<String> keys = errors.keys();
                                        while (keys.hasNext()) {
                                            String key = keys.next();
                                            JSONArray arr = errors.optJSONArray(key);
                                            if (arr != null) {
                                                for (int i = 0; i < arr.length(); i++) {
                                                    if (sb.length() > 0) sb.append(" • ");
                                                    sb.append(arr.optString(i));
                                                }
                                            } else {
                                                String val = errors.optString(key, "");
                                                if (!val.isEmpty()) {
                                                    if (sb.length() > 0) sb.append(" • ");
                                                    sb.append(val);
                                                }
                                            }
                                        }
                                        String joined = sb.toString();
                                        parsedMessage = joined.isEmpty() ? "⚠ Validation error" : "⚠ " + joined;
                                    } else {
                                        // fallback: whole body text
                                        parsedMessage = "⚠ " + errorJson;
                                    }
                                } catch (JSONException je) {
                                    // not JSON — show raw text (trim to reasonable length)
                                    String raw = errorJson.trim();
                                    if (raw.length() > 300) raw = raw.substring(0, 300) + "...";
                                    parsedMessage = "⚠ " + raw;
                                }
                            }
                        } catch (IOException ioe) {
                            Log.e("API_ERROR", "Failed to read errorBody", ioe);
                        }
                    } else if (response.body() != null && !response.body().isSuccess()) {
                        // response.isSuccessful() might be true but body null handled above; this is extra guard
                        parsedMessage = response.body().getMessage() != null ? "⚠ " + response.body().getMessage() : parsedMessage;
                    }

                    SnackbarHelper.showTopCenter(ScannerActivity.this, parsedMessage, false);
                    Log.e("API_ERROR", "updateProductStage failed: HTTP " + response.code() + " — " + parsedMessage);

                    // re-enable rework button if present
                    if (ivFailedCardRework != null) ivFailedCardRework.setEnabled(true);
                } catch (Exception e) {
                    // catch-all to avoid crashing UI
                    Log.e("API_ERROR", "Unhandled exception in onResponse", e);
                    SnackbarHelper.showTopCenter(ScannerActivity.this, "⚠ Unexpected response from server", false);

                    if (ivFailedCardRework != null) ivFailedCardRework.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<TagResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                if (scanStatusText != null) scanStatusText.setVisibility(View.GONE);
                SnackbarHelper.showTopCenter(ScannerActivity.this, "⚠ Network error: " + t.getMessage(), false);
                Log.e("API_ERROR", "Failed to update stage", t);

                // re-enable rework button if present
                if (ivFailedCardRework != null) ivFailedCardRework.setEnabled(true);
            }
        });
    }

    private void showNoProductFound(String tagId) {
        // Remember the tag we scanned (useful if user wants to create product later)
        lastScannedTagId = tagId;

        // Clear model reference
        selectedProduct = null;

        // Primary product fields
        if (qaCode != null) qaCode.setText("-");                    // clear QA code
        if (scannedProduct != null) scannedProduct.setText("No product found");
        if (scannedProductSKU != null) scannedProductSKU.setText("-"); // clear SKU
        if (scannedSize != null) scannedSize.setText("Unknown");
        if (scannedStatus != null) scannedStatus.setText("N/A");

        // Dropdowns
        if (scannedStage != null) scannedStage.setText("", false);
        if (scannedQcStatus != null) scannedQcStatus.setText("", false);

        // Show editable card (so operator can add product or try again)
        if (scanResultCard != null) scanResultCard.setVisibility(View.VISIBLE);

        // Hide failed card (we are in "not found" path)
        if (failedResultCard != null) failedResultCard.setVisibility(View.GONE);

        // Hide print icons (no product to print)
//        if (ivCardPrint != null) ivCardPrint.setVisibility(View.GONE);
        if (ivCardPrint != null) ivCardPrint.setVisibility(View.VISIBLE);
//        if (ivFailedCardPrint != null) ivFailedCardPrint.setVisibility(View.GONE);

        // Show clear button so user can manually clear if needed
        if (ivCardClear != null) ivCardClear.setVisibility(View.VISIBLE);

        // Hide rework button (not applicable)
        if (ivFailedCardRework != null) ivFailedCardRework.setVisibility(View.GONE);

        // Reset editors and defects area
        if (productEditLayout != null) productEditLayout.setVisibility(View.GONE);
        if (productEditButtons != null) productEditButtons.setVisibility(View.GONE);
        if (productSkuEditLayout != null) productSkuEditLayout.setVisibility(View.GONE);
        if (productSkuEditButtons != null) productSkuEditButtons.setVisibility(View.GONE);
        if (etRemarks != null) etRemarks.setText("");
        if (llDefectsContainer != null) llDefectsContainer.removeAllViews();
        if (defectsCard != null) defectsCard.setVisibility(View.GONE);

        // Buttons state
        if (updateStageBtn != null) updateStageBtn.setEnabled(false);
        if (rejectBtn != null) rejectBtn.setEnabled(false);
    }


    private void clearUI() {
        if (scannedProduct != null) scannedProduct.setText("-");
        if (scannedStage != null) scannedStage.setText("", false);
        if (scannedQcStatus != null) scannedQcStatus.setText("", false);
        if (scannedSize != null) scannedSize.setText("-");
        if (scannedStatus != null) scannedStatus.setText("-");
        if (scanResultCard != null) scanResultCard.setVisibility(View.GONE);
        if (failedResultCard != null) failedResultCard.setVisibility(View.GONE);

        if (updateStageBtn != null) updateStageBtn.setEnabled(false);
        if (rejectBtn != null) rejectBtn.setEnabled(false);
        if (etRemarks != null) etRemarks.setText("");
        if (defectsCard != null) defectsCard.setVisibility(View.GONE);
        if (llDefectsContainer != null) llDefectsContainer.removeAllViews();

        if (ivCardClear != null) ivCardClear.setVisibility(View.GONE);
        if (ivCardPrint != null) ivCardPrint.setVisibility(View.GONE);

        selectedProduct = null;
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
    protected void onResume() {
        super.onResume();
        // Attempt to open UHF device
        if (mDevice != null && !mDevice.open()) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_LONG).show();
        }

        // If a saved printer exists and we're not connected, try to connect when activity resumes
        if (printerManager != null && !printerManager.isConnected()) {
            autoConnectSavedPrinterIfNeeded();
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
        // optionally disconnect from printer
        if (printerManager != null && printerManager.isConnected()) {
            printerManager.disconnect();
        }
        bgExecutor.shutdownNow();
    }

    /**
     * Fetch allowed stages & statuses from server (and defect points).
     */
    /**
     * Fetch allowed stages & statuses from server (and defect points).
     */
    private void fetchStagesAndStatuses(String latestStage, String latestStatus, String latestRemarks) {
        if (isFinishing() || isDestroyed()) return;

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        StagesStatusRequest request =
                new StagesStatusRequest(latestStage, latestStatus, latestRemarks);

        apiService.getStagesAndStatus(request).enqueue(new Callback<StagesStatusResponse>() {
            @Override
            public void onResponse(Call<StagesStatusResponse> call, Response<StagesStatusResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                List<String> stagesList = new ArrayList<>();
                List<String> qcList = new ArrayList<>();
                lastStagesMap = null;
                lastStatusMap = null;
                lastDefectsMap.clear();

                List<String> allowedStageKeys = new ArrayList<>();

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    StagesStatusResponse.Data data = response.body().getData();
                    if (data != null) {
                        if (data.getStages() != null) {
                            lastStagesMap = data.getStages();
                            stagesList.addAll(data.getStages().values());
                        }
                        if (data.getStatus() != null) {
                            lastStatusMap = data.getStatus();
                            qcList.addAll(data.getStatus().values());
                        }
                        if (data.getDefectPoints() != null) {
                            lastDefectsMap.putAll(data.getDefectPoints());
                        }
                        if (data.getAllowedStages() != null) {
                            allowedStageKeys = data.getAllowedStages();
                        } else {
                            allowedStageKeys = new ArrayList<>();
                        }

// store into activity field so current UI logic uses freshest list
                        lastAllowedStageKeys = new ArrayList<>(allowedStageKeys);

// also persist (optional) so other flows or future sessions have cache
                        PrefHelper.saveAllowedStages(ScannerActivity.this, allowedStageKeys);
                    }
                }

                // ⭐⭐⭐ SAVE allowed stages locally
                PrefHelper.saveAllowedStages(ScannerActivity.this, allowedStageKeys);

                // ---------------------------
                // Ensure CURRENT stage is added to dropdown ALWAYS
                // ---------------------------
                String displayStage = null;
                if (latestStage != null && lastStagesMap != null) {
                    displayStage = getValueForKey(lastStagesMap, latestStage);
                }
                if (displayStage == null && latestStage != null) {
                    displayStage = latestStage;
                }

                if (displayStage != null && !stagesList.contains(displayStage)) {
                    stagesList.add(0, displayStage);
                }

                // ---------------------------
                // Set adapters
                // ---------------------------
                ArrayAdapter<String> stageAdapter = new ArrayAdapter<>(
                        ScannerActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        stagesList
                );
                scannedStage.setAdapter(stageAdapter);
                scannedStage.setThreshold(0);

                ArrayAdapter<String> qcAdapter = new ArrayAdapter<>(
                        ScannerActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        qcList
                );
                scannedQcStatus.setAdapter(qcAdapter);
                scannedQcStatus.setThreshold(0);

                scannedStage.setText("", false);
                scannedQcStatus.setText("", false);

                // ---------------------------
                // PRE-SELECT stage + QC
                // ---------------------------
                if (displayStage != null) {
                    String fs = displayStage;
                    scannedStage.post(() -> scannedStage.setText(fs, false));
                }

                if (latestStatus != null && lastStatusMap != null) {
                    String displayStatus = getValueForKey(lastStatusMap, latestStatus);
                    if (displayStatus != null) {
                        scannedQcStatus.post(() -> scannedQcStatus.setText(displayStatus, false));
                    }
                }

                // ---------------------------
                // Lock/Unlock stage dropdown depending on allowed_stages
                // ---------------------------
                boolean isUserAllowed = false;

                if (latestStage != null && allowedStageKeys != null && !allowedStageKeys.isEmpty()) {
                    for (String k : allowedStageKeys) {
                        if (k != null && k.equalsIgnoreCase(latestStage)) {
                            isUserAllowed = true;
                            break;
                        }
                    }
                } else {
                    // No restrictions => editable
                    isUserAllowed = true;
                }

                scannedStage.setEnabled(isUserAllowed);

                if (!isUserAllowed) {
                    scannedStage.setHint("Stage (read-only)");
                } else {
                    scannedStage.setHint("");
                }

                scannedQcStatus.setEnabled(true);

                // ---------------------------
                // Update buttons & defect logic
                // ---------------------------
                handleQcStageSelection();
                applyLockingByStage(latestStage);
                evaluateUpdateButtonState(); // ⭐ MUST RUN LAST ⭐
            }

            @Override
            public void onFailure(Call<StagesStatusResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ScannerActivity.this, "Error fetching stages/status: " + t.getMessage(), Toast.LENGTH_LONG).show();
                updateStageBtn.setEnabled(false);
            }
        });
    }



    private String getValueForKey(Map<String, String> map, String key) {
        if (map == null || key == null) return null;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    private String getKeyForValue(Map<String, String> map, String displayValue) {
        if (map == null || displayValue == null) return null;
        String trimmed = displayValue.trim();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getValue() != null && e.getValue().equalsIgnoreCase(trimmed)) {
                return e.getKey();
            }
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(trimmed)) {
                return e.getKey();
            }
        }
        return null;
    }


    /**
     * Hide product/SKU inline edit icons when the product is in a final stage
     * (packaging in your request). Accepts either a canonical stage key or a display value.
     */
    private void updateProductEditIconsVisibility(@Nullable String stageKeyOrDisplay) {
        boolean hideEditors = false;

        if (stageKeyOrDisplay != null) {
            String s = stageKeyOrDisplay.trim();

            // If we already have a canonical key (like "packaging"), check it directly
            if ("packaging".equalsIgnoreCase(s)) {
                hideEditors = true;
            } else {
                // Otherwise try to convert display -> key using lastStagesMap
                String possibleKey = getKeyForValue(lastStagesMap, s);
                if (possibleKey != null && "packaging".equalsIgnoreCase(possibleKey)) {
                    hideEditors = true;
                }
            }
        }

        final int vis = hideEditors ? View.GONE : View.VISIBLE;

        if (ivEditProduct != null) ivEditProduct.setVisibility(vis);
        if (ivEditProductSKU != null) ivEditProductSKU.setVisibility(vis);

        // If hiding editors, also make sure any open edit layouts/buttons are closed
        if (hideEditors) {
            if (productEditLayout != null) productEditLayout.setVisibility(View.GONE);
            if (productEditButtons != null) productEditButtons.setVisibility(View.GONE);
            if (productSkuEditLayout != null) productSkuEditLayout.setVisibility(View.GONE);
            if (productSkuEditButtons != null) productSkuEditButtons.setVisibility(View.GONE);
            // also clear edit text so nothing remains
            if (etProductNameEdit != null) etProductNameEdit.setText("");
            if (etProductSkuEdit != null) etProductSkuEdit.setText("");
            hideKeyboard(etProductNameEdit);
            hideKeyboard(etProductSkuEdit);
        }
    }


    /**
     * Evaluate whether the Update button should be enabled.
     * Final logic:
     * - If stage is locked → update disabled.
     * - If user can change stage → must have (stage + QC) and QC != PENDING.
     * - If user cannot change stage → QC must be PASS only.
     * - If QC = FAIL → reject button path only.
     */
    private void evaluateUpdateButtonState() {

        if (updateStageBtn == null) return;
        String stageDisplay = scannedStage.getText() == null ? "" :
                scannedStage.getText().toString().trim();
        String qcDisplay = scannedQcStatus.getText() == null ? "" :
                scannedQcStatus.getText().toString().trim();

        // if Working stage is not equal to selected stage then both the button will be disabled
//        if (!stageDisplay.equalsIgnoreCase(selectedProduct.getLatestStage())) {
//            updateStageBtn.setEnabled(false);
//            rejectBtn.setEnabled(false);
//            return;
//        }

        // No product loaded → disable
        if (selectedProduct == null) {
            updateStageBtn.setEnabled(false);
            return;
        }

        // If packaging/ready-for-shipment/shipped → LOCKED
        if (isStageLocked(selectedProduct.getLatestStage())) {
            updateStageBtn.setEnabled(false);
            return;
        }



        boolean hasStage = stageDisplay.length() > 0;
        boolean hasQC = qcDisplay.length() > 0;

        // Determine QC key
        String qcKey = getKeyForValue(lastStatusMap, qcDisplay);
        if (qcKey == null) qcKey = qcDisplay;
        if (qcKey == null) qcKey = "";

        // ---------------------------
        // CASE 1: QC = FAIL → Update OFF
        // ---------------------------
        if ("FAIL".equalsIgnoreCase(qcKey)) {
            updateStageBtn.setEnabled(false);
            return;
        }

        // ---------------------------
        // ALWAYS check ALLOWED STAGES (use freshest data from last fetch)
        // ---------------------------
        try {
            List<String> allowedStageKeys = lastAllowedStageKeys; // use activity field (fresh)

            if (allowedStageKeys != null && !allowedStageKeys.isEmpty()) {

                // If user didn't touch the stage dropdown (empty display), fall back to product's latest stage
                String selectedStageDisplay = stageDisplay;
                if (selectedStageDisplay == null || selectedStageDisplay.isEmpty()) {
                    selectedStageDisplay = selectedProduct != null ? selectedProduct.getLatestStage() : "";
                }

                // Convert display → key using map; if not found use raw text as fallback
                String selectedStageKey = getKeyForValue(lastStagesMap, selectedStageDisplay);
                if (selectedStageKey == null || selectedStageKey.trim().isEmpty()) {
                    selectedStageKey = selectedStageDisplay;
                }

                boolean isAllowed = false;
                for (String s : allowedStageKeys) {
                    if (s != null && selectedStageKey != null &&
                            s.trim().equalsIgnoreCase(selectedStageKey.trim())) {
                        isAllowed = true;
                        break;
                    }
                }

                // ❌ NOT ALLOWED → UPDATE DISABLED
                if (!isAllowed) {
                    updateStageBtn.setEnabled(false);
                    return;
                }
            }
        } catch (Exception ignored) {}


        // ---------------------------
        // CASE 2: User CAN change stage
        // ---------------------------
        if (scannedStage.isEnabled()) {

            if (hasStage && hasQC && !"PENDING".equalsIgnoreCase(qcKey)) {
                updateStageBtn.setEnabled(true);
            } else {
                updateStageBtn.setEnabled(false);
            }
            return;
        }

        // ---------------------------
        // CASE 3: User CANNOT change stage
        // QC must be PASS
        // ---------------------------
        if (!scannedStage.isEnabled()) {
            if ("PASS".equalsIgnoreCase(qcKey)) {
                updateStageBtn.setEnabled(true);
            } else {
                updateStageBtn.setEnabled(false);
            }
            return;
        }

        updateStageBtn.setEnabled(false);
    }

    private void showConfirmPrintDialog(Product product) {

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Print")
                .setMessage("Do you want to take print?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    try {

                        // If product is not available → send test print values
                        String qaCode         = product != null ? product.getQAcode()       : "TEST";
                        String productName    = product != null ? product.getProductName()   : "TEST PRINT";
                        String size           = product != null ? product.getSize()          : "-";
                        String stage          = product != null ? product.getLatestStage()   : "-";
                        String status         = product != null ? product.getLatestStatus()  : "-";
                        String sku            = product != null ? product.getSku()           : "-";
                        String referenceCode  = product != null ? product.getReferenceCode() : "-";

                        // Now always print (normal or test)
                        printerManager.printProductLabel(
                                qaCode,
                                productName,
                                size,
                                stage,
                                status,
                                sku,
                                referenceCode
                        );

                        Log.d("PRINT_SUCCESS", "Label printed successfully");

                    } catch (Exception e) {
                        Log.e("PRINT_ERROR", "Failed to print: " + e.getMessage(), e);
                        Toast.makeText(this, "Print failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void populateDefectsReadOnly(String stageKey, List<String> defectKeys) {
        if (llDefectsContainer == null) return;
        llDefectsContainer.removeAllViews();

        List<String> names = new ArrayList<>();

        // If the product returned explicit defect keys (latestDefectsPoints), map them to friendly names
        if (defectKeys != null && !defectKeys.isEmpty()) {
            for (String dKey : defectKeys) {
                String foundName = null;
                if (stageKey != null && lastDefectsMap.containsKey(stageKey)) {
                    List<Map<String, String>> defs = lastDefectsMap.get(stageKey);
                    if (defs != null) {
                        for (Map<String, String> m : defs) {
                            String value = m.get("value");
                            String name = m.get("name");
                            if (value != null && value.equalsIgnoreCase(dKey)) {
                                foundName = name;
                                break;
                            }
                        }
                    }
                }
                // fallback to showing key if name not found
                names.add(foundName != null ? foundName : dKey);
            }
        } else {
            // If no explicit defect keys on product, try to show configured defect points for the stage
            if (stageKey != null && lastDefectsMap.containsKey(stageKey)) {
                List<Map<String, String>> defs = lastDefectsMap.get(stageKey);
                if (defs != null) {
                    for (Map<String, String> m : defs) {
                        String name = m.get("name");
                        if (name != null) names.add(name);
                    }
                }
            }
        }

        if (names.isEmpty()) {
            MaterialTextView tv = new MaterialTextView(this);
            tv.setText("No defect points recorded for this product.");
            tv.setPadding(0, dpToPx(4), 0, dpToPx(4));
            llDefectsContainer.addView(tv);
            return;
        }

        // Add each defect as a simple bullet-like text view
        for (String n : names) {
            MaterialTextView tv = new MaterialTextView(this);
            tv.setText("• " + n);
            tv.setTextSize(14);
            tv.setPadding(0, dpToPx(4), 0, dpToPx(4));
            llDefectsContainer.addView(tv);
        }
    }

    // small utility to convert dp to px (used above)
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
//-------------------kumar----------------------
private void hideProductEditor() {
    if (productEditLayout != null) productEditLayout.setVisibility(View.GONE);
    if (productEditButtons != null) productEditButtons.setVisibility(View.GONE);
    if (etProductNameEdit != null) etProductNameEdit.setText("");
    hideKeyboard(etProductNameEdit);
    if (btnSaveProduct != null) btnSaveProduct.setEnabled(true);
}

    /* Keyboard helpers */
    private void showKeyboard(View v) {
        if (v == null) return;
        v.post(() -> {
            v.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void hideKeyboard(View v) {
        if (v == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }


    /**
     * Unified save: update product_name and/or sku using same API endpoint.
     *
     * @param tagId    RFID tag id (may be null if server accepts other identifiers)
     * @param productName  new product name to set (nullable)
     * @param sku          new sku to set (nullable)
     * @param onSuccess    Runnable executed on success (UI thread)
     * @param onError      Consumer-like callback receiving error message (UI thread)
     */
    private void saveProductFields(@Nullable String tagId,
                                   @Nullable String productName,
                                   @Nullable String sku,
                                   @NonNull Runnable onSuccess,
                                   @NonNull java.util.function.Consumer<String> onError) {

        if (scanStatusText != null) {
            scanStatusText.setText("Saving...");
            scanStatusText.setVisibility(View.VISIBLE);
        }

        // Build request: both fields optional, backend should update only provided fields
        UpdateProductDetailsRequest req = new UpdateProductDetailsRequest(tagId, productName, sku);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.updateProductDetails(req).enqueue(new Callback<TagResponse>() {
            @Override
            public void onResponse(Call<TagResponse> call, Response<TagResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (scanStatusText != null) scanStatusText.setVisibility(View.GONE);

                try {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        TagResponse body = response.body();
                        // update local product model if server returned updated product
                        if (body.getProduct() != null) {
                            selectedProduct = body.getProduct();
                        } else {
                            // server didn't return full product — update locally where possible
                            if (selectedProduct != null) {
                                if (productName != null && !productName.isEmpty()) selectedProduct.setProductName(productName);
                                if (sku != null && !sku.isEmpty()) selectedProduct.setSku(sku);
                            }
                        }

                        // update UI
                        if (productName != null && scannedProduct != null) scannedProduct.setText(productName);
                        if (sku != null && scannedProductSKU != null) scannedProductSKU.setText(sku);

                        // hide editors and restore buttons
                        if (productEditLayout != null) productEditLayout.setVisibility(View.GONE);
                        if (productEditButtons != null) productEditButtons.setVisibility(View.GONE);
                        if (productSkuEditLayout != null) productSkuEditLayout.setVisibility(View.GONE);
                        if (productSkuEditButtons != null) productSkuEditButtons.setVisibility(View.GONE);

                        hideKeyboard(etProductNameEdit);
                        hideKeyboard(etProductSkuEdit);

                        // enable save buttons if they were disabled
                        if (btnSaveProduct != null) btnSaveProduct.setEnabled(true);
                        if (btnSaveSku != null) btnSaveSku.setEnabled(true);

                        runOnUiThread(() -> {
                            SnackbarHelper.showTopCenter(ScannerActivity.this, "✔ Product updated", true);
                            onSuccess.run();
                        });
                    } else {
                        // parse error message if possible
                        String errMsg = "Failed to update product";
                        if (response.errorBody() != null) {
                            try {
                                String raw = response.errorBody().string();
                                if (!raw.isEmpty()) {
                                    errMsg = raw.length() > 250 ? raw.substring(0, 250) + "..." : raw;
                                }
                            } catch (IOException ignored) {}
                        } else if (response.body() != null) {
                            errMsg = response.body().getMessage() != null ? response.body().getMessage() : errMsg;
                        }

                        // re-enable save buttons
                        if (btnSaveProduct != null) btnSaveProduct.setEnabled(true);
                        if (btnSaveSku != null) btnSaveSku.setEnabled(true);

                        final String finalErr = errMsg;
                        runOnUiThread(() -> onError.accept(finalErr));
                    }
                } catch (Exception e) {
                    Log.e("API_ERROR", "Unhandled exception in saveProductFields:onResponse", e);
                    if (btnSaveProduct != null) btnSaveProduct.setEnabled(true);
                    if (btnSaveSku != null) btnSaveSku.setEnabled(true);
                    runOnUiThread(() -> onError.accept("Unexpected server response"));
                }
            }

            @Override
            public void onFailure(Call<TagResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                if (scanStatusText != null) scanStatusText.setVisibility(View.GONE);
                Log.e("API_ERROR", "saveProductFields failed", t);
                if (btnSaveProduct != null) btnSaveProduct.setEnabled(true);
                if (btnSaveSku != null) btnSaveSku.setEnabled(true);
                runOnUiThread(() -> onError.accept("Network error: " + t.getMessage()));
            }
        });
    }

    private void onReworkClicked() {
        if (selectedProduct == null || lastScannedTagId == null) {
            Toast.makeText(this, "No failed product loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine stage key to send (prefer canonical key from lastStagesMap)
        String displayStage = failedStage == null ? null : (failedStage.getText() == null ? null : failedStage.getText().toString().trim());
        String stageToSend = getKeyForValue(lastStagesMap, displayStage);
        if (stageToSend == null) {
            // fallback to product latest stage key/value
            stageToSend = selectedProduct.getLatestStage();
        }

        if (stageToSend == null || stageToSend.trim().isEmpty()) {
            SnackbarHelper.showTopCenter(this, "❌ Unable to determine stage for this product", false);
            return;
        }

        // Make a final copy for use inside the lambda (required by Java)
        final String finalStageToSend = stageToSend;

        // Prefill remark with existing failed remark (if any)
        String existingRemark = (failedRemarks == null || failedRemarks.getText() == null) ? "" : failedRemarks.getText().toString().trim();
        if (existingRemark.equals("-")) existingRemark = "";

        // Build a small input dialog to capture operator remark (optional)
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(false);
        input.setMaxLines(4);
        input.setLines(3);
        input.setHint("Enter remark (optional)");
        input.setText(existingRemark);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Update for Rework")
//                .setMessage("Add a remark (optional) and confirm marking this product as REWORK.")
                .setView(input)
                .setPositiveButton("Update", (dialog, which) -> {
                    String remark = input.getText() == null ? "" : input.getText().toString().trim();
                    if (remark.length() > 250) {
                        remark = remark.substring(0, 250);
                    }

                    // Disable rework icon to avoid duplicate taps
                    if (ivFailedCardRework != null) ivFailedCardRework.setEnabled(false);

                    if (scanStatusText != null) {
                        scanStatusText.setText("Updating status to REWORK...");
                        scanStatusText.setVisibility(View.VISIBLE);
                    }

                    // qc status to send
                    String qcToSend = "REWORK";

                    // No defects for rework (empty array)
                    List<String> emptyDefects = new ArrayList<>();

                    // Use finalStageToSend here (final local copy)
                    updateProductStageWithPayload(lastScannedTagId, finalStageToSend, qcToSend, emptyDefects, remark, true);

                    // safety re-enable in case callback doesn't (will be re-enabled in onFailure/onError)
                    mainHandler.postDelayed(() -> {
                        if (ivFailedCardRework != null && !ivFailedCardRework.isEnabled()) {
                            ivFailedCardRework.setEnabled(true);
                        }
                    }, 8000);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // no-op
                })
                .show();
    }

    /**
     * Ensure device power matches saved preference before scanning.
     * Returns true if device is available/open (not necessarily that setPower succeeded).
     */
    private boolean syncRfidPowerBeforeScan() {
        if (mDevice == null) {
            Log.e("RFID_POWER", "mDevice is null in syncRfidPowerBeforeScan");
            return false;
        }

        try {
            // Ensure device is opened
            if (!mDevice.open()) {
                Log.e("RFID_POWER", "Failed to open UHF device in syncRfidPowerBeforeScan");
                return false;
            }

            int savedPower = PrefHelper.getRfidPower(this);
            int devicePower = mDevice.getPower();

            Log.d("RFID_POWER", "Device power = " + devicePower + " | Saved power = " + savedPower);

            if (devicePower != savedPower) {
                Log.i("RFID_POWER", "Power mismatch detected. Attempting to set device to saved power: " + savedPower);

                boolean setOk = false;
                try {
                    // Preferred: call direct API (returns boolean on most seuic libs)
                    setOk = mDevice.setPower(savedPower);
                } catch (Throwable t) {
                    // Fallback to reflection for devices/SDKs where setPower is not public
                    try {
                        java.lang.reflect.Method m = mDevice.getClass().getMethod("setPower", int.class);
                        m.invoke(mDevice, savedPower);
                        setOk = true;
                    } catch (Exception ex) {
                        Log.w("RFID_POWER", "Reflection setPower failed: " + ex.getMessage());
                        setOk = false;
                    }
                }

                // Verify actual device power after attempt
                int newDevicePower = mDevice.getPower();
                Log.d("RFID_POWER", "After set attempt, device power = " + newDevicePower + " | setOk=" + setOk);

                if (setOk && newDevicePower == savedPower) {
                    // success — keep saved value consistent
                    PrefHelper.saveRfidPower(this, savedPower);
                    Log.i("RFID_POWER", "Power synced to saved value: " + savedPower);
                } else {
                    // failed or not matching — persist actual device power so app won't repeatedly try to set and fail
                    PrefHelper.saveRfidPower(this, newDevicePower);
                    Log.w("RFID_POWER", "Could not apply saved power. Persisting actual device power: " + newDevicePower);
                }
            } else {
                // already matching — ensure prefs consistent
                PrefHelper.saveRfidPower(this, devicePower);
                Log.d("RFID_POWER", "Device power already matches saved power: " + devicePower);
            }
            return true;
        } catch (Exception e) {
            Log.e("RFID_POWER", "Exception in syncRfidPowerBeforeScan: " + e.getMessage(), e);
            return false;
        }
    }



}
