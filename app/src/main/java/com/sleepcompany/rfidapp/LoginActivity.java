package com.sleepcompany.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

// Import Material Design components
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usernameInput, passwordInput;
    private TextInputLayout usernameLayout, passwordLayout;
    private CheckBox rememberMe;
    private MaterialButton loginBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Test Material UI availability
        testMaterialUI();

        // Initialize Material Design components
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        rememberMe = findViewById(R.id.rememberMe);
        loginBtn = findViewById(R.id.loginBtn);

        // Set click listener
        loginBtn.setOnClickListener(v -> attemptLogin());
    }

    private void testMaterialUI() {
        try {
            // Test Material UI components
            MaterialButton testButton = new MaterialButton(this);
            TextInputLayout testLayout = new TextInputLayout(this);

            Log.d("MATERIAL_TEST", "✅ Material UI is working!");
            Log.d("MATERIAL_TEST", "MaterialButton: " + testButton.getClass().getName());
            Log.d("MATERIAL_TEST", "TextInputLayout: " + testLayout.getClass().getName());

        } catch (Exception e) {
            Log.e("MATERIAL_TEST", "❌ Material UI failed: " + e.getMessage());
        }
    }

    private void attemptLogin() {
        // Clear previous errors
        usernameLayout.setError(null);
        passwordLayout.setError(null);

        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        boolean cancel = false;

        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError("Username is required");
            cancel = true;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            cancel = true;
        }

        if (cancel) return;

        if (username.equals("admin") && password.equals("admin123")) {
            // Use Material Snackbar instead of Toast
            Snackbar.make(findViewById(android.R.id.content),
                    "Login successful!", Snackbar.LENGTH_SHORT).show();

            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        } else {
            passwordLayout.setError("Invalid username or password");
        }
    }
}
