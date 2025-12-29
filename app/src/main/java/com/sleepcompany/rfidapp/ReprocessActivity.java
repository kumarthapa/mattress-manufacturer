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
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;
import com.sleepcompany.rfidapp.adapter.ProductSelectAdapter;
import com.sleepcompany.rfidapp.databinding.ActivityReprocessBinding;
//import com.sleepcompany.rfidapp.model.Product;
import com.sleepcompany.rfidapp.model.StagesStatusRequest;
import com.sleepcompany.rfidapp.model.UpdateReprocessProductRequest;
import com.sleepcompany.rfidapp.network.ApiClient;
import com.sleepcompany.rfidapp.network.ApiService;
import com.sleepcompany.rfidapp.model.BondingProductModel;
//import com.sleepcompany.rfidapp.network.BondingProduct;
import com.sleepcompany.rfidapp.network.BondingProduct;
import com.sleepcompany.rfidapp.network.BondingProductsRequest;
import com.sleepcompany.rfidapp.network.BondingResponse;
import com.sleepcompany.rfidapp.network.ReprocessTagResponse;
import com.sleepcompany.rfidapp.network.StagesStatusResponse;
//import com.sleepcompany.rfidapp.network.TagResponse;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ReprocessActivity - simplified for packaging -> reprocess (status=Return).
 * Modified: inline edits are local (pending) and saved only when Update is pressed.
 *          model selection popup required before editing or updating if model missing.
 */
public class ReprocessActivity extends AppCompatActivity {

    private ActivityReprocessBinding binding;
    private UHFService mDevice;

    private MaterialButton startScanBtn, updateStageBtn;
    private CircularProgressIndicator scanProgress;
    private MaterialTextView scanStatusText;

    // QA Code fields
    private MaterialTextView qaCode;
    private ImageButton ivEditQaCode;
    private TextInputLayout qaEditLayout;
    private TextInputEditText etQaEdit;
    private LinearLayout qaEditButtons;
    private MaterialButton btnSaveQa, btnCancelQa;

    private MaterialTextView scannedProduct;
    private MaterialTextView scannedProductSKU;

    // SKU edit fields:
    private ImageButton ivEditProductSKU;
    private TextInputLayout productSkuEditLayout;
    private TextInputEditText etProductSkuEdit;
    private LinearLayout productSkuEditButtons;
    private MaterialButton btnSaveSku, btnCancelSkuEdit;

    private MaterialAutoCompleteTextView scannedStage, scannedQcStatus;
    private MaterialTextView scannedSize;
    private MaterialTextView scannedStatus;
    private MaterialTextView selectedModel;
    private MaterialCardView scanResultCard;

    // editable card extras
    private TextInputEditText etRemarks;
    private LinearLayout llDefectsContainer;
    private MaterialCardView defectsCard;
    private ImageButton ivCardClear; // small clear icon on editable card
    private ImageButton ivCardPrint; // small print icon on editable card

    private boolean isScanning = false;
    private Thread scanThread;

    // private Product selectedProduct;

    private BondingProductModel selectedProduct;

    private String lastScannedTagId;

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

    // Inline product edit fields
    private ImageButton ivEditProduct;
    private TextInputLayout productEditLayout;
    private TextInputEditText etProductNameEdit;
    private LinearLayout productEditButtons;
    private MaterialButton btnSaveProduct, btnCancelProductEdit;

    // Size inline editor fields (NEW)
    private ImageButton ivEditProductSize;
    private TextInputLayout productSizeEditLayout;
    private TextInputEditText etProductSizeEdit;
    private LinearLayout productSizeEditButtons;
    private MaterialButton btnSaveSize, btnCancelSizeEdit;

    // Executor for background printer connect (so we don't block UI)
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    // Keep the allowed-stage keys returned by backend for the currently loaded product
    private List<String> lastAllowedStageKeys = new ArrayList<>();

