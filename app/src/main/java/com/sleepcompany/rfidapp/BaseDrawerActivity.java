// BaseDrawerActivity.java
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

    // Holds DataBinding reference if used
    protected ViewDataBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int layoutId = getLayoutResourceId();
        if (layoutId != 0) {
            if (useDataBinding()) {
                // Use DataBinding
                binding = DataBindingUtil.setContentView(this, layoutId);
            } else {
                // Normal setContentView
                setContentView(layoutId);
            }
        }

        // Initialize drawer components (if they exist in layout)
        drawerLayout   = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar        = findViewById(R.id.toolbar);

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

        // Set toolbar title from child activity
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getToolbarTitle());
        }
    }

    /**
     * Child activities must return their layout resource.
     * Return 0 if you want to handle layout inflation manually.
     */
    @LayoutRes
    protected abstract int getLayoutResourceId();

    /**
     * Override this in child activity if you want DataBinding.
     */
    protected boolean useDataBinding() {
        return false;
    }

    /**
     * Override to set custom toolbar title in child activities.
     */
    protected String getToolbarTitle() {
        return getString(R.string.app_name);
    }

    /**
     * Utility method to navigate to another activity safely.
     */
    protected void navigateTo(Class<?> activityClass) {
        if (!activityClass.isInstance(this)) {
            Intent intent = new Intent(this, activityClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            navigateTo(DashboardActivity.class);

        } else if (id == R.id.nav_production) {
            // TODO: Implement ProductionOverviewActivity
            // navigateTo(ProductionOverviewActivity.class);

        } else if (id == R.id.nav_quality_control) {
            navigateTo(QcActivity.class);

        } else if (id == R.id.nav_rfid_scan) {
            navigateTo(ScannerActivity.class);

        } else if (id == R.id.nav_products) {
            navigateTo(ProductsActivity.class);

        } else if (id == R.id.nav_defects) {
            // TODO: Implement DefectTrackingActivity
            // navigateTo(DefectTrackingActivity.class);

        } else if (id == R.id.nav_settings) {
            // TODO: Implement SettingsActivity
            // navigateTo(SettingsActivity.class);

        } else if (id == R.id.nav_logout) {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
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
