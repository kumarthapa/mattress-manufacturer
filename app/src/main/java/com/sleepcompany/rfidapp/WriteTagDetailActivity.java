package com.sleepcompany.rfidapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;
import com.sleepcompany.rfidapp.network.ApiClient;
import com.sleepcompany.rfidapp.network.ApiService;
import com.sleepcompany.rfidapp.network.BondingProductsRequest;
import com.sleepcompany.rfidapp.network.BondingResponse;
import com.sleepcompany.rfidapp.util.SnackbarHelper;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Updated ReadTagDetailActivity (previously WriteTagDetailActivity).
 * - Auto-uppercase letters, validate 2-5 chars, first char 1-9.
 * - Keeps existing RFID scanning + server update behavior.
 */
public class WriteTagDetailActivity extends BaseDrawerActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    public static final String EXTRA_QA_CODE = "extra_qa_code";
    public static final String EXTRA_PRODUCT_NAME = "extra_product_name";
    public static final String EXTRA_PRODUCT_MODEL = "extra_product_model";
    public static final String RESULT_FINAL_QA = "result_final_qa";

    private static final String TAG = "WriteTagDetail";

    private MaterialTextView tvTitleQaCode, tvTitleName, tvTitleModel, tvValidation;
    private MaterialTextView tvScannedTag, tvScanStatus;
    private TextInputEditText etExtraDetails;
    private MaterialButton btnScan, btnWrite;

    private UHFService mDevice;

    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> scanFuture;

    private String scannedTagId = null;
    private String initialQaCode = null;

    // hardware trigger keys
    private static final int[] HARDWARE_KEYS = {142, KeyEvent.KEYCODE_F1, KeyEvent.KEYCODE_F2};

    /**
     * Validation pattern:
     * - Total length: 2 to 5 characters
     * - First char: digit 1-9 (no leading zero)
     * - Remaining: uppercase alphanumeric (letters or digits)
     * Examples: 1K, 10G, 1234K, 11WEK, 345KH
     */
    private static final Pattern INPUT_PATTERN = Pattern.compile("^[1-9][A-Z0-9]{1,4}$");

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_write_tag_detail;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_tag_detail);

        setupToolbar();
        bindViews();
        readIntentData();
        initUiState();
        initUhfDevice();
        setupListeners();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Read / Update QA");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindViews() {
        btnScan = findViewById(R.id.btnScan);
        tvTitleQaCode = findViewById(R.id.tvTitleQaCode);
        tvTitleName = findViewById(R.id.tvTitleName);
        tvTitleModel = findViewById(R.id.tvTitleModel);
        etExtraDetails = findViewById(R.id.etExtraDetails);
        tvValidation = findViewById(R.id.tvValidation);
        tvScannedTag = findViewById(R.id.tvScannedTag);
        tvScanStatus = findViewById(R.id.tvScanStatus);
        btnWrite = findViewById(R.id.btnWrite);
    }

    private void readIntentData() {
        Intent i = getIntent();
        initialQaCode = i.getStringExtra(EXTRA_QA_CODE);
        String name = i.getStringExtra(EXTRA_PRODUCT_NAME);
        String model = i.getStringExtra(EXTRA_PRODUCT_MODEL);

        tvTitleQaCode.setText(initialQaCode != null ? initialQaCode : "-");
        tvTitleName.setText(name != null ? name : "-");
        tvTitleModel.setText(model != null ? model : "-");
    }

    private void initUiState() {
        tvScannedTag.setText("-");
        tvScanStatus.setVisibility(View.GONE);

        // prevent accidental typing when not focused; allow manual entry through explicit touch
        etExtraDetails.setFocusable(false);
        etExtraDetails.setFocusableInTouchMode(false);
        etExtraDetails.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)}); // max 5 chars

        btnScan.requestFocus();
        updateWriteButtonState(); // parameterless delegates to reading current text

        // if there is text (rare) validate it
        String initialVal = etExtraDetails.getText() == null ? "" : etExtraDetails.getText().toString().trim();
        validateInputAndUpdateUI(initialVal);
    }

    private void initUhfDevice() {
        try {
            mDevice = UHFService.getInstance();
            if (mDevice == null) throw new Exception("UHF hardware not available");
            Log.d(TAG, "UHF device initialized");
        } catch (Exception e) {
            mDevice = null;
            Toast.makeText(this, "⚠ This device does not support UHF RFID scanning.", Toast.LENGTH_LONG).show();
            Log.w(TAG, "UHFService not available", e);
        }
    }

    private boolean alertShown = false; // class-level variable

    private void setupListeners() {

        // Prevent typing if no tag scanned
        etExtraDetails.setOnTouchListener((v, event) -> {
            if (scannedTagId == null || scannedTagId.isEmpty()) {
                if (!alertShown) {
                    alertShown = true; // mark alert as shown
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Scan Required")
                            .setMessage("Please scan a tag first before entering details.")
                            .setPositiveButton("OK", (dialog, which) -> alertShown = false) // reset flag
                            .setCancelable(false) // force user to tap OK
                            .show();
                }
                return true; // consume touch, prevent typing
            } else {
                // Allow typing if tag scanned
                etExtraDetails.setFocusable(true);
                etExtraDetails.setFocusableInTouchMode(true);
                etExtraDetails.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(etExtraDetails, InputMethodManager.SHOW_IMPLICIT);
                return false;
            }
        });

        // Focus change listener remains same
        etExtraDetails.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(etExtraDetails.getWindowToken(), 0);
                etExtraDetails.post(() -> {
                    etExtraDetails.setFocusable(false);
                    etExtraDetails.setFocusableInTouchMode(false);
                });
            }
        });

        // TextWatcher remains same
        etExtraDetails.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String val = s == null ? "" : s.toString();

                if (val.length() > 5) {
                    String trimmed = val.substring(0, 5);
                    etExtraDetails.removeTextChangedListener(this);
                    etExtraDetails.setText(trimmed);
                    etExtraDetails.setSelection(trimmed.length());
                    etExtraDetails.addTextChangedListener(this);
                    val = trimmed;
                }

                String transformed = transformLettersToUpper(val);
                if (!val.equals(transformed)) {
                    int sel = etExtraDetails.getSelectionStart();
                    etExtraDetails.removeTextChangedListener(this);
                    etExtraDetails.setText(transformed);
                    int newPos = Math.min(transformed.length(), Math.max(0, sel));
                    etExtraDetails.setSelection(newPos);
                    etExtraDetails.addTextChangedListener(this);
                    val = transformed;
                }

                val = val.trim();
                validateInputAndUpdateUI(val);
                updateWriteButtonState(val);
            }
        });

        // Scan button remains same
        btnScan.setOnClickListener(v -> {
            if (etExtraDetails.hasFocus()) etExtraDetails.clearFocus();
            if (!isScanning.get()) startRFIDScan();
            else stopRFIDScan();
        });

        // Write button with alert if no tag scanned
        btnWrite.setOnClickListener(v -> {
            String extra = etExtraDetails.getText() == null ? "" : etExtraDetails.getText().toString().trim().toUpperCase();
            if (!isValidInput(extra)) {
                Toast.makeText(this, "Invalid extra details", Toast.LENGTH_SHORT).show();
                return;
            }
            if (scannedTagId == null || scannedTagId.isEmpty()) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Scan Required")
                        .setMessage("Please scan a tag first before writing details.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            final String finalQa = (initialQaCode != null ? initialQaCode : "") + extra;
            showConfirmDialogAndUpdate(scannedTagId, finalQa);
        });
    }



    /* ---------- Helper: transform letters to uppercase ---------- */
    private String transformLettersToUpper(String input) {
        if (input == null || input.length() == 0) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isLetter(c)) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /* ---------- Validation ---------- */

    private void validateInputAndUpdateUI(String val) {
        boolean isValid = validateInput(val);
        // Use the transformed value when updating button state
        updateWriteButtonState(val);
        Log.d(TAG, "Input: '" + val + "' - Valid: " + isValid + " - Button enabled: " + btnWrite.isEnabled());
    }

    /**
     * Parameterless helper kept for compatibility: reads current EditText, transforms and delegates.
     */
    private void updateWriteButtonState() {
        String currentInput = etExtraDetails.getText() == null ? "" : etExtraDetails.getText().toString().trim().toUpperCase();
        updateWriteButtonState(currentInput);
    }

    /**
     * Primary method — decide whether Write button should be enabled using the exact input string.
     */
    private void updateWriteButtonState(String currentInput) {
        String input = currentInput == null ? "" : currentInput.trim().toUpperCase();
        boolean inputValid = isValidInput(input);
        boolean tagScanned = scannedTagId != null && !scannedTagId.isEmpty();
        boolean shouldEnable = inputValid && tagScanned && !isScanning.get();

        btnWrite.setEnabled(shouldEnable);

        Log.d(TAG, "UpdateWriteButtonState - Input: '" + input + "' InputValid: " + inputValid
                + " TagScanned: " + tagScanned + " IsScanning: " + isScanning.get() + " ButtonEnabled: " + shouldEnable);
    }

    private boolean validateInput(String val) {
        // ✅ New check: if input is valid but no tag scanned
//        if (scannedTagId == null || scannedTagId.isEmpty()) {
//            tvValidation.setText("Please scan a tag first!");
//            tvValidation.setVisibility(View.VISIBLE);
//            Toast.makeText(this, "Please scan a tag first!", Toast.LENGTH_SHORT).show();
//            return false;
//        }

        if (val == null) val = "";

        if (val.isEmpty()) {
            tvValidation.setText("Enter 2 to 5 characters. First must be digit 1-9.");
            tvValidation.setVisibility(View.VISIBLE);
            return false;
        }



        if (val.length() < 2) {
            tvValidation.setText("Minimum 2 characters required.");
            tvValidation.setVisibility(View.VISIBLE);
            return false;
        }

        if (val.length() > 5) {
            tvValidation.setText("Maximum 5 characters allowed.");
            tvValidation.setVisibility(View.VISIBLE);
            return false;
        }

        if (!isValidInput(val)) {
            tvValidation.setText("Invalid format. First char must be digit 1-9. Only letters and digits allowed. No spaces.");
            tvValidation.setVisibility(View.VISIBLE);
            return false;
        }



        tvValidation.setText("");
        tvValidation.setVisibility(View.GONE);
        return true;
    }

    private boolean isValidInput(String v) {
        if (v == null) return false;
        v = v.trim().toUpperCase();
        if (v.length() < 2 || v.length() > 5) return false;
        boolean matches = INPUT_PATTERN.matcher(v).matches();
        Log.d(TAG, "isValidInput: '" + v + "' matches pattern: " + matches);
        return matches;
    }

    /* ---------- RFID scanning ---------- */

    private void startRFIDScan() {
        if (mDevice == null) {
            Toast.makeText(this, "UHF device not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // lock manual input while scanning
        if (etExtraDetails.hasFocus()) etExtraDetails.clearFocus();
        etExtraDetails.setFocusable(false);
        etExtraDetails.setFocusableInTouchMode(false);

        isScanning.set(true);
        updateScanUI(true);
        updateWriteButtonState(); // use parameterless helper

        scanFuture = executor.submit(() -> {
            EPC epc = new EPC();
            while (isScanning.get()) {
                try {
                    if (mDevice != null && mDevice.inventoryOnce(epc, 300)) {
                        String tagId = epc.getId();
                        if (tagId != null && !tagId.isEmpty()) {
                            runOnUiThread(() -> {
                                Log.d(TAG, "Scanned Tag ID: " + tagId);
                                scannedTagId = tagId.trim();
                                tvScannedTag.setText(scannedTagId);
                                tvScanStatus.setVisibility(View.VISIBLE);
                                tvScanStatus.setText("Tag scanned. Enter extra details to create QA code.");
                                // ✅ Play success sound
                                playScanSuccessSound();
                                stopRFIDScan();
                            });
                            break;
                        }
                    } else {
                        Log.d(TAG, "inventoryOnce returned false or no device");
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Exception during inventory", ex);
                    break;
                }

                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
            }
            runOnUiThread(() -> updateScanUI(false));
        });
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
        isScanning.set(false);
        if (scanFuture != null && !scanFuture.isDone()) scanFuture.cancel(true);
        updateScanUI(false);
        updateWriteButtonState();
    }

    private void updateScanUI(boolean scanning) {
        if (scanning) {
            btnScan.setText("Stop Scan");
            tvScanStatus.setVisibility(View.VISIBLE);
            tvScanStatus.setText("Scanning...");
            btnWrite.setEnabled(false);
        } else {
            btnScan.setText("Start Scan");
            if (scannedTagId == null || scannedTagId.isEmpty()) {
                tvScanStatus.setVisibility(View.GONE);
                tvScannedTag.setText("-");
            }
        }
    }

    /* ---------- Confirmation + Server update ---------- */

    private void showConfirmDialogAndUpdate(String epcId, String finalQa) {
        String message = "Final QA: " + finalQa + "\n\nUpdate this for tag " + epcId + " ?";
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Update")
                .setMessage(message)
                .setPositiveButton("Update", (dialog, which) -> updateQaCodeInBackend(epcId, finalQa))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateQaCodeInBackend(String tagId, String finalQa) {
        tvScanStatus.setVisibility(View.VISIBLE);
        tvScanStatus.setText("Updating server...");

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);

        BondingProductsRequest request = new BondingProductsRequest();
        request.setProductId(getIntent().getIntExtra(EXTRA_PRODUCT_ID, 0)); // product_id
        request.setQaCode(finalQa);                                         // qa_code
        request.setRfidTag(tagId);                                          // rfid_tag

        apiService.updateQaCode(request).enqueue(new Callback<BondingResponse>() {
            @Override
            public void onResponse(Call<BondingResponse> call, Response<BondingResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                tvScanStatus.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // ✅ Success
                    SnackbarHelper.showTopCenter(WriteTagDetailActivity.this, "✔ QA Code Updated Successfully", true);

                    Intent res = new Intent();
                    res.putExtra(RESULT_FINAL_QA, finalQa);
                    setResult(Activity.RESULT_OK, res);

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        Intent intent = new Intent(WriteTagDetailActivity.this, ScannerActivity.class);
                        startActivity(intent);
                        finish();
                    }, 1000);

                } else {
                    // ❌ Error from backend
                    String errorMessage = "Failed to update QA Code on server";
                    try {
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            JSONObject obj = new JSONObject(errorJson);

                            if (obj.has("message")) {
                                errorMessage = obj.getString("message");  // Laravel sends "The RFID Tag '...' is already in use."
                            } else if (obj.has("errors")) {
                                // Laravel validation errors
                                JSONObject errors = obj.getJSONObject("errors");
                                Iterator<String> keys = errors.keys();
                                if (keys.hasNext()) {
                                    String key = keys.next();
                                    errorMessage = errors.getJSONArray(key).getString(0);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing backend error response", e);
                    }

                    SnackbarHelper.showTopCenter(WriteTagDetailActivity.this, "❌ " + errorMessage, false);
                    btnScan.setEnabled(true);
                    updateWriteButtonState();
                }
            }

            @Override
            public void onFailure(Call<BondingResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                tvScanStatus.setVisibility(View.GONE);
                Toast.makeText(WriteTagDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "updateQaCodeInBackend failed", t);
                btnScan.setEnabled(true);
                updateWriteButtonState();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        for (int hwKey : HARDWARE_KEYS) {
            if (keyCode == hwKey) {
                if (etExtraDetails.hasFocus()) etExtraDetails.clearFocus();
                etExtraDetails.setFocusable(false);
                etExtraDetails.setFocusableInTouchMode(false);

                if (!isScanning.get()) startRFIDScan();
                else stopRFIDScan();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        // shutdown executor
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                Log.w(TAG, "Executor did not terminate in time");
            }
        } catch (InterruptedException ignored) {}
    }
}
