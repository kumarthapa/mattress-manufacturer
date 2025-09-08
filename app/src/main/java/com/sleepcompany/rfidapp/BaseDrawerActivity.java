// BaseDrawerActivity.java
package com.sleepcompany.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;


public abstract class BaseDrawerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        drawerLayout   = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar        = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
    }

    protected abstract int getLayoutResourceId();

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            if (!(this instanceof DashboardActivity)) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
            }
        } else if (id == R.id.nav_production) {
            // TODO: Navigate to ProductionOverviewActivity when implemented
        } else if (id == R.id.nav_quality_control) {
            if (!(this instanceof QcActivity)) {
                startActivity(new Intent(this, QcActivity.class));
                finish();
            }
        } else if (id == R.id.nav_rfid_scan) {
            if (!(this instanceof ScannerActivity)) {
                startActivity(new Intent(this, ScannerActivity.class));
                finish();
            }
        } else if (id == R.id.nav_products) {
            if (!(this instanceof ProductsActivity)) {
                startActivity(new Intent(this, ProductsActivity.class));
                finish();
            }
        } else if (id == R.id.nav_defects) {
            // TODO: Navigate to DefectTrackingActivity when implemented
        } else if (id == R.id.nav_settings) {
            // TODO: Navigate to SettingsActivity when implemented
        } else if (id == R.id.nav_logout) {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