    // -------------------- Pending/local edits & model helper --------------------
    private String pendingProductName = null;
    private String pendingSku = null;
    private String pendingQa = null;
    private String pendingSize = null;
    private String pendingModel = null;
    // NEW: pending bonding product id (to send in update)
    private Long pendingBondingProductId = null;
    private boolean hasPendingEdits = false;
    private boolean isModelSelectionInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityReprocessBinding.inflate(getLayoutInflater());
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
                Toast.makeText(ReprocessActivity.this, "Printer connected: " + deviceInfo, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrinterDisconnected() {
                Toast.makeText(ReprocessActivity.this, "Printer disconnected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrintSuccess() {
                Toast.makeText(ReprocessActivity.this, "Print succeeded", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrintError(@NonNull String error) {
                Toast.makeText(ReprocessActivity.this, "Print error: " + error, Toast.LENGTH_LONG).show();
            }
        });

        openBuletooth();
        autoConnectSavedPrinterIfNeeded();
    }

    private void autoConnectSavedPrinterIfNeeded() {
        final String savedMac = PrefHelper.getPrinterMac(this);
        if (savedMac == null || savedMac.isEmpty()) {
            Log.d("PRINTER", "No saved printer mac to auto-connect.");
            return;
        }

        if (printerManager != null && printerManager.isConnected()) {
            Log.d("PRINTER", "Already connected to printer: " + printerManager.getDeviceInfo());
            return;
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Log.d("PRINTER", "No Bluetooth hardware available; cannot auto connect.");
            return;
        }

        if (!btAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                boolean enabled = btAdapter.enable();
                Log.d("PRINTER", "Requested Bluetooth enable: " + enabled);
                mainHandler.postDelayed(this::connectToSavedPrinterBackground, 800);
                return;
            } else {
                Toast.makeText(this, "Enable Bluetooth to connect saved printer", Toast.LENGTH_SHORT).show();
                return;
            }
        }

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
            try {
                printerManager.connectBluetooth();
            } catch (Exception e) {
                Log.e("PRINTER", "Auto connect exception: " + e.getMessage(), e);
                mainHandler.post(() -> Toast.makeText(ReprocessActivity.this, "Auto connect failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void openBuletooth() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d("Bluetooth", "btAdapter: " + btAdapter);
        if (btAdapter == null) {
            Toast.makeText(this, "No Found Bluetooth Hardware", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!btAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth Opening", Toast.LENGTH_SHORT).show();
            if (!mBtOpenSilent) {
                Intent mIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(mIntent, REQUEST_BT_ENABLE);
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                btAdapter.enable();
                Toast.makeText(this, "Bluetooth Open", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Bluetooth Open", Toast.LENGTH_SHORT).show();
        }
    }

    // NOTE: these receivers are declared but not registered in this file.
    // If you intend to use them, register/unregister in lifecycle methods.
    // Otherwise you can remove them.
    private BroadcastReceiver dynamicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("cp31.printerstatus")) {
                String msg = intent.getStringExtra("msg");
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @SuppressLint("WrongConstant")
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Toast.makeText(context, "Bluetooth Disconnect", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            getSupportActionBar().setTitle("Reprocess Mattress");
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

        // ===============================
        // BASIC CONTROLS
        // ===============================
        startScanBtn = binding.startScanBtn;
        updateStageBtn = binding.updateStageBtn;
        scanProgress = binding.scanProgress;
        scanStatusText = binding.scanStatusText;
        scanResultCard = binding.scanResultCard;

        updateStageBtn.setEnabled(false);

        // ===============================
        // QA CODE
        // ===============================
        qaCode = binding.qaCode;
        ivEditQaCode = binding.ivEditQaCode;
        qaEditLayout = binding.qaEditLayout;
        etQaEdit = binding.etQaEdit;
        qaEditButtons = binding.qaEditButtons;
        btnSaveQa = binding.btnSaveQa;
        btnCancelQa = binding.btnCancelQa;

        qaEditLayout.setVisibility(View.GONE);
        qaEditButtons.setVisibility(View.GONE);

        // Ensure Save QA button visible & wired
        if (btnSaveQa != null) {
            btnSaveQa.setVisibility(View.VISIBLE);
            btnSaveQa.setEnabled(true);
            btnSaveQa.setOnClickListener(v -> commitQaEditLocally());
        }

        // ===============================
        // PRODUCT
        // ===============================
        scannedProduct = binding.scannedProduct;
        ivEditProduct = binding.ivEditProduct;

        productEditLayout = binding.productEditLayout;
        etProductNameEdit = binding.etProductNameEdit;
        productEditButtons = binding.productEditButtons;
        btnSaveProduct = binding.btnSaveProduct;
        btnCancelProductEdit = binding.btnCancelProductEdit;

        productEditLayout.setVisibility(View.GONE);
        productEditButtons.setVisibility(View.GONE);

        // make Save Product button ready (visible/enabled) and wire commit
        if (btnSaveProduct != null) {
            btnSaveProduct.setVisibility(View.VISIBLE);
            btnSaveProduct.setEnabled(true);
            btnSaveProduct.setOnClickListener(v -> commitProductEditLocally());
        }

        // ===============================
        // SKU
        // ===============================
        scannedProductSKU = binding.scannedProductSKU;
        ivEditProductSKU = binding.ivEditProductSKU;

        productSkuEditLayout = binding.productSkuEditLayout;
        etProductSkuEdit = binding.etProductSkuEdit;
        productSkuEditButtons = binding.productSkuEditButtons;
        btnSaveSku = binding.btnSaveSku;
        btnCancelSkuEdit = binding.btnCancelSkuEdit;

        productSkuEditLayout.setVisibility(View.GONE);
        productSkuEditButtons.setVisibility(View.GONE);

        // make Save SKU button ready
        if (btnSaveSku != null) {
            btnSaveSku.setVisibility(View.VISIBLE);
            btnSaveSku.setEnabled(true);
            btnSaveSku.setOnClickListener(v -> commitSkuEditLocally());
        }

        // ===============================
        // SIZE (NEW)
        // ===============================
        scannedSize = binding.scannedProductSize; // display TextView
        ivEditProductSize = binding.ivEditProductSize;
        productSizeEditLayout = binding.productSizeEditLayout;
        etProductSizeEdit = binding.etProductSizeEdit;
        productSizeEditButtons = binding.productSizeEditButtons;
        btnSaveSize = binding.btnSaveSize;
        btnCancelSizeEdit = binding.btnCancelSizeEdit;

        productSizeEditLayout.setVisibility(View.GONE);
        productSizeEditButtons.setVisibility(View.GONE);

        // make Save Size button ready
        if (btnSaveSize != null) {
            btnSaveSize.setVisibility(View.VISIBLE);
            btnSaveSize.setEnabled(true);
            btnSaveSize.setOnClickListener(v -> commitSizeEditLocally());
        }

        // NOTE: previously we hid per-field save buttons here — removed so they are available
        // when the inline editor containers are shown.

        // ===============================
        // DROPDOWNS
        // ===============================
        scannedStage = binding.scannedStage;
        scannedQcStatus = binding.scannedQcStatus;

        scannedStage.setInputType(InputType.TYPE_NULL);
        scannedQcStatus.setInputType(InputType.TYPE_NULL);

        // ===============================
        // OTHER INFO
        // ===============================
        scannedStatus = binding.scannedStatus;
        selectedModel = binding.selectedModel;
        etRemarks = binding.etRemarks;

        // ===============================
        // DEFECTS & CARD ICONS
        // ===============================
        llDefectsContainer = binding.llDefectsContainer;
        defectsCard = binding.defectsCard;
        ivCardClear = binding.ivCardClear;
        ivCardPrint = binding.ivCardPrint;

        defectsCard.setVisibility(View.GONE);
        ivCardClear.setVisibility(View.GONE);
        ivCardPrint.setVisibility(View.GONE);

        // ===============================
        // DROPDOWN LISTENERS
        // ===============================
        scannedStage.setOnItemClickListener((parent, view, position, id) -> {
            evaluateUpdateButtonState();
            handleStageSelectionForDefects();
        });

        scannedQcStatus.setOnItemClickListener((parent, view, position, id) -> {
            evaluateUpdateButtonState();
            handleStageSelectionForDefects();
        });

        // ===============================
        // CLEAR CARD
        // ===============================
        ivCardClear.setOnClickListener(v -> clearUI());

        // ===============================
        // INLINE QA EDIT
        // ===============================
        ivEditQaCode.setOnClickListener(v -> {
            if (ensureModelSelectedBeforeEdit()) return;
            String currentQa = qaCode.getText() == null ? "" : qaCode.getText().toString();
            etQaEdit.setText(currentQa.equals("-") ? "" : currentQa);

            qaEditLayout.setVisibility(View.VISIBLE);
            qaEditButtons.setVisibility(View.VISIBLE);
            etQaEdit.requestFocus();
            showKeyboard(etQaEdit);
        });

        btnCancelQa.setOnClickListener(v -> {
            qaEditLayout.setVisibility(View.GONE);
            qaEditButtons.setVisibility(View.GONE);
            etQaEdit.setText("");
            hideKeyboard(etQaEdit);
        });

        // We replaced immediate network save for QA with local commit
        etQaEdit.setOnEditorActionListener((v, actionId, event) -> {
            commitQaEditLocally();
            return true;
        });
        // btnSaveQa wired above to commitQaEditLocally()
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

        // setup editors
        setupProductEditorListeners();

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

    private void setupProductEditorListeners() {

        // =========================
        // PRODUCT NAME editor
        // =========================
        if (ivEditProduct != null) {
            ivEditProduct.setOnClickListener(v -> {
                if (ensureModelSelectedBeforeEdit()) return;

                closeAllInlineEditors();

                String current = scannedProduct == null ? "" : scannedProduct.getText().toString();
                etProductNameEdit.setText(current.equals("-") ? "" : current);

                productEditLayout.setVisibility(View.VISIBLE);
                productEditButtons.setVisibility(View.VISIBLE);
                etProductNameEdit.requestFocus();
                showKeyboard(etProductNameEdit);
            });
        }

        if (btnCancelProductEdit != null) {
            btnCancelProductEdit.setOnClickListener(v -> hideProductEditor());
        }

        etProductNameEdit.setOnEditorActionListener((v, actionId, event) -> {
            commitProductEditLocally();
            return true;
        });

        if (btnSaveProduct != null) {
            btnSaveProduct.setOnClickListener(v -> commitProductEditLocally());
        }

        // =========================
        // SKU editor
        // =========================
        if (ivEditProductSKU != null) {
            ivEditProductSKU.setOnClickListener(v -> {
                if (ensureModelSelectedBeforeEdit()) return;

                closeAllInlineEditors();

                String currentSku = scannedProductSKU == null ? "" : scannedProductSKU.getText().toString();
                etProductSkuEdit.setText(currentSku.equals("-") ? "" : currentSku);

                productSkuEditLayout.setVisibility(View.VISIBLE);
                productSkuEditButtons.setVisibility(View.VISIBLE);
                etProductSkuEdit.requestFocus();
                showKeyboard(etProductSkuEdit);
            });
        }

        if (btnCancelSkuEdit != null) {
            btnCancelSkuEdit.setOnClickListener(v -> {
                productSkuEditLayout.setVisibility(View.GONE);
                productSkuEditButtons.setVisibility(View.GONE);
                etProductSkuEdit.setText("");
                hideKeyboard(etProductSkuEdit);
            });
        }

        etProductSkuEdit.setOnEditorActionListener((v, actionId, event) -> {
            commitSkuEditLocally();
            return true;
        });

        if (btnSaveSku != null) {
            btnSaveSku.setOnClickListener(v -> commitSkuEditLocally());
        }

        // =========================
        // SIZE editor
        // =========================
        if (ivEditProductSize != null) {
            ivEditProductSize.setOnClickListener(v -> {
                if (ensureModelSelectedBeforeEdit()) return;

                closeAllInlineEditors();

                String currentSize = scannedSize == null ? "" : scannedSize.getText().toString();
                etProductSizeEdit.setText(currentSize.equals("-") ? "" : currentSize);

                productSizeEditLayout.setVisibility(View.VISIBLE);
                productSizeEditButtons.setVisibility(View.VISIBLE);
                etProductSizeEdit.requestFocus();
                showKeyboard(etProductSizeEdit);
            });
        }

        if (btnCancelSizeEdit != null) {
            btnCancelSizeEdit.setOnClickListener(v -> {
                productSizeEditLayout.setVisibility(View.GONE);
                productSizeEditButtons.setVisibility(View.GONE);
                etProductSizeEdit.setText("");
                hideKeyboard(etProductSizeEdit);
            });
        }

        etProductSizeEdit.setOnEditorActionListener((v, actionId, event) -> {
            commitSizeEditLocally();
            return true;
        });

        if (btnSaveSize != null) {
            btnSaveSize.setOnClickListener(v -> commitSizeEditLocally());
        }
    }


    private void closeAllInlineEditors() {

        if (productEditLayout != null) productEditLayout.setVisibility(View.GONE);
        if (productEditButtons != null) productEditButtons.setVisibility(View.GONE);

        if (productSkuEditLayout != null) productSkuEditLayout.setVisibility(View.GONE);
        if (productSkuEditButtons != null) productSkuEditButtons.setVisibility(View.GONE);

        if (productSizeEditLayout != null) productSizeEditLayout.setVisibility(View.GONE);
        if (productSizeEditButtons != null) productSizeEditButtons.setVisibility(View.GONE);

        if (qaEditLayout != null) qaEditLayout.setVisibility(View.GONE);
        if (qaEditButtons != null) qaEditButtons.setVisibility(View.GONE);

        hideKeyboard(etProductNameEdit);
        hideKeyboard(etProductSkuEdit);
        hideKeyboard(etProductSizeEdit);
        hideKeyboard(etQaEdit);
    }


    // --- SCAN / API methods ---

    private void startRFIDScan() {
        if (mDevice == null) {
            Toast.makeText(this, "UHF device not available", Toast.LENGTH_SHORT).show();
            return;
        }
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

                                playScanSuccessSound();
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

        mainHandler.postDelayed(() -> {
            if (isScanning) {
                Log.d("RFID_SCAN", "Auto-stop: Timeout reached");
                stopRFIDScan();
                Toast.makeText(ReprocessActivity.this, "Scan timeout (4 seconds)", Toast.LENGTH_SHORT).show();
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
            ivCardPrint.setVisibility(View.GONE);
            ivCardClear.setVisibility(View.GONE);
            defectsCard.setVisibility(View.GONE);
        } else {
            startScanBtn.setText("Start Scan");
            scanProgress.setVisibility(View.GONE);
            scanStatusText.setVisibility(View.GONE);
        }
    }

    private void checkProductWithAPI(String tagId) {

        if (scanStatusText != null) {
            scanStatusText.setText("Checking product...");
            scanStatusText.setVisibility(View.VISIBLE);
        }

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);

        apiService.getProductDetailsByTagId(tagId)
                .enqueue(new Callback<TagResponse>() {

                    @Override
                    public void onResponse(
                            Call<TagResponse> call,
                            Response<TagResponse> response
                    ) {
                        if (isFinishing() || isDestroyed()) return;

                        if (scanStatusText != null) {
                            scanStatusText.setVisibility(View.GONE);
                        }

                        // ---------- SUCCESS ----------
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getReprocessProduct() != null) {

                            selectedProduct = response.body().getReprocessProduct();
                            lastScannedTagId = tagId;

                            String latestStage = selectedProduct.getLatestStage() == null
                                    ? ""
                                    : selectedProduct.getLatestStage().trim();

                            String latestStatus = selectedProduct.getLatestStatus() == null
                                    ? ""
                                    : selectedProduct.getLatestStatus().trim();

                            // Case 1: Already Reprocess + RETURN
                            if ("reprocess".equalsIgnoreCase(latestStage)
                                    && "RETURN".equalsIgnoreCase(latestStatus)) {

                                displayProductDetails(tagId, selectedProduct);

                                SnackbarHelper.showTopCenter(
                                        ReprocessActivity.this,
                                        "This item is already under reprocess",
                                        false
                                );

                                if (updateStageBtn != null) {
                                    updateStageBtn.setEnabled(false);
                                }
                                return;
                            }

                            // Case 2: Not in Packaging stage
                            if (!"packaging".equalsIgnoreCase(latestStage)) {

                                SnackbarHelper.showTopCenter(
                                        ReprocessActivity.this,
                                        "Product must be in Packaging stage to create Reprocess",
                                        false
                                );

                                if (updateStageBtn != null) {
                                    updateStageBtn.setEnabled(false);
                                }
                                return;
                            }

                            // Case 3: Valid Packaging stage → allow reprocess
                            displayProductDetails(tagId, selectedProduct);
                            return;
                        }

                        // ---------- NO PRODUCT / INVALID ----------
                        showNoProductFound(tagId);
                    }

                    @Override
                    public void onFailure(
                            Call<TagResponse> call,
                            Throwable t
                    ) {
                        if (isFinishing() || isDestroyed()) return;

                        if (scanStatusText != null) {
                            scanStatusText.setVisibility(View.GONE);
                        }

                        Toast.makeText(
                                ReprocessActivity.this,
                                "Network error: " + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        Log.e("API_ERROR", "Failed to fetch product by tagId=" + tagId, t);

                        showNoProductFound(tagId);
                    }
                });
    }


    private void displayProductDetails(String tagId, BondingProductModel product) {
        if (product == null) return;

        selectedProduct = product;
        lastScannedTagId = tagId;

        // Basic fields
        if (qaCode != null) qaCode.setText(product.getQAcode());
        if (scannedProduct != null) scannedProduct.setText(product.getProductName());
        if (scannedProductSKU != null) scannedProductSKU.setText(product.getSku());
        if (scannedSize != null) scannedSize.setText(product.getSize() == null ? "-" : product.getSize());
        if (scannedStatus != null) scannedStatus.setText(product.getLatestStatus() == null ? "-" : product.getLatestStatus());

        // Locking by stage: packaging remains editable in this activity
        applyLockingByStage(product.getLatestStage());

        // Keep inline editors visible for packaging -> reprocess flow
        updateProductEditIconsVisibility(product.getLatestStage());

        String qc_status = product.getLatestStatus() == null ? "PENDING" : product.getLatestStatus();
        String remarks = product.getLatestRemarks() == null ? "" : product.getLatestRemarks();
        if (etRemarks != null) {
            etRemarks.setText(remarks);
            etRemarks.setEnabled(true);
        }

        // Refresh dropdowns and defects for this product
        fetchStagesAndStatuses(product.getLatestStage(), qc_status, remarks);

        // Show card & icons
        if (scanResultCard != null) scanResultCard.setVisibility(View.VISIBLE);
        if (ivCardPrint != null) ivCardPrint.setVisibility(View.VISIBLE);
        if (ivCardClear != null) ivCardClear.setVisibility(View.VISIBLE);

        // Make dropdowns interactive again
        scannedStage.setEnabled(true);
        scannedQcStatus.setEnabled(true);

        // reset pending edits when loading a new product
        pendingProductName = pendingSku = pendingQa = pendingSize = null;
        pendingModel = null;
        pendingBondingProductId = null;
        hasPendingEdits = false;
        evaluateUpdateButtonState();
    }

    private void applyLockingByStage(String stage) {
        boolean locked = isStageLocked(stage);
        if (scannedStage != null) scannedStage.setEnabled(!locked);
        if (scannedQcStatus != null) scannedQcStatus.setEnabled(!locked);
        if (etRemarks != null) etRemarks.setEnabled(!locked);
        if (locked) {
            if (scannedStage != null) scannedStage.setHint("Locked after final stage");
            if (scannedQcStatus != null) scannedQcStatus.setHint("Locked");
        } else {
            if (scannedStage != null) scannedStage.setHint("");
            if (scannedQcStatus != null) scannedQcStatus.setHint("");
        }
    }

    private boolean isStageLocked(String stage) {
        if (stage == null) return false;
        String s = stage.trim().toLowerCase();
        return s.equals("ready for shipment") || s.equals("shipped");
    }

    private void handleStageSelectionForDefects() {
        if (selectedProduct == null) {
            if (defectsCard != null) defectsCard.setVisibility(View.GONE);
            if (llDefectsContainer != null) llDefectsContainer.removeAllViews();
            evaluateUpdateButtonState();
            return;
        }

        String selectedStageDisplay = scannedStage.getText() == null ? "" : scannedStage.getText().toString().trim();
        String stageKey = getKeyForValue(lastStagesMap, selectedStageDisplay);

        if (stageKey == null || stageKey.isEmpty()) {
            if (defectsCard != null) defectsCard.setVisibility(View.GONE);
            if (llDefectsContainer != null) llDefectsContainer.removeAllViews();
            evaluateUpdateButtonState();
            return;
        }

        // AUTO-SELECT QC -> Return when stage == reprocess
        if ("reprocess".equalsIgnoreCase(stageKey)) {
            String displayReturn = getValueForKey(lastStatusMap, "RETURN");
            if (displayReturn == null) displayReturn = "RETURN";
            String finalDisplayReturn = displayReturn;
            scannedQcStatus.post(() -> {
                scannedQcStatus.setText(finalDisplayReturn, false);
                scannedQcStatus.setEnabled(false);
            });
            updateStageBtn.setEnabled(true);
        } else {
            scannedQcStatus.post(() -> scannedQcStatus.setEnabled(true));
        }

        // If defects configured for this stage -> populate
        if (lastDefectsMap.containsKey(stageKey)) {
            populateDefectsForStage(stageKey);
            if (defectsCard != null) defectsCard.setVisibility(View.VISIBLE);
        } else {
            if (defectsCard != null) defectsCard.setVisibility(View.GONE);
            if (llDefectsContainer != null) llDefectsContainer.removeAllViews();
        }
        evaluateUpdateButtonState();
    }

    private void populateDefectsForStage(String stageKey) {
        if (llDefectsContainer == null) return;
        llDefectsContainer.removeAllViews();

        List<Map<String, String>> defects = lastDefectsMap.get(stageKey);
        if (defects == null) defects = new ArrayList<>();

        List<String> existingDefects = null;
        if (selectedProduct != null && selectedProduct.getLatestDefectsPoints() != null) {
            existingDefects = selectedProduct.getLatestDefectsPoints();
        }

        if (defects.isEmpty()) {
            com.google.android.material.textview.MaterialTextView tv = new com.google.android.material.textview.MaterialTextView(this);
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

    /**
     * New onUpdateClicked: ensures model exists and persists pending edits (if any) before stage update.
     */
    private void onUpdateClicked() {

        // -----------------------------
        // BASIC GUARD
        // -----------------------------
        if (selectedProduct == null) {
            SnackbarHelper.showTopCenter(this, "No product loaded", false);
            return;
        }

        // -----------------------------
        // MODEL MUST EXIST
        // -----------------------------
        String model = null;
        try { model = selectedProduct.getModel(); } catch (Exception ignored) {}

        if ((model == null || model.trim().isEmpty())
                && (pendingModel == null || pendingModel.trim().isEmpty())) {
            showModelSelectDialog();
            return;
        }

        // -----------------------------
        // READ DROPDOWN VALUES
        // -----------------------------
        String stageDisplay =
                scannedStage.getText() == null
                        ? ""
                        : scannedStage.getText().toString().trim();

        String statusDisplay =
                scannedQcStatus.getText() == null
                        ? ""
                        : scannedQcStatus.getText().toString().trim();

        // -----------------------------
        // MAP DISPLAY → KEY
        // -----------------------------
        String stageKey = getKeyForValue(lastStagesMap, stageDisplay);
        String statusKey = getKeyForValue(lastStatusMap, statusDisplay);

        // -----------------------------
        // HARD VALIDATION
        // -----------------------------
        if (stageKey == null || !"reprocess".equalsIgnoreCase(stageKey)) {
            SnackbarHelper.showTopCenter(
                    this,
                    "❌ Please select Reprocess stage",
                    false
            );
            return;
        }

        if (statusKey == null || !"RETURN".equalsIgnoreCase(statusKey)) {
            SnackbarHelper.showTopCenter(
                    this,
                    "❌ Please select Return status",
                    false
            );
            return;
        }

        // -----------------------------
        // COLLECT DEFECT POINTS
        // -----------------------------
        List<String> selectedDefects = new ArrayList<>();
        if (llDefectsContainer != null) {
            for (int i = 0; i < llDefectsContainer.getChildCount(); i++) {
                View v = llDefectsContainer.getChildAt(i);
                if (v instanceof CheckBox) {
                    CheckBox cb = (CheckBox) v;
                    if (cb.isChecked() && cb.getTag() != null) {
                        selectedDefects.add(cb.getTag().toString());
                    }
                }
            }
        }
        List<String> defectsToSend =
                selectedDefects.isEmpty() ? null : selectedDefects;

        // -----------------------------
        // COLLECT REMARKS
        // -----------------------------
        String remarksInput =
                etRemarks == null || etRemarks.getText() == null
                        ? null
                        : etRemarks.getText().toString().trim();

        String remarksToSend =
                (remarksInput == null || remarksInput.isEmpty())
                        ? null
                        : remarksInput;

        // -----------------------------
        // PREVENT DOUBLE CLICK
        // -----------------------------
        if (updateStageBtn != null) {
            updateStageBtn.setEnabled(false);
        }

        // -----------------------------
        // SINGLE API CALL
        // -----------------------------
        saveProductFields(
                lastScannedTagId,
                pendingProductName,
                pendingSku,
                pendingQa,
                pendingSize,
                pendingModel,
                pendingBondingProductId,
                defectsToSend,
                remarksToSend,

                // -----------------------------
                // SUCCESS CALLBACK
                // -----------------------------
                () -> {
                    hasPendingEdits = false;
                    pendingProductName = pendingSku = pendingQa = pendingSize = null;
                    pendingModel = null;
                    pendingBondingProductId = null;

                    runOnUiThread(() -> {
                        // ❌ NO SUCCESS MESSAGE HERE
                        // ✔ Message is shown inside saveProductFields()
                        if (updateStageBtn != null) {
                            updateStageBtn.setEnabled(false);
                        }

                        mainHandler.postDelayed(
                                ReprocessActivity.this::clearUI,
                                1000
                        );
                    });
                },

                // -----------------------------
                // ERROR CALLBACK
                // -----------------------------
                error -> runOnUiThread(() -> {
                    if (updateStageBtn != null) {
                        updateStageBtn.setEnabled(true);
                    }
                    SnackbarHelper.showTopCenter(
                            ReprocessActivity.this,
                            "⚠ " + error,
                            false
                    );
                })
        );
    }



    private void showNoProductFound(String tagId) {
        lastScannedTagId = tagId;
        selectedProduct = null;

        if (qaCode != null) qaCode.setText("-");
        if (scannedProduct != null) scannedProduct.setText("No product found");
        if (scannedProductSKU != null) scannedProductSKU.setText("-");
        if (scannedSize != null) scannedSize.setText("Unknown");
        if (scannedStatus != null) scannedStatus.setText("N/A");

        if (scannedStage != null) scannedStage.setText("", false);
        if (scannedQcStatus != null) scannedQcStatus.setText("", false);

        if (scanResultCard != null) scanResultCard.setVisibility(View.VISIBLE);

        if (ivCardPrint != null) ivCardPrint.setVisibility(View.VISIBLE);
        if (ivCardClear != null) ivCardClear.setVisibility(View.VISIBLE);

        if (productEditLayout != null) productEditLayout.setVisibility(View.GONE);
        if (productEditButtons != null) productEditButtons.setVisibility(View.GONE);
        if (productSkuEditLayout != null) productSkuEditLayout.setVisibility(View.GONE);
        if (productSkuEditButtons != null) productSkuEditButtons.setVisibility(View.GONE);
        if (productSizeEditLayout != null) productSizeEditLayout.setVisibility(View.GONE);
        if (productSizeEditButtons != null) productSizeEditButtons.setVisibility(View.GONE);
        if (etRemarks != null) etRemarks.setText("");
        if (llDefectsContainer != null) llDefectsContainer.removeAllViews();
        if (defectsCard != null) defectsCard.setVisibility(View.GONE);

        if (updateStageBtn != null) updateStageBtn.setEnabled(false);
    }

    private void clearUI() {
        if (selectedModel != null) selectedModel.setText("Select Model First");
        if (scannedProduct != null) scannedProduct.setText("-");
        if (scannedStage != null) scannedStage.setText("", false);
        if (scannedQcStatus != null) scannedQcStatus.setText("", false);
        if (scannedSize != null) scannedSize.setText("-");
        if (scannedStatus != null) scannedStatus.setText("-");
        if (scanResultCard != null) scanResultCard.setVisibility(View.GONE);

        if (updateStageBtn != null) updateStageBtn.setEnabled(false);
        if (etRemarks != null) etRemarks.setText("");
        if (defectsCard != null) defectsCard.setVisibility(View.GONE);
        if (llDefectsContainer != null) llDefectsContainer.removeAllViews();

        if (ivCardClear != null) ivCardClear.setVisibility(View.GONE);
        if (ivCardPrint != null) ivCardPrint.setVisibility(View.GONE);

        selectedProduct = null;
        lastScannedTagId = null;
        pendingProductName = pendingSku = pendingQa = pendingSize = null;
        pendingModel = null;
        pendingBondingProductId = null;
        hasPendingEdits = false;
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
    private void fetchStagesAndStatuses(String latestStage, String latestStatus, String latestRemarks) {
        if (isFinishing() || isDestroyed()) return;

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        StagesStatusRequest request = new StagesStatusRequest(latestStage, latestStatus, latestRemarks);

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

                        lastAllowedStageKeys = new ArrayList<>(allowedStageKeys);
                        PrefHelper.saveAllowedStages(ReprocessActivity.this, allowedStageKeys);
                    }
                }

                // Ensure CURRENT stage is added to dropdown ALWAYS
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

                ArrayAdapter<String> stageAdapter = new ArrayAdapter<>(
                        ReprocessActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        stagesList
                );
                scannedStage.setAdapter(stageAdapter);
                scannedStage.setThreshold(0);

                ArrayAdapter<String> qcAdapter = new ArrayAdapter<>(
                        ReprocessActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        qcList
                );
                scannedQcStatus.setAdapter(qcAdapter);
                scannedQcStatus.setThreshold(0);

                scannedStage.setText("", false);
                scannedQcStatus.setText("", false);

                // Preselect stage + QC
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

                // Lock/Unlock stage dropdown depending on allowed_stages
                boolean isUserAllowed = false;

                if (latestStage != null && allowedStageKeys != null && !allowedStageKeys.isEmpty()) {
                    for (String k : allowedStageKeys) {
                        if (k != null && k.equalsIgnoreCase(latestStage)) {
                            isUserAllowed = true;
                            break;
                        }
                    }
                } else {
                    isUserAllowed = true;
                }

                scannedStage.setEnabled(isUserAllowed);
                if (!isUserAllowed) scannedStage.setHint("Stage (read-only)");
                else scannedStage.setHint("");

                scannedQcStatus.setEnabled(true);

                // Update buttons & defect logic
                handleStageSelectionForDefects();
                applyLockingByStage(latestStage);
                evaluateUpdateButtonState();
            }

            @Override
            public void onFailure(Call<StagesStatusResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ReprocessActivity.this, "Error fetching stages/status: " + t.getMessage(), Toast.LENGTH_LONG).show();
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

    private void updateProductEditIconsVisibility(@Nullable String stageKeyOrDisplay) {
        boolean hideEditors = false;

        if (stageKeyOrDisplay != null) {
            String s = stageKeyOrDisplay.trim().toLowerCase();
            if ("ready for shipment".equalsIgnoreCase(s) || "shipped".equalsIgnoreCase(s)) {
                hideEditors = true;
            } else {
                String possibleKey = getKeyForValue(lastStagesMap, s);
                if (possibleKey != null && (possibleKey.equalsIgnoreCase("ready for shipment") || possibleKey.equalsIgnoreCase("shipped"))) {
                    hideEditors = true;
                }
            }
        }

        final int vis = hideEditors ? View.GONE : View.VISIBLE;

        if (ivEditQaCode != null) ivEditQaCode.setVisibility(vis);
        if (ivEditProduct != null) ivEditProduct.setVisibility(vis);
        if (ivEditProductSKU != null) ivEditProductSKU.setVisibility(vis);
        if (ivEditProductSize != null) ivEditProductSize.setVisibility(vis);

        if (hideEditors) {
            if (productEditLayout != null) productEditLayout.setVisibility(View.GONE);
            if (productEditButtons != null) productEditButtons.setVisibility(View.GONE);
            if (productSkuEditLayout != null) productSkuEditLayout.setVisibility(View.GONE);
            if (productSkuEditButtons != null) productSkuEditButtons.setVisibility(View.GONE);
            if (productSizeEditLayout != null) productSizeEditLayout.setVisibility(View.GONE);
            if (productSizeEditButtons != null) productSizeEditButtons.setVisibility(View.GONE);
            if (etProductNameEdit != null) etProductNameEdit.setText("");
            if (etProductSkuEdit != null) etProductSkuEdit.setText("");
            if (etProductSizeEdit != null) etProductSizeEdit.setText("");
            // also hide/clear QA editor if present
            if (qaEditLayout != null) qaEditLayout.setVisibility(View.GONE);
            if (qaEditButtons != null) qaEditButtons.setVisibility(View.GONE);
            if (etQaEdit != null) etQaEdit.setText("");
            hideKeyboard(etProductNameEdit);
            hideKeyboard(etProductSkuEdit);
            hideKeyboard(etProductSizeEdit);
            hideKeyboard(etQaEdit);
        }
    }

    private void evaluateUpdateButtonState() {
        if (updateStageBtn == null) return;

        if (selectedProduct == null) {
            updateStageBtn.setEnabled(false);
            return;
        }

        // require model presence
        String model = null;
        try { model = selectedProduct != null ? selectedProduct.getModel() : null; } catch (Exception ignored) {}
        if ((model == null || model.trim().isEmpty()) && (pendingModel == null || pendingModel.trim().isEmpty())) {
            updateStageBtn.setEnabled(false);
            return;
        }
        String stageDisplay = scannedStage.getText() == null ? "" : scannedStage.getText().toString().trim();
        String qcDisplay = scannedQcStatus.getText() == null ? "" : scannedQcStatus.getText().toString().trim();

        String latestStage = selectedProduct.getLatestStage() == null ? "" : selectedProduct.getLatestStage().trim();
        String latestStatus = selectedProduct.getLatestStatus() == null ? "" : selectedProduct.getLatestStatus().trim();
        if ("reprocess".equalsIgnoreCase(latestStage) && "RETURN".equalsIgnoreCase(latestStatus)) {
            updateStageBtn.setEnabled(false);
            return;
        }

        if (!"packaging".equalsIgnoreCase(latestStage)) {
            updateStageBtn.setEnabled(false);
            return;
        }

        String selectedStageKey = getKeyForValue(lastStagesMap, stageDisplay);
        if (selectedStageKey == null) selectedStageKey = stageDisplay;

        String selectedQcKey = getKeyForValue(lastStatusMap, qcDisplay);
        if (selectedQcKey == null) selectedQcKey = qcDisplay;

        updateStageBtn.setEnabled(true);
    }

    private void showConfirmPrintDialog(BondingProductModel product) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Print")
                .setMessage("Do you want to take print?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    try {
                        String qaCode = product != null ? product.getQAcode() : "TEST";
                        String productName = product != null ? product.getProductName() : "TEST PRINT";
                        String size = product != null ? product.getSize() : "-";
                        String stage = product != null ? product.getLatestStage() : "-";
                        String status = product != null ? product.getLatestStatus() : "-";
                        String sku = product != null ? product.getSku() : "-";
                        String referenceCode = product != null ? product.getReferenceCode() : "-";

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

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void hideProductEditor() {
        if (productEditLayout != null) productEditLayout.setVisibility(View.GONE);
        if (productEditButtons != null) productEditButtons.setVisibility(View.GONE);
        if (etProductNameEdit != null) etProductNameEdit.setText("");
        hideKeyboard(etProductNameEdit);
        if (btnSaveProduct != null) btnSaveProduct.setEnabled(true);
    }

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
     * Unified save method. NEW: accepts `size`, `model` and `bondingPlanProductId` (nullable).
     *
     * NOTE: You must update UpdateReprocessProductRequest to include `model` and `bonding_plan_product_id`
     * in its constructor/fields, and ensure ApiService.updateReprocessProductDetails accepts it.
     */
    private void saveProductFields(@Nullable String tagId,
                                   @Nullable String productName,
                                   @Nullable String sku,
                                   @Nullable String qa_code,
                                   @Nullable String size,
                                   @Nullable String model,
                                   @Nullable Long bondingPlanProductId,
                                   @Nullable List<String> defectsPoints,
                                   @Nullable String remarks,
                                   @NonNull Runnable onSuccess,
                                   @NonNull java.util.function.Consumer<String> onError) {

        if (scanStatusText != null) {
            scanStatusText.setText("Saving...");
            scanStatusText.setVisibility(View.VISIBLE);
        }

        // -----------------------------
        // NORMALIZE OPTIONAL FIELDS
        // -----------------------------
        List<String> defectsToSend =
                (defectsPoints == null || defectsPoints.isEmpty())
                        ? null
                        : defectsPoints;

        String remarksToSend =
                (remarks == null || remarks.trim().isEmpty())
                        ? null
                        : remarks.trim();

        // -----------------------------
        // BUILD REQUEST
        // -----------------------------
        UpdateReprocessProductRequest req =
                new UpdateReprocessProductRequest(
                        tagId,
                        productName,
                        sku,
                        qa_code,
                        size,
                        model,
                        bondingPlanProductId,
                        defectsToSend,
                        remarksToSend
                );

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.updateReprocessProductStage(req)
                .enqueue(new Callback<ReprocessTagResponse>() {

                    @Override
                    public void onResponse(Call<ReprocessTagResponse> call,
                                           Response<ReprocessTagResponse> response) {

                        if (isFinishing() || isDestroyed()) return;
                        if (scanStatusText != null) scanStatusText.setVisibility(View.GONE);

                        try {
                            // ---------------- HTTP SUCCESS ----------------
                            if (response.isSuccessful() && response.body() != null) {

                                ReprocessTagResponse body = response.body();

                                // ---------------- BUSINESS SUCCESS ----------------
                                if (body.isSuccess()) {

                                    if (body.getProduct() != null) {
                                        selectedProduct = body.getProduct();
                                    }

                                    runOnUiThread(() -> {
                                        String successMsg =
                                                (body.getMessage() != null && !body.getMessage().isEmpty())
                                                        ? body.getMessage()
                                                        : "Product updated successfully";

                                        SnackbarHelper.showTopCenter(
                                                ReprocessActivity.this,
                                                "✔ " + successMsg,
                                                true
                                        );

                                        onSuccess.run();
                                    });
                                    return;
                                }

                                // ---------------- BUSINESS FAILURE ----------------
                                String serverMessage =
                                        (body.getMessage() != null && !body.getMessage().isEmpty())
                                                ? body.getMessage()
                                                : "Failed to update product";

                                runOnUiThread(() -> onError.accept(serverMessage));
                                return;
                            }

                            // ---------------- HTTP FAILURE (4xx / 5xx) ---------------

                            String tempErrorMsg = "Failed to update product";

                            if (response.errorBody() != null) {
                                try {
                                    String errorJson = response.errorBody().string();
                                    JSONObject obj = new JSONObject(errorJson);

                                    if (obj.has("message")) {
                                        tempErrorMsg = obj.getString("message");
                                    }
                                } catch (Exception ignored) {}
                            }

                            // ✅ make final copy for lambda
                            final String finalErrorMsg = tempErrorMsg;

                            runOnUiThread(() -> onError.accept(finalErrorMsg));

                        } catch (Exception e) {
                            Log.e("API_ERROR", "Unhandled exception", e);
                            runOnUiThread(() ->
                                    onError.accept("Unexpected server response"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ReprocessTagResponse> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        if (scanStatusText != null) scanStatusText.setVisibility(View.GONE);

                        Log.e("API_ERROR", "saveProductFields failed", t);
                        runOnUiThread(() ->
                                onError.accept("Network error: " + t.getMessage()));
                    }
                });
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

    // -------------------- Local commit helpers & model selection --------------------

    private void commitProductEditLocally() {
        if (etProductNameEdit == null || productEditLayout == null) return;
        String newName = etProductNameEdit.getText() == null ? "" : etProductNameEdit.getText().toString().trim();
        if (newName.isEmpty()) {
            productEditLayout.setError("Product name cannot be empty");
            etProductNameEdit.requestFocus();
            return;
        }
        productEditLayout.setError(null);
        pendingProductName = newName;
        hasPendingEdits = true;

        if (scannedProduct != null) scannedProduct.setText(newName);
        if (selectedProduct != null) {
            try { selectedProduct.setProductName(newName); } catch (Exception ignored) {}
        }
        hideProductEditor();
        SnackbarHelper.showTopCenter(this, "✔ Product edited (will save on Update)", true);
    }

    private void commitSkuEditLocally() {
        if (etProductSkuEdit == null || productSkuEditLayout == null) return;
        String newSku = etProductSkuEdit.getText() == null ? "" : etProductSkuEdit.getText().toString().trim();
        if (newSku.isEmpty()) {
            productSkuEditLayout.setError("SKU cannot be empty");
            etProductSkuEdit.requestFocus();
            return;
        }
        productSkuEditLayout.setError(null);
        pendingSku = newSku;
        hasPendingEdits = true;

        if (scannedProductSKU != null) scannedProductSKU.setText(newSku);
        if (selectedProduct != null) {
            try { selectedProduct.setSku(newSku); } catch (Exception ignored) {}
        }
        if (productSkuEditLayout != null) productSkuEditLayout.setVisibility(View.GONE);
        if (productSkuEditButtons != null) productSkuEditButtons.setVisibility(View.GONE);
        hideKeyboard(etProductSkuEdit);
        SnackbarHelper.showTopCenter(this, "✔ SKU edited (will save on Update)", true);
    }

    private void commitQaEditLocally() {
        if (etQaEdit == null || qaEditLayout == null) return;
        String newQa = etQaEdit.getText() == null ? "" : etQaEdit.getText().toString().trim();
        if (newQa.isEmpty()) {
            qaEditLayout.setError("QA code cannot be empty");
            etQaEdit.requestFocus();
            return;
        }
        qaEditLayout.setError(null);
        pendingQa = newQa;
        hasPendingEdits = true;

        if (qaCode != null) qaCode.setText(newQa);
        if (selectedProduct != null) {
            try { selectedProduct.setQAcode(newQa); } catch (Exception ignored) {}
        }
        if (qaEditLayout != null) qaEditLayout.setVisibility(View.GONE);
        if (qaEditButtons != null) qaEditButtons.setVisibility(View.GONE);
        hideKeyboard(etQaEdit);
        SnackbarHelper.showTopCenter(this, "✔ QA edited (will save on Update)", true);
    }

    private void commitSizeEditLocally() {
        if (etProductSizeEdit == null || productSizeEditLayout == null) return;
        String newSize = etProductSizeEdit.getText() == null ? "" : etProductSizeEdit.getText().toString().trim();
        if (newSize.isEmpty()) {
            productSizeEditLayout.setError("Size cannot be empty");
            etProductSizeEdit.requestFocus();
            return;
        }
        productSizeEditLayout.setError(null);
        pendingSize = newSize;
        hasPendingEdits = true;

        if (scannedSize != null) scannedSize.setText(newSize);
        if (selectedProduct != null) {
            try { selectedProduct.setSize(newSize); } catch (Exception ignored) {}
        }
        if (productSizeEditLayout != null) productSizeEditLayout.setVisibility(View.GONE);
        if (productSizeEditButtons != null) productSizeEditButtons.setVisibility(View.GONE);
        hideKeyboard(etProductSizeEdit);
        SnackbarHelper.showTopCenter(this, "✔ Size edited (will save on Update)", true);
    }

    private boolean ensureModelSelectedBeforeEdit() {
        if (selectedProduct == null) return false;
        String model = null;
        try { model = selectedProduct.getModel(); } catch (Exception ignored) {}
        if (model == null || model.trim().isEmpty()) {
            showModelSelectDialog();
            return true; // dialog shown -> editing should stop for now
        }
        return false;
    }

    private void showModelSelectDialog() {
        if (isModelSelectionInProgress) return;
        isModelSelectionInProgress = true;

        final androidx.appcompat.app.AlertDialog loading =
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Select Model / QA")
                        .setMessage("Loading models...")
                        .setCancelable(false)
                        .show();

        BondingProductsRequest req = new BondingProductsRequest();
        req.setSearch("");
        req.setPage(1);
        req.setLimit(200);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.getPlanProducts(req).enqueue(new Callback<BondingResponse>() {
            @Override
            public void onResponse(Call<BondingResponse> call, Response<BondingResponse> response) {
                isModelSelectionInProgress = false;
                try { loading.dismiss(); } catch (Exception ignored) {}
                if (isFinishing() || isDestroyed()) return;

                List<BondingProduct> products = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<BondingProduct> fetched = response.body().getProducts();
                    if (fetched != null) products.addAll(fetched);
                }

                if (products.isEmpty()) {
                    new androidx.appcompat.app.AlertDialog.Builder(ReprocessActivity.this)
                            .setTitle("No models")
                            .setMessage("No models available to select.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                // inflate custom dialog view
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_product_select, null);
                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(ReprocessActivity.this)
                        .setView(dialogView)
                        .create();

                TextInputEditText etSearch = dialogView.findViewById(R.id.etDialogSearch);
                RecyclerView rv = dialogView.findViewById(R.id.rvDialogResults);
                TextView tvNo = dialogView.findViewById(R.id.tvDialogNoResults);
                CircularProgressIndicator pb = dialogView.findViewById(R.id.pbDialogLoading);

                rv.setLayoutManager(new LinearLayoutManager(ReprocessActivity.this));
                ProductSelectAdapter adapter = new ProductSelectAdapter(products, product -> {
                    // on item click -> SELECT but DO NOT persist immediately
                    dialog.dismiss();

                    // copy values into pending fields (user can edit further)
                    String chosenQa = product.getQaCode();

                    // SAVE BONDING PRODUCT ID
                    try {
                        pendingBondingProductId = Long.valueOf(product.getId());
                    } catch (Exception ignored) {
                        pendingBondingProductId = null;
                    }

                    if (selectedModel != null) {
                        selectedModel.setText("Model: " + chosenQa);
                    }

                    pendingQa = chosenQa;
                    pendingProductName = product.getProductName();
                    pendingSku = product.getSku();
                    pendingSize = product.getSize();
                    pendingModel = product.getModel();

                    hasPendingEdits = true;

                    // update UI immediately so user sees selection
                    if (selectedProduct != null) {
                        try {
                            selectedProduct.setQAcode(chosenQa);
                            selectedProduct.setProductName(pendingProductName);
                            selectedProduct.setSku(pendingSku);
                            selectedProduct.setSize(pendingSize);
                            selectedProduct.setModel(pendingModel);
                            if (pendingBondingProductId != null) {
                                try { selectedProduct.setBondingPlanProductId(pendingBondingProductId); } catch (Exception ignored) {}
                            }
                        } catch (Exception ignored) {}
                    }

                    if (qaCode != null) qaCode.setText(chosenQa);
                    if (scannedProduct != null && pendingProductName != null) scannedProduct.setText(pendingProductName);
                    if (scannedProductSKU != null && pendingSku != null) scannedProductSKU.setText(pendingSku);
                    if (scannedSize != null && pendingSize != null) scannedSize.setText(pendingSize);
                    // optionally show model somewhere if you have a field for it (add a TextView for model if needed)

                    // DO NOT call persistQaForProduct() here — we'll save everything on Update
                    evaluateUpdateButtonState();
                });

                rv.setAdapter(adapter);

                // search filter (client-side)
                etSearch.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                    @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        String q = s == null ? "" : s.toString().trim();
                        adapter.filter(q);
                        tvNo.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                    }
                });

                // pre-populate search hint
                etSearch.setHint("Search QA, model, name or slno");

                tvNo.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);

                dialog.show();
            }

            @Override
            public void onFailure(Call<BondingResponse> call, Throwable t) {
                isModelSelectionInProgress = false;
                try { loading.dismiss(); } catch (Exception ignored) {}
                if (isFinishing() || isDestroyed()) return;
                new androidx.appcompat.app.AlertDialog.Builder(ReprocessActivity.this)
                        .setTitle("Error")
                        .setMessage("Failed to load models: " + t.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }
}
