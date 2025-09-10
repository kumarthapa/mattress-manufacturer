package com.sleepcompany.rfidapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sleepcompany.rfidapp.network.ApiClient;
import com.sleepcompany.rfidapp.network.ApiService;
import com.sleepcompany.rfidapp.network.LoginRequest;
import com.sleepcompany.rfidapp.network.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usernameInput, passwordInput;
    private TextInputLayout usernameLayout, passwordLayout;
    private MaterialCheckBox rememberMe;
    private MaterialButton loginBtn;

    // SharedPreferences keys
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_REMEMBER = "remember_me";
    private static final String KEY_SAVED_USER = "saved_user";
    private static final String KEY_SAVED_PASS = "saved_pass";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Bind views
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        rememberMe = findViewById(R.id.rememberMe);
        loginBtn = findViewById(R.id.loginBtn);

        // Restore saved credentials if Remember Me was checked
        if (prefs.getBoolean(KEY_REMEMBER, false)) {
            usernameInput.setText(prefs.getString(KEY_SAVED_USER, ""));
            passwordInput.setText(prefs.getString(KEY_SAVED_PASS, ""));
            rememberMe.setChecked(true);
        }

        loginBtn.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
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

        loginBtn.setEnabled(false); // disable button during API call

        LoginRequest loginRequest = new LoginRequest(username, password);
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class); // ✅ Pass context

        apiService.loginUser(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                loginBtn.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess()) {
                        String authToken = loginResponse.getToken();

                        // 🔑 Save token via ApiClient helper
                        ApiClient.setToken(LoginActivity.this, authToken);

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

                        Snackbar.make(findViewById(android.R.id.content),
                                "Login successful!", Snackbar.LENGTH_SHORT).show();

                        // Navigate to dashboard
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        finish();
                    } else {
                        passwordLayout.setError(loginResponse.getMessage());
                    }
                } else {
                    Log.d(TAG, "Login failed: Login API not responding.");
                    passwordLayout.setError("Login failed please try again later. " + response.message());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                loginBtn.setEnabled(true);
                passwordLayout.setError("Network error: " + t.getMessage());
            }
        });
    }
}
