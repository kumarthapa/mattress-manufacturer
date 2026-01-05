package com.galla.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText usernameInput, passwordInput;
    private TextInputLayout usernameLayout, passwordLayout;
    private CheckBox rememberMe;
    private MaterialButton loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Use Material Design components (matching the XML)
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        rememberMe = findViewById(R.id.rememberMe);
        loginBtn = findViewById(R.id.loginBtn);

        loginBtn.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        // Clear previous errors (Material Design way)
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
            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, DashboardActivity.class));
            finish();
        } else {
            passwordLayout.setError("Invalid username or password");
        }
    }
}
