package com.sleepcompany.rfidapp;

import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;
import com.seuic.uhf.UHFService;
import com.sleepcompany.rfidapp.databinding.ActivityScannerBinding;

public class ScannerActivity extends BaseDrawerActivity {

    private ActivityScannerBinding binding;
    private FragmentManager fm;
    private UHFService mDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mDevice = UHFService.getInstance();

        // Toolbar setup
        setSupportActionBar(binding.toolbar);

        // TabLayout setup
        fm = getSupportFragmentManager();
        setupTabs();

        // Setup FloatingActionButton click listener
//        binding.fabAction.setOnClickListener(v -> {
//            // TODO: Handle FAB action, e.g., open Add Item screen or dialog
//            Toast.makeText(this, "Add Item clicked", Toast.LENGTH_SHORT).show();
//        });
    }

    private void setupTabs() {
//        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.inventory)));
//        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.config)));

        // Default fragment
        replaceFragment(InventoryFragment.getInstance());

//        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
//            @Override
//            public void onTabSelected(TabLayout.Tab tab) {
//                if (tab.getPosition() == 0) {
//                    replaceFragment(InventoryFragment.getInstance());
//                } else {
//                    replaceFragment(SettingsFragment.getInstance());
//                }
//            }
//
//            @Override
//            public void onTabUnselected(TabLayout.Tab tab) { }
//
//            @Override
//            public void onTabReselected(TabLayout.Tab tab) { }
//        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.frl_content, fragment);
        ft.commit();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_scanner;
    }

    @Override
    protected boolean useDataBinding() {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDevice != null && !mDevice.open()) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDevice != null) mDevice.close();
    }
}
