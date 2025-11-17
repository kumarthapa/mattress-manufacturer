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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import com.sleepcompany.rfidapp.util.PrefHelper;

import org.json.JSONObject;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

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

        // bind UI
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        rememberMe = findViewById(R.id.rememberMe);
        loginBtn = findViewById(R.id.loginBtn);

        // Restore saved credentials if "remember me" selected
        if (prefs.getBoolean(KEY_REMEMBER, false)) {
            usernameInput.setText(prefs.getString(KEY_SAVED_USER, ""));
            passwordInput.setText(prefs.getString(KEY_SAVED_PASS, ""));
            rememberMe.setChecked(true);
            Log.d(TAG, "Restored remembered credentials.");
        }

        // ---------- Device license check flow ----------
        // If already verified locally (persisted), skip server check (covers APK updates)
        if (isLicenseVerified()) {
            Log.d(TAG, "License verified locally, skipping server check.");
        } else {
            // Try reading Android ID and consult server
            String androidId = getAndroidId(this);
            if (TextUtils.isEmpty(androidId)) {
                // Cannot read Android ID -> fallback to showing dialog (allow create)
                Log.w(TAG, "ANDROID_ID not available; falling back to show license dialog (create allowed).");
                showLicenseDialog(false, null);
            } else {
                Log.d(TAG, "Checking device license on server for Android ID: " + androidId);
                checkDeviceWithServer(androidId, (ok, message, data) -> runOnUiThread(() -> {
                    Log.d(TAG, "checkDevice result: ok=" + ok + " message=" + message + " data=" + (data != null ? ("licenseKey=" + data.licenseKey + " status=" + data.status) : "null"));
                    if (ok && data != null) {
                        // Normalize status from server (data.status should have been merged in checkDeviceWithServer)
                        String status = data.status != null ? data.status.toUpperCase() : "UNKNOWN";
                        Log.d(TAG, "Device license status (normalized): " + status);

                        if ("ACTIVE".equals(status)) {
                            // Device already has active license: store locally and allow login
                            String licenseKey = data.licenseKey != null ? data.licenseKey : prefs.getString(KEY_LICENSE_KEY, "");
                            String endDate = data.endDate != null ? data.endDate : prefs.getString(KEY_LICENSE_END_DATE, "");
                            setLicenseVerifiedLocal(licenseKey, endDate);
                            Log.i(TAG, "Device has ACTIVE license. Local prefs updated.");
                        } else if ("INACTIVE".equals(status) || "EXPIRE".equals(status)) {
                            // Device has license but it's inactive/expired: show dialog, pre-fill license and hide create
                            Log.i(TAG, "Device license is INACTIVE/EXPIRE – showing dialog with prefilled key and hiding Create.");
                            showLicenseDialog(true, data.licenseKey);
                        } else {
                            // Unknown status — allow user to create a license
                            Log.w(TAG, "Device license status unknown — allowing create.");
                            showLicenseDialog(false, null);
                        }
                    } else {
                        // No license found or server indicated error -> allow create
                        Log.i(TAG, "No license found for device or server returned not-ok. Allowing create.");
                        showLicenseDialog(false, null);
                    }
                }));
            }
        }

        // Hook login button
        loginBtn.setOnClickListener(v -> attemptLogin());
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

        // Keep PrefHelper consistent (your utility)
        PrefHelper.setLicenseVerified(this, true);
        PrefHelper.saveLicenseInfo(this, licenseKey, endDate);

        Log.d(TAG, "setLicenseVerifiedLocal: key=" + licenseKey + " endDate=" + endDate);
    }

    // ---------- Login flow ----------
    private void attemptLogin() {
        if (!isLicenseVerified()) {
            // Defensive: ensure verified before login
            Log.w(TAG, "Attempted login without verified license. Showing license dialog.");
            showLicenseDialog(false, null);
            return;
        }

        // clear previous errors
        usernameLayout.setError(null);
        passwordLayout.setError(null);

        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError("Username is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            return;
        }

        loginBtn.setEnabled(false);

        // get saved license key
        String savedLicenseKey = prefs.getString(KEY_LICENSE_KEY, "").trim();

        // build login request (server expects license_key, username, password)
        LoginRequest loginReq = new LoginRequest(savedLicenseKey, username, password);

        // Use PUBLIC client for login (no Authorization header)
        ApiService apiService = ApiClient.getPublicClient().create(ApiService.class);
        apiService.loginUser(loginReq).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                loginBtn.setEnabled(true);
                LoginResponse loginResponse = null;

                try {
                    // Successful 2xx response
                    if (response.isSuccessful() && response.body() != null) {
                        loginResponse = response.body();
                    }
                    // Non-2xx response: read error body ONCE and handle
                    else if (response.errorBody() != null) {
                        String raw = response.errorBody().string();
                        Log.d(TAG, "Login API errorBody: " + raw);

                        // 1) License missing (server 404 & specific message)
                        if (response.code() == 404 && raw.contains("Invalid license key")) {
                            showLicenseDialog(false, null);
                            return;
                        }

                        // 2) Common license messages from server
                        if (raw.contains("Invalid license key") ||
                                raw.contains("License is inactive") ||
                                raw.contains("License expired") ||
                                raw.toLowerCase().contains("license")) {

                            // display server message if present
                            try {
                                JSONObject jo = new JSONObject(raw);
                                String msg = jo.has("message") ? jo.getString("message") : raw;
                                passwordLayout.setError(msg);
                                Log.w(TAG, "License/login error: " + msg);
                            } catch (Exception ex) {
                                passwordLayout.setError(raw);
                                Log.w(TAG, "License/login error (raw): " + raw);
                            }
                            return;
                        }

                        // 3) Try to parse LoginResponse from error body (some APIs return the full object with success=false)
                        try {
                            loginResponse = new Gson().fromJson(raw, LoginResponse.class);
                        } catch (Exception ex) {
                            // fallback: try extract "message" field
                            try {
                                JSONObject jo = new JSONObject(raw);
                                if (jo.has("message")) {
                                    String msg = jo.getString("message");
                                    passwordLayout.setError(msg);
                                    Log.w(TAG, "Login error from server: " + msg);
                                    return;
                                }
                            } catch (Exception ignored) { }
                            passwordLayout.setError("Login failed. Code: " + response.code());
                            Log.e(TAG, "Login API error, unparseable body");
                            return;
                        }
                    } else {
                        // No body at all
                        passwordLayout.setError("Server error: " + response.message());
                        Log.e(TAG, "Login server error: " + response.message());
                        return;
                    }
                } catch (Exception e) {
                    loginBtn.setEnabled(true);
                    passwordLayout.setError("Response parse error: " + e.getMessage());
                    Log.e(TAG, "Exception parsing login response: " + e.getMessage());
                    return;
                }

                // If we reach here, loginResponse may be populated (from success or parsed error)
                if (loginResponse != null) {
                    if (loginResponse.isSuccess()) {
                        // Save token using ApiClient helper
                        String token = loginResponse.getToken();
                        if (token != null && !token.isEmpty()) {
                            ApiClient.setToken(LoginActivity.this, token);
                        }

                        if (rememberMe.isChecked()) {
                            prefs.edit()
                                    .putBoolean(KEY_REMEMBER, true)
                                    .putString(KEY_SAVED_USER, username)
                                    .putString(KEY_SAVED_PASS, password)
                                    .apply();
                        } else {
                            prefs.edit()
                                    .remove(KEY_REMEMBER)
                                    .remove(KEY_SAVED_USER)
                                    .remove(KEY_SAVED_PASS)
                                    .apply();
                        }

                        PrefHelper.savePermissions(LoginActivity.this,
                                loginResponse.getData() != null && loginResponse.getData().getPermissions() != null
                                        ? loginResponse.getData().getPermissions()
                                        : new HashMap<>());

                        Snackbar.make(findViewById(android.R.id.content),
                                "Login successful!", Snackbar.LENGTH_SHORT).show();

                        Log.i(TAG, "Login successful for user: " + username);
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        finish();
                    } else {
                        passwordLayout.setError(loginResponse.getMessage() != null
                                ? loginResponse.getMessage()
                                : "Login failed");
                        Log.w(TAG, "Login failed: " + loginResponse.getMessage());
                    }
                } else {
                    passwordLayout.setError("Unexpected server response");
                    Log.e(TAG, "Login: loginResponse null after parsing");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                loginBtn.setEnabled(true);
                passwordLayout.setError("Network error: " + t.getMessage());
                Log.e(TAG, "Network error during login: " + t.getMessage());
            }
        });
    }


    // ---------- License dialog helpers ----------
    // Backwards-compatible no-arg method
    private void showLicenseDialog() {
        showLicenseDialog(false, null);
    }

    /**
     * Show license dialog.
     *
     * @param hideCreateButton if true, hide the "Create License" button (use when server reports INACTIVE/EXPIRE)
     * @param prefillLicense   optional license key to prefill the input (can be null)
     */
    private void showLicenseDialog(boolean hideCreateButton, String prefillLicense) {
        Log.d(TAG, "showLicenseDialog called. hideCreate=" + hideCreateButton + " prefill=" + (prefillLicense != null));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter License Key");

        // Inflate dialog layout (dialog_license.xml)
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_license, null);
        final TextInputEditText licenseInput = dialogView.findViewById(R.id.licenseInput);
        final ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        final Button createBtn = dialogView.findViewById(R.id.createLicenseBtn); // this button is inside dialog layout

        progressBar.setVisibility(View.GONE);
        this.dialogLicenseInput = licenseInput;

        // Prefill license input if provided and select the text so user can copy quickly
        if (!TextUtils.isEmpty(prefillLicense)) {
            licenseInput.setText(prefillLicense);
            // post selection to ensure view is ready
            licenseInput.post(() -> {
                try {
                    int len = licenseInput.getText() != null ? licenseInput.getText().length() : 0;
                    if (len > 0) {
                        // select whole text (helpful for copy)
                        licenseInput.setSelection(0, len);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not select prefilled license text: " + e.getMessage());
                }
            });
            Log.d(TAG, "Prefilled license input with value: " + prefillLicense);
        } else {
            licenseInput.setText("");
        }

        // Apply visibility to create button before showing dialog
        if (createBtn != null) {
            createBtn.setVisibility(hideCreateButton ? View.GONE : View.VISIBLE);
            Log.d(TAG, "Create button visibility set to: " + (hideCreateButton ? "GONE" : "VISIBLE"));
        } else {
            Log.w(TAG, "Create button not found in dialog view.");
        }

        builder.setView(dialogView);
        builder.setCancelable(false);
        builder.setPositiveButton("Verify", null);
        builder.setNegativeButton("Exit", (d, which) -> {
            d.dismiss();
            finish();
        });

        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        // VERIFY button behavior
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positive.setOnClickListener(v -> {
            String license = licenseInput.getText() != null ? licenseInput.getText().toString().trim() : "";

            if (TextUtils.isEmpty(license)) {
                licenseInput.setError("License required");
                return;
            }

            positive.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            licenseInput.setError(null);

            // send Android ID as device_id
            String androidId = getAndroidId(this);

            Log.d(TAG, "User triggered license verify. deviceId=" + androidId + " license=" + license);

            // Use public client (no Authorization) to verify license
            verifyLicenseWithServer(androidId, license, (ok, message, data) -> runOnUiThread(() -> {
                positive.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                if (ok) {
                    String endDate = (data != null && data.endDate != null) ? data.endDate : "";
                    String licenseKey = (data != null && data.licenseKey != null) ? data.licenseKey : license;
                    setLicenseVerifiedLocal(licenseKey, endDate);

                    Snackbar.make(findViewById(android.R.id.content), "License verified", Snackbar.LENGTH_SHORT).show();
                    Log.i(TAG, "License verified by server. key=" + licenseKey + " endDate=" + endDate);
                    dialog.dismiss();
                } else {
                    licenseInput.setError(message != null ? message : "Verification failed");
                    Log.w(TAG, "License verification failed: " + message);
                }
            }));
        });

        // CREATE LICENSE button (click handler; may be hidden by hideCreateButton)
        if (createBtn != null) {
            createBtn.setOnClickListener(v -> {
                // disable UI while creating
                createBtn.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);

                String androidId = getAndroidId(this);
                // compute endDate = today + 1 year (format yyyy-MM-dd)
                java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.add(java.util.Calendar.YEAR, 1);
                String endDate = fmt.format(cal.getTime());

                Log.d(TAG, "Creating license on server. deviceId=" + androidId + " endDate=" + endDate);

                createLicenseWithServer(androidId, endDate, (ok, message, data) -> runOnUiThread(() -> {
                    createBtn.setEnabled(true);
                    progressBar.setVisibility(View.GONE);

                    if (ok) {
                        // If server returns license info, show it to user and let them copy
                        String createdKey = (data != null && data.licenseKey != null) ? data.licenseKey : null;
                        String createdEnd = (data != null && data.endDate != null) ? data.endDate : endDate;
                        String text = "";
                        if (createdKey != null) text += "\nLicense: " + createdKey;


                        Log.i(TAG, "License created on server. key=" + createdKey + " endDate=" + createdEnd);

                        // show dialog with created license and copy option
                        new AlertDialog.Builder(LoginActivity.this)
                                .setTitle("License Created")
                                .setMessage(text)
                                .setPositiveButton("Copy Key", (dd, ww) -> {
                                    if (createdKey != null) {
                                        android.content.ClipboardManager clipboard =
                                                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                        android.content.ClipData clip = android.content.ClipData.newPlainText("license", createdKey);
                                        clipboard.setPrimaryClip(clip);
                                        Snackbar.make(findViewById(android.R.id.content), "License copied to clipboard", Snackbar.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("OK", null)
                                .show();
                        // NOTE: don't auto-verify — provider will activate later manually
                    } else {
                        // show error to user
                        String err = message != null ? message : "Failed to create license";
                        Snackbar.make(findViewById(android.R.id.content), err, Snackbar.LENGTH_LONG).show();
                        Log.w(TAG, "createLicense failed: " + message);
                    }
                }));
            });
        }
    }

    // ---------- Callbacks / network helpers ----------

    // License verification callback interface
    private interface LicenseCallback {
        void onResult(boolean ok, String message, LicenseResponse.LicenseData data);
    }

    // Check device callback (used by checkDeviceWithServer)
    private interface CheckDeviceCallback {
        void onResult(boolean ok, String message, LicenseResponse.LicenseData data);
    }

    /**
     * Verify license with server (POST /device/verify-license)
     */
    private void verifyLicenseWithServer(String deviceId, String licenseKey, LicenseCallback callback) {
        ApiService apiService = ApiClient.getPublicClient().create(ApiService.class);
        LicenseRequest req = new LicenseRequest(deviceId, licenseKey, "");

        Log.d(TAG, "Calling verifyLicense API for device: " + deviceId + " license: " + licenseKey);

        apiService.verifyLicense(req).enqueue(new Callback<LicenseResponse>() {
            @Override
            public void onResponse(Call<LicenseResponse> call, Response<LicenseResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        LicenseResponse res = response.body();
                        Log.d(TAG, "verifyLicense response: success=" + res.isSuccess() + " message=" + res.getMessage());
                        if (res.isSuccess()) {
                            callback.onResult(true, res.getMessage(), res.getData());
                        } else {
                            callback.onResult(false, res.getMessage() != null ? res.getMessage() : "License invalid", null);
                        }
                        return;
                    }

                    if (response.errorBody() != null) {
                        String raw = response.errorBody().string();
                        Log.w(TAG, "verifyLicense errorBody: " + raw);
                        // try parse LicenseResponse
                        try {
                            LicenseResponse lr = new Gson().fromJson(raw, LicenseResponse.class);
                            if (lr != null && lr.getMessage() != null) {
                                callback.onResult(lr.isSuccess(), lr.getMessage(), lr.getData());
                                return;
                            }
                        } catch (Exception ignored) { }

                        // try extract "message" from JSON
                        try {
                            JSONObject jo = new JSONObject(raw);
                            if (jo.has("message")) {
                                callback.onResult(false, jo.getString("message"), null);
                                return;
                            }
                        } catch (Exception ignored) { }

                        callback.onResult(false, raw, null);
                        return;
                    }

                    callback.onResult(false, "Server error: " + response.message(), null);
                } catch (Exception e) {
                    Log.e(TAG, "Exception parsing license response: " + e.getMessage());
                    callback.onResult(false, "Error parsing server response", null);
                }
            }

            @Override
            public void onFailure(Call<LicenseResponse> call, Throwable t) {
                Log.e(TAG, "Network error verifying license: " + t.getMessage());
                callback.onResult(false, "Network error: " + t.getMessage(), null);
            }
        });
    }

    /**
     * Create a new license on server (POST /device/create-license)
     */
    private void createLicenseWithServer(String deviceId, String endDate, LicenseCallback callback) {
        ApiService apiService = ApiClient.getPublicClient().create(ApiService.class);

        CreateLicenseRequest req = new CreateLicenseRequest(deviceId, endDate, "INACTIVE");

        Log.d(TAG, "Calling createLicense API for device: " + deviceId + " end=" + endDate);

        apiService.createLicense(req).enqueue(new Callback<LicenseResponse>() {
            @Override
            public void onResponse(Call<LicenseResponse> call, Response<LicenseResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        LicenseResponse res = response.body();
                        Log.d(TAG, "createLicense response: success=" + res.isSuccess() + " message=" + res.getMessage());
                        if (res.isSuccess()) {
                            callback.onResult(true, res.getMessage(), res.getData());
                        } else {
                            callback.onResult(false, res.getMessage() != null ? res.getMessage() : "Create failed", null);
                        }
                        return;
                    }

                    if (response.errorBody() != null) {
                        String raw = response.errorBody().string();
                        Log.w(TAG, "createLicense errorBody: " + raw);
                        try {
                            LicenseResponse lr = new Gson().fromJson(raw, LicenseResponse.class);
                            if (lr != null && lr.getMessage() != null) {
                                callback.onResult(lr.isSuccess(), lr.getMessage(), lr.getData());
                                return;
                            }
                        } catch (Exception ignored) {}

                        try {
                            JSONObject jo = new JSONObject(raw);
                            if (jo.has("message")) {
                                callback.onResult(false, jo.getString("message"), null);
                                return;
                            }
                        } catch (Exception ignored) {}

                        callback.onResult(false, raw, null);
                        return;
                    }

                    callback.onResult(false, "Server error: " + response.message(), null);
                } catch (Exception e) {
                    Log.e(TAG, "Exception parsing create license response: " + e.getMessage());
                    callback.onResult(false, "Error parsing server response", null);
                }
            }

            @Override
            public void onFailure(Call<LicenseResponse> call, Throwable t) {
                Log.e(TAG, "Network error creating license: " + t.getMessage());
                callback.onResult(false, "Network error: " + t.getMessage(), null);
            }
        });
    }

    /**
     * Check device license on server (POST /device/check)
     * Merges top-level status into data.status if server uses top-level status field.
     */
    private void checkDeviceWithServer(String deviceId, CheckDeviceCallback callback) {
        ApiService apiService = ApiClient.getPublicClient().create(ApiService.class);
        DeviceCheckRequest req = new DeviceCheckRequest(deviceId);

        Log.d(TAG, "Calling checkDevice API for device: " + deviceId);

        apiService.checkDevice(req).enqueue(new Callback<LicenseResponse>() {
            @Override
            public void onResponse(Call<LicenseResponse> call, Response<LicenseResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        LicenseResponse res = response.body();

                        // Ensure LicenseData exists and contains the status (merge top-level status if needed)
                        LicenseResponse.LicenseData data = res.getData();
                        if (data == null) {
                            data = new LicenseResponse.LicenseData();
                        }
                        // If data.status is null but server returned top-level status, copy it in
                        if ((data.status == null || data.status.isEmpty()) && res.getStatus() != null) {
                            data.status = res.getStatus();
                        }

                        Log.d(TAG, "checkDevice success. status=" + data.status + " license=" + data.licenseKey);
                        callback.onResult(res.isSuccess(), res.getMessage(), data);
                        return;
                    }

                    // If server returned non-2xx with JSON body, try to parse a LicenseResponse
                    if (response.errorBody() != null) {
                        String raw = response.errorBody().string();
                        Log.w(TAG, "checkDevice errorBody: " + raw);
                        try {
                            LicenseResponse lr = new Gson().fromJson(raw, LicenseResponse.class);
                            if (lr != null) {
                                LicenseResponse.LicenseData data = lr.getData();
                                if (data == null) data = new LicenseResponse.LicenseData();
                                if ((data.status == null || data.status.isEmpty()) && lr.getStatus() != null) {
                                    data.status = lr.getStatus();
                                }
                                callback.onResult(lr.isSuccess(), lr.getMessage(), data);
                                return;
                            }
                        } catch (Exception ignored) { }

                        callback.onResult(false, "No license found", null);
                        return;
                    }

                    callback.onResult(false, "Server error: " + response.message(), null);
                } catch (Exception e) {
                    Log.e(TAG, "Exception parsing checkDevice response: " + e.getMessage());
                    callback.onResult(false, "Error parsing server response", null);
                }
            }

            @Override
            public void onFailure(Call<LicenseResponse> call, Throwable t) {
                Log.e(TAG, "Network error checking device: " + t.getMessage());
                callback.onResult(false, "Network error: " + t.getMessage(), null);
            }
        });
    }

    // ---------- Utility ----------
    public static String getAndroidId(android.content.Context ctx) {
        try {
            String id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
            return id != null ? id : "";
        } catch (Exception e) {
            Log.w(TAG, "Couldn't read ANDROID_ID: " + e.getMessage());
            return "";
        }
    }
}
