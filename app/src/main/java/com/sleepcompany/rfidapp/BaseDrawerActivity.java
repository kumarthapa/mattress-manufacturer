package com.sleepcompany.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public abstract class BaseDrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;
    protected ViewDataBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int layoutId = getLayoutResourceId();
        if (layoutId != 0) {
            if (useDataBinding()) {
                binding = DataBindingUtil.setContentView(this, layoutId);
            } else {
                setContentView(layoutId);
            }
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        setupToolbarAndDrawer();
        setupNavigationDrawer();
    }

    private void setupToolbarAndDrawer() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (drawerLayout != null) {
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, drawerLayout, toolbar,
                        R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close
                );
                drawerLayout.addDrawerListener(toggle);
                toggle.syncState();
            }
        }

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getToolbarTitle());
        }
    }

    protected void setupNavigationDrawer() {
        // default implementation: nothing extra
        // child activities can override if needed
    }

    @LayoutRes
    protected abstract int getLayoutResourceId();

    protected boolean useDataBinding() {
        return false;
    }

    protected String getToolbarTitle() {
        return getString(R.string.app_name);
    }

    protected void navigateTo(Class<?> activityClass) {
        if (!activityClass.isInstance(this)) {
            Intent intent = new Intent(this, activityClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Allow child activities to handle custom items first
        if (handleNavigationItem(item)) return true;

        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            navigateTo(DashboardActivity.class);
        } else if (id == R.id.nav_products) {
            navigateTo(ProductsActivity.class);
        } else if (id == R.id.nav_quality_control) {
            navigateTo(QcActivity.class);
        } else if (id == R.id.nav_rfid_scan) {
            navigateTo(ScannerActivity.class);
        } else if (id == R.id.nav_defects) {
            // TODO: navigateTo(DefectTrackingActivity.class);
        } else if (id == R.id.nav_settings) {
            // TODO: navigateTo(SettingsActivity.class);
        } else if (id == R.id.nav_logout) {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


    protected boolean handleNavigationItem(@NonNull MenuItem item) {
        return false; // default: not handled
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
