package com.sleepcompany.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

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
import com.sleepcompany.rfidapp.util.PrefHelper;
import com.sleepcompany.rfidapp.util.SessionManager;

public abstract class BaseDrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "BaseDrawerActivity";

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
        if (navigationView != null) {
            // get menu and write item
            Menu menu = navigationView.getMenu();
            MenuItem writeItem = menu.findItem(R.id.nav_write_tags);

            boolean canWriteBonding = PrefHelper.hasPermission(this, "write.bonding");
            if (writeItem != null) {
                writeItem.setVisible(canWriteBonding);
            }
        }
    }


    @LayoutRes
    protected abstract int getLayoutResourceId();

    protected boolean useDataBinding() {
        return false;
    }

    protected String getToolbarTitle() {
        return "Dashboard";
    }

    protected void navigateTo(Class<?> activityClass) {
        if (!activityClass.isInstance(this)) {
            Intent intent = new Intent(this, activityClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
        }
//        else if (id == R.id.nav_quality_control) {
//        navigateTo(QcActivity.class);}
        else if (id == R.id.nav_rfid_scan) {
            navigateTo(ScannerActivity.class);
        } else if (id == R.id.nav_write_tags) {
            navigateTo(WriteTagsActivity.class);
        }
//        else if (id == R.id.nav_defects) {
//            // TODO: navigateTo(DefectTrackingActivity.class);}
        else if (id == R.id.nav_settings) {

            navigateTo(SettingsActivity.class);
        } else if (id == R.id.nav_logout) {
            // DEBUG DUMP (optional) - uncomment if you want to log prefs before/after:
            // Log.d(TAG, "PREFS BEFORE LOGOUT:\n" + PrefHelper.dumpAllPrefs(this));

            // Perform safe logout (clears token + session keys but keeps printer & credentials)
            SessionManager.logoutKeepPreferences(this);

            // DEBUG DUMP (optional) - uncomment if you want to log prefs after logout:
            // Log.d(TAG, "PREFS AFTER LOGOUT:\n" + PrefHelper.dumpAllPrefs(this));

            // Navigate to login screen and clear activity stack
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


    private long backPressedTime = 0;
    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if (System.currentTimeMillis() - backPressedTime < 2000) {
                super.onBackPressed();
            } else {
                backPressedTime = System.currentTimeMillis();
                Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
