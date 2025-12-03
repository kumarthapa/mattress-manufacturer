package com.sleepcompany.rfidapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.seuic.uhf.UHFService;
import com.sleepcompany.rfidapp.util.PrefHelper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.sleepcompany.rfidapp.network.ApiClient;
import com.sleepcompany.rfidapp.network.ApiService;
import com.sleepcompany.rfidapp.network.CreateLicenseRequest;
import com.sleepcompany.rfidapp.network.DeviceCheckRequest;
import com.sleepcompany.rfidapp.network.LicenseRequest;
import com.sleepcompany.rfidapp.network.LicenseResponse;
import com.sleepcompany.rfidapp.network.LoginRequest;
import com.sleepcompany.rfidapp.network.LoginResponse;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Cleaned LoginActivity
 * - consistent UHFService usage (context-based)
 * - safe open/close checks
 * - sync RFID power after successful login
 * - clearer separation of concerns
 */
public class LoginActivity extends AppCompatActivity {
    private static final String ACT_TAG = "LoginActivity";

    private UHFService mDevice;

    // UI refs
    private TextInputEditText usernameInput, passwordInput;
    private TextInputLayout usernameLayout, passwordLayout;
    private MaterialCheckBox rememberMe;
    private MaterialButton loginBtn;

    // Shared prefs / keys
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_REMEMBER = "remember_me";
    private static final String KEY_SAVED_USER = "saved_user";
    private static final String KEY_SAVED_PASS = "saved_pass";
    private static final String KEY_LICENSE_VERIFIED = "license_verified";
    private static final String KEY_LICENSE_KEY = "license_key";
    private static final String KEY_LICENSE_END_DATE = "license_end_date";

    // dialog license input ref (for potential future use)
    private TextInputEditText dialogLicenseInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        bindViews();

        // Initialize UHFService using context-based API consistently
        try {
            mDevice = UHFService.getInstance(LoginActivity.this);
            if (mDevice == null) {
                throw new Exception("UHF hardware not available on this device");
            }
        } catch (Exception e) {
            mDevice = null;
            Toast.makeText(this, "⚠ This device does not support UHF RFID scanning.", Toast.LENGTH_LONG).show();
            Log.w(ACT_TAG, "UHF not available: " + e.getMessage());
            // keep behavior same as before: finish activity
            finish();
            return;
        }

        // Footer info
        TextView tvInfo = findViewById(R.id.tvAppInfo);
        if (tvInfo != null) {
            String version = BuildConfig.VERSION_NAME;

            // Remove "-debug" or any suffix after "-"
            if (version.contains("-")) {
                version = version.substring(0, version.indexOf("-"));
            }

            String deviceId = getAndroidId(this);
            tvInfo.setText("POWERED BY Galla | Version: " + version + " / DID: " + deviceId);
        }


        restoreSavedCredentialsIfAny();

        // License check
        if (!isLicenseVerified()) {
            String androidId = getAndroidId(this);
            if (TextUtils.isEmpty(androidId)) {
                Log.w(ACT_TAG, "ANDROID_ID not available - showing license dialog");
                showLicenseDialog(false, null);
            } else {
                checkDeviceWithServer(androidId, (ok, message, data) -> runOnUiThread(() -> {
                    if (ok && data != null) {
                        String status = data.status != null ? data.status.toUpperCase() : "UNKNOWN";
                        if ("ACTIVE".equals(status)) {
                            String licenseKey = data.licenseKey != null ? data.licenseKey : prefs.getString(KEY_LICENSE_KEY, "");
                            String endDate = data.endDate != null ? data.endDate : prefs.getString(KEY_LICENSE_END_DATE, "");
                            setLicenseVerifiedLocal(licenseKey, endDate);
                            Log.i(ACT_TAG, "Device ACTIVE license - saved locally");
                        } else if ("INACTIVE".equals(status) || "EXPIRE".equals(status)) {
                            showLicenseDialog(true, data.licenseKey);
                        } else {
                            showLicenseDialog(false, null);
                        }
                    } else {
                        showLicenseDialog(false, null);
                    }
                }));
            }
        }

