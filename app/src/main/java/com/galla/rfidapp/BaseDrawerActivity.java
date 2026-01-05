package com.galla.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
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
import com.galla.rfidapp.util.PrefHelper;
import com.galla.rfidapp.util.SessionManager;

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

            // ----------- PERMISSION CHECK --------------
            Menu menu = navigationView.getMenu();
            MenuItem createInventory = menu.findItem(R.id.tag_mapping);

            boolean canCreateInventory = PrefHelper.hasPermission(this, "create.inventory");
            if (createInventory != null) {
                createInventory.setVisible(canCreateInventory);
            }

            // ----------- DRAWER HEADER SETUP --------------
            View header = navigationView.getHeaderView(0);
            if (header == null) {
                header = navigationView.inflateHeaderView(R.layout.nav_header);
            }

            TextView tvUserName = header.findViewById(R.id.tvUserName);

            // READ FROM PREFHELPER
            String savedUserName = PrefHelper.getHeaderUserName(this);
            String savedWorkingStage = PrefHelper.getHeaderWorkingStage(this);
//============= Formating removing "_" from stage =================== START ==================
            String formattedStage = savedWorkingStage.replace("_", " ").toLowerCase();
            StringBuilder builder = new StringBuilder();
            boolean capitalizeNext = true;
            for (char c : formattedStage.toCharArray()) {
                if (capitalizeNext && Character.isLetter(c)) {
                    builder.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    builder.append(c);
                }

                if (c == ' ') {
                    capitalizeNext = true;
                }
            }
            formattedStage = builder.toString();
            String finalText = savedUserName + " (" + formattedStage + ")";
//============= Formating removing "_" from stage =================== END ==================
            tvUserName.setText(finalText);

            Log.e("HEADER_DEBUG", "User=" + savedUserName + " Stage=" + savedWorkingStage);
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

    /**
     * Direct navigation to target activity (no LauncherActivity routing).
     */
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

        if (handleNavigationItem(item))
            return true;

        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            navigateTo(DashboardActivity.class);

        } else if (id == R.id.nav_products) {
            navigateTo(ProductsActivity.class);

        }else if (id == R.id.laundry_tage_control) {
            navigateTo(TagDetailsActivity.class);

        } else if (id == R.id.tag_mapping) {
            navigateTo(ScannerActivity.class);

        }else if (id == R.id.items_inventory) {
            navigateTo(InventoryActivity.class);

        }else if (id == R.id.nav_settings) {
            navigateTo(SettingsActivity.class);

        } else if (id == R.id.nav_logout) {

            SessionManager.logoutKeepPreferences(this);

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        if (drawerLayout != null)
            drawerLayout.closeDrawer(GravityCompat.START);

        return true;
    }

    protected boolean handleNavigationItem(@NonNull MenuItem item) {
        return false;
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
