package com.sleepcompany.rfidapp;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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

        // Setup toolbar as the ActionBar
        setSupportActionBar(binding.toolbar);

        // Show the back arrow icon in toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);         // Show back button
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back); // Your back arrow icon
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Handle back navigation icon click
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Setup fragment manager & load default fragment
        fm = getSupportFragmentManager();
        setupTabs();
    }

    private void setupTabs() {
        // Default fragment on start
        replaceFragment(InventoryFragment.getInstance());
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.frl_content, fragment);
        ft.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle toolbar button clicks; back button triggers finish or fragment backstack pop
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