        // Hook login button
        loginBtn.setOnClickListener(v -> attemptLogin());
    }

    private void bindViews() {
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        rememberMe = findViewById(R.id.rememberMe);
        loginBtn = findViewById(R.id.loginBtn);
    }

    private void restoreSavedCredentialsIfAny() {
        if (prefs.getBoolean(KEY_REMEMBER, false)) {
            usernameInput.setText(prefs.getString(KEY_SAVED_USER, ""));
            passwordInput.setText(prefs.getString(KEY_SAVED_PASS, ""));
            rememberMe.setChecked(true);
            Log.d(ACT_TAG, "Restored remembered credentials.");
        }
    }

    // ---------- Local license helpers ----------
    private boolean isLicenseVerified() {
        return prefs.getBoolean(KEY_LICENSE_VERIFIED, false);
    }

    private void setLicenseVerifiedLocal(String licenseKey, String endDate) {
        prefs.edit()
                .putBoolean(KEY_LICENSE_VERIFIED, true)
                .putString(KEY_LICENSE_KEY, licenseKey)
                .putString(KEY_LICENSE_END_DATE, endDate)
                .apply();

        PrefHelper.setLicenseVerified(this, true);
        PrefHelper.saveLicenseInfo(this, licenseKey, endDate);

        Log.d(ACT_TAG, "setLicenseVerifiedLocal: key=" + licenseKey + " endDate=" + endDate);
    }

    // ---------- Login flow ----------
    private void attemptLogin() {
        if (!isLicenseVerified()) {
            showLicenseDialog(false, null);
            return;
        }

        usernameLayout.setError(null);
        passwordLayout.setError(null);

        String username = usernameInput.getText() == null ? "" : usernameInput.getText().toString().trim();
        String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError("Username is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            return;
        }

        loginBtn.setEnabled(false);

        String savedLicenseKey = prefs.getString(KEY_LICENSE_KEY, "").trim();
        LoginRequest loginReq = new LoginRequest(savedLicenseKey, username, password);

        ApiService apiService = ApiClient.getPublicClient().create(ApiService.class);
        apiService.loginUser(loginReq).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                loginBtn.setEnabled(true);
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse loginResponse = response.body();
                        handleSuccessfulLogin(loginResponse, username, password);
                        return;
                    }

                    // handle error bodies
                    if (response.errorBody() != null) {
                        String raw = response.errorBody().string();
                        Log.d(ACT_TAG, "Login API errorBody: " + raw);

                        if (response.code() == 404 && raw.contains("Invalid license key")) {
                            showLicenseDialog(false, null);
                            return;
                        }

                        if (raw.contains("Invalid license key") || raw.contains("License is inactive") || raw.contains("License expired") || raw.toLowerCase().contains("license")) {
                            try {
                                JSONObject jo = new JSONObject(raw);
                                String msg = jo.has("message") ? jo.getString("message") : raw;
                                passwordLayout.setError(msg);
                            } catch (Exception ex) {
                                passwordLayout.setError(raw);
                            }
                            return;
                        }

                        try {
                            LoginResponse lr = new Gson().fromJson(raw, LoginResponse.class);
                            if (lr != null) {
                                if (lr.isSuccess()) {
                                    handleSuccessfulLogin(lr, username, password);
                                } else {
                                    passwordLayout.setError(lr.getMessage() != null ? lr.getMessage() : "Login failed");
                                }
                                return;
                            }
                        } catch (Exception ignored) {
                        }

                        passwordLayout.setError("Login failed. Code: " + response.code());
                        return;
                    }

                    passwordLayout.setError("Server error: " + response.message());
                } catch (Exception e) {
                    loginBtn.setEnabled(true);
                    passwordLayout.setError("Response parse error: " + e.getMessage());
                    Log.e(ACT_TAG, "Exception parsing login response: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                loginBtn.setEnabled(true);
                passwordLayout.setError("Network error: " + t.getMessage());
                Log.e(ACT_TAG, "Network error during login: " + t.getMessage());
            }
        });
    }

    private void handleSuccessfulLogin(LoginResponse loginResponse, String username, String password) {
        if (loginResponse == null) return;

        if (!loginResponse.isSuccess()) {
            passwordLayout.setError(loginResponse.getMessage() != null ? loginResponse.getMessage() : "Login failed");
            return;
        }

        // Save token
        String token = loginResponse.getToken();
        if (token != null && !token.isEmpty()) ApiClient.setToken(LoginActivity.this, token);

        // Remember me
        if (rememberMe.isChecked()) {
            prefs.edit()
                    .putBoolean(KEY_REMEMBER, true)
                    .putString(KEY_SAVED_USER, username)
                    .putString(KEY_SAVED_PASS, password)
                    .apply();
        } else {
            prefs.edit().remove(KEY_REMEMBER).remove(KEY_SAVED_USER).remove(KEY_SAVED_PASS).apply();
        }

        // Save permissions
        PrefHelper.savePermissions(LoginActivity.this,
                loginResponse.getData() != null && loginResponse.getData().getPermissions() != null
                        ? loginResponse.getData().getPermissions()
                        : new HashMap<>());

        // Sync RFID power now (best-effort) so app scans use expected power
        try {
            syncRfidPowerAfterLogin();
        } catch (Exception e) {
            Log.w(ACT_TAG, "syncRfidPowerAfterLogin failed: " + e.getMessage());
        }

        // Update header info and navigate
        try {
            LoginResponse.LoginData.User user = loginResponse.getData().getUser();
            String userName = user != null ? user.getName() : "";
            List<String> wsList = user != null ? user.getWorkingStages() : null;
            String wsText = (wsList == null || wsList.isEmpty()) ? "-" : TextUtils.join(", ", wsList);

            PrefHelper.saveHeaderUser(LoginActivity.this, userName, wsText);

            // Force update drawer header if present
            NavigationView nav = findViewById(R.id.nav_view);
            if (nav != null) {
                View header = nav.getHeaderView(0);
                if (header != null) {
                    TextView tvUser = header.findViewById(R.id.tvUserName);
                    String savedUserName = PrefHelper.getHeaderUserName(LoginActivity.this);
                    String savedStage = PrefHelper.getHeaderWorkingStage(LoginActivity.this);
                    String combined = savedUserName + "\n" + savedStage;
                    tvUser.setText(combined);
                }
            }
        } catch (Exception ignored) {}

        Snackbar.make(findViewById(android.R.id.content), "Login successful!", Snackbar.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
        finish();
    }

    /**
     * Try to ensure device power matches saved preference. This is best-effort and won't block login.
     */
    private void syncRfidPowerAfterLogin() {
        if (mDevice == null) return;

        try {
            // Ensure a fresh open
            try { mDevice.close(); } catch (Exception ignored) {}
            if (!mDevice.open()) {
                Log.w(ACT_TAG, "syncRfidPowerAfterLogin: failed to open device");
                return;
            }

            int savedPower = PrefHelper.getRfidPower(this);
            int devicePower = mDevice.getPower();
            Log.d(ACT_TAG, "PowerCheck - device:" + devicePower + " saved:" + savedPower);

            if (devicePower != savedPower) {
                boolean setOk = false;
                try {
                    setOk = mDevice.setPower(savedPower);
                } catch (Throwable t) {
                    // fallback reflection
                    try {
                        java.lang.reflect.Method m = mDevice.getClass().getMethod("setPower", int.class);
                        m.invoke(mDevice, savedPower);
                        setOk = true;
                    } catch (Exception ex) {
                        Log.w(ACT_TAG, "reflection setPower failed: " + ex.getMessage());
                    }
                }

                int newPower = mDevice.getPower();
                if (setOk && newPower == savedPower) {
                    PrefHelper.saveRfidPower(this, savedPower);
                    Log.i(ACT_TAG, "RFID power synced to " + savedPower);
                } else {
                    PrefHelper.saveRfidPower(this, newPower);
                    Log.w(ACT_TAG, "Could not apply saved power - persisted actual:" + newPower);
                }
            } else {
                // keep prefs consistent
                PrefHelper.saveRfidPower(this, devicePower);
            }

        } catch (Exception e) {
            Log.w(ACT_TAG, "syncRfidPowerAfterLogin exception: " + e.getMessage());
        } finally {
            try { mDevice.close(); } catch (Exception ignored) {}
        }
    }

    // ---------- License dialog & network helpers ----------
    private void showLicenseDialog() { showLicenseDialog(false, null); }

    private void showLicenseDialog(boolean hideCreateButton, String prefillLicense) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter License Key");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_license, null);
        final TextInputEditText licenseInput = dialogView.findViewById(R.id.licenseInput);
        final ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        final Button createBtn = dialogView.findViewById(R.id.createLicenseBtn);

        progressBar.setVisibility(View.GONE);
        this.dialogLicenseInput = licenseInput;

        if (!TextUtils.isEmpty(prefillLicense)) {
            licenseInput.setText(prefillLicense);
            licenseInput.post(() -> {
                try { int len = licenseInput.getText() != null ? licenseInput.getText().length() : 0; if (len > 0) licenseInput.setSelection(0, len); } catch (Exception ignored) {}
            });
        } else {
            licenseInput.setText("");
        }

        if (createBtn != null) createBtn.setVisibility(hideCreateButton ? View.GONE : View.VISIBLE);

        builder.setView(dialogView).setCancelable(false).setPositiveButton("Verify", null)
                .setNegativeButton("Exit", (d, which) -> { d.dismiss(); finish(); });

        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positive.setOnClickListener(v -> {
            String license = licenseInput.getText() == null ? "" : licenseInput.getText().toString().trim();
            if (TextUtils.isEmpty(license)) { licenseInput.setError("License required"); return; }

            positive.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            String androidId = getAndroidId(this);
            verifyLicenseWithServer(androidId, license, (ok, message, data) -> runOnUiThread(() -> {
                positive.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                if (ok) {
                    String endDate = data != null && data.endDate != null ? data.endDate : "";
                    String licenseKey = data != null && data.licenseKey != null ? data.licenseKey : license;
                    setLicenseVerifiedLocal(licenseKey, endDate);
                    Snackbar.make(findViewById(android.R.id.content), "License verified", Snackbar.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    licenseInput.setError(message != null ? message : "Verification failed");
                }
            }));
        });

        if (createBtn != null) {
            createBtn.setOnClickListener(v -> {
                createBtn.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                String androidId = getAndroidId(this);
                java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.add(java.util.Calendar.YEAR, 1);
                String endDate = fmt.format(cal.getTime());

                createLicenseWithServer(androidId, endDate, (ok, message, data) -> runOnUiThread(() -> {
                    createBtn.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    if (ok) {
                        String createdKey = data != null ? data.licenseKey : null;
                        String text = createdKey != null ? "\nLicense: " + createdKey : "";
                        new AlertDialog.Builder(LoginActivity.this).setTitle("License Created").setMessage(text)
                                .setPositiveButton("Copy Key", (dd, ww) -> { if (createdKey != null) {
                                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                    android.content.ClipData clip = android.content.ClipData.newPlainText("license", createdKey);
                                    clipboard.setPrimaryClip(clip);
                                    Snackbar.make(findViewById(android.R.id.content), "License copied to clipboard", Snackbar.LENGTH_SHORT).show();
                                }}).setNegativeButton("OK", null).show();
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), message != null ? message : "Failed to create license", Snackbar.LENGTH_LONG).show();
                    }
                }));
            });
        }
    }

    // License callbacks and network helpers (unchanged but kept concise)
    private interface LicenseCallback { void onResult(boolean ok, String message, LicenseResponse.LicenseData data); }
    private interface CheckDeviceCallback { void onResult(boolean ok, String message, LicenseResponse.LicenseData data); }

    private void verifyLicenseWithServer(String deviceId, String licenseKey, LicenseCallback callback) {
        ApiService apiService = ApiClient.getPublicClient().create(ApiService.class);
        LicenseRequest req = new LicenseRequest(deviceId, licenseKey, "");
        apiService.verifyLicense(req).enqueue(new Callback<LicenseResponse>() {
            @Override public void onResponse(Call<LicenseResponse> call, Response<LicenseResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) { LicenseResponse res = response.body(); callback.onResult(res.isSuccess(), res.getMessage(), res.getData()); return; }
                    if (response.errorBody() != null) { String raw = response.errorBody().string(); try { LicenseResponse lr = new Gson().fromJson(raw, LicenseResponse.class); if (lr != null) { callback.onResult(lr.isSuccess(), lr.getMessage(), lr.getData()); return; } } catch (Exception ignored) {}
                        try { JSONObject jo = new JSONObject(raw); if (jo.has("message")) { callback.onResult(false, jo.getString("message"), null); return; } } catch (Exception ignored) {}
                        callback.onResult(false, raw, null); return; }
                    callback.onResult(false, "Server error: " + response.message(), null);
                } catch (Exception e) { callback.onResult(false, "Error parsing server response", null); }
            }
            @Override public void onFailure(Call<LicenseResponse> call, Throwable t) { callback.onResult(false, "Network error: " + t.getMessage(), null); }
        });
    }

    private void createLicenseWithServer(String deviceId, String endDate, LicenseCallback callback) {
        ApiService apiService = ApiClient.getPublicClient().create(ApiService.class);
        CreateLicenseRequest req = new CreateLicenseRequest(deviceId, endDate, "INACTIVE");
        apiService.createLicense(req).enqueue(new Callback<LicenseResponse>() {
            @Override public void onResponse(Call<LicenseResponse> call, Response<LicenseResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) { LicenseResponse res = response.body(); callback.onResult(res.isSuccess(), res.getMessage(), res.getData()); return; }
                    if (response.errorBody() != null) { String raw = response.errorBody().string(); try { LicenseResponse lr = new Gson().fromJson(raw, LicenseResponse.class); if (lr != null) { callback.onResult(lr.isSuccess(), lr.getMessage(), lr.getData()); return; } } catch (Exception ignored) {}
                        try { JSONObject jo = new JSONObject(raw); if (jo.has("message")) { callback.onResult(false, jo.getString("message"), null); return; } } catch (Exception ignored) {}
                        callback.onResult(false, raw, null); return; }
                    callback.onResult(false, "Server error: " + response.message(), null);
                } catch (Exception e) { callback.onResult(false, "Error parsing server response", null); }
            }
            @Override public void onFailure(Call<LicenseResponse> call, Throwable t) { callback.onResult(false, "Network error: " + t.getMessage(), null); }
        });
    }

    private void checkDeviceWithServer(String deviceId, CheckDeviceCallback callback) {
        ApiService apiService = ApiClient.getPublicClient().create(ApiService.class);
        DeviceCheckRequest req = new DeviceCheckRequest(deviceId);
        apiService.checkDevice(req).enqueue(new Callback<LicenseResponse>() {
            @Override public void onResponse(Call<LicenseResponse> call, Response<LicenseResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        LicenseResponse res = response.body();
                        LicenseResponse.LicenseData data = res.getData();
                        if (data == null) data = new LicenseResponse.LicenseData();
                        if ((data.status == null || data.status.isEmpty()) && res.getStatus() != null) data.status = res.getStatus();
                        callback.onResult(res.isSuccess(), res.getMessage(), data); return;
                    }
                    if (response.errorBody() != null) { String raw = response.errorBody().string(); try { LicenseResponse lr = new Gson().fromJson(raw, LicenseResponse.class); if (lr != null) { LicenseResponse.LicenseData data = lr.getData(); if (data == null) data = new LicenseResponse.LicenseData(); if ((data.status == null || data.status.isEmpty()) && lr.getStatus() != null) data.status = lr.getStatus(); callback.onResult(lr.isSuccess(), lr.getMessage(), data); return; } } catch (Exception ignored) {}
                        callback.onResult(false, "No license found", null); return; }
                    callback.onResult(false, "Server error: " + response.message(), null);
                } catch (Exception e) { callback.onResult(false, "Error parsing server response", null); }
            }
            @Override public void onFailure(Call<LicenseResponse> call, Throwable t) { callback.onResult(false, "Network error: " + t.getMessage(), null); }
        });
    }

    // ---------- Utility ----------
    public static String getAndroidId(android.content.Context ctx) {
        try { String id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID); return id != null ? id : ""; } catch (Exception e) { Log.w(ACT_TAG, "Couldn't read ANDROID_ID: " + e.getMessage()); return ""; }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDevice != null) {
            try { mDevice.open(); } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDevice != null) {
            try { mDevice.close(); } catch (Exception ignored) {}
        }
    }
}
