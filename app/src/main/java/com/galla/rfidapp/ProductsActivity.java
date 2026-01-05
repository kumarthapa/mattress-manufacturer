package com.galla.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.galla.rfidapp.adapter.ProductAdapter;
import com.galla.rfidapp.network.ApiClient;
import com.galla.rfidapp.network.ApiService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.galla.rfidapp.util.SyncManager;

/**
 * ProductsActivity — displays Assets (from /assets/all-details)
 * - safe parsing
 * - local search
 * - stable sorting
 */
public class ProductsActivity extends BaseDrawerActivity {

    private long lastSeenSyncVersion = 0;

    private static final String TAG = "ProductsActivity";

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private MaterialButton refreshButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputEditText searchInput;

    private TextView tvTotalProducts;
    private TextView tvTotalQuantity;
    private TextView tvNoResults;

    // full list from API
    private final List<AssetItem> allAssets = new ArrayList<>();
    // filtered list for UI
    private final List<AssetItem> filteredAssets = new ArrayList<>();

    private String currentSearchQuery = "";
    private final android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY = 400;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_products;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();
        initViews();
        setupRecyclerView();
        setupListeners();
        setupSearch();

        loadAssets(true);
    }

    /* =========================================================
                           UI SETUP
       ========================================================= */

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Assets");
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.productsRecyclerView);
        refreshButton = findViewById(R.id.refreshProducts);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        searchInput = findViewById(R.id.searchInput);

        tvTotalProducts = findViewById(R.id.tvActiveProducts);
        tvTotalQuantity = findViewById(R.id.tvPassedProducts);
        tvNoResults = findViewById(R.id.tvNoResults);

        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light
        );
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        adapter = new ProductAdapter(filteredAssets, this::onAssetClick);
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        refreshButton.setOnClickListener(v -> refreshData());
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    currentSearchQuery = s == null ? "" : s.toString().trim();
                    applyLocalFilter();
                };
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }
        });
    }

    /* =========================================================
                         API LOADING
       ========================================================= */

    private void loadAssets(boolean isRefresh) {

        if (isRefresh) swipeRefreshLayout.setRefreshing(true);
        refreshButton.setEnabled(false);
        refreshButton.setText("Loading...");

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getAllAssetDetails().enqueue(new Callback<Map<String, Object>>() {

            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                restoreUi();

                if (!response.isSuccessful() || response.body() == null) {
                    showMessage("Failed to load items");
                    return;
                }

                try {
                    allAssets.clear();

                    Object assetsObj = response.body().get("assets");
                    if (assetsObj instanceof List<?>) {
                        for (Object o : (List<?>) assetsObj) {
                            if (o instanceof Map<?, ?>) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> m = (Map<String, Object>) o;
                                allAssets.add(AssetItem.fromMap(m));
                            }
                        }
                    }

                    // SAFE sort by id desc (NO crash)
                    Collections.sort(allAssets, (a, b) ->
                            Long.compare(safeLong(b.id), safeLong(a.id))
                    );

                    applyLocalFilter();
                    updateStats();

                    showMessage("Fetch Successfully " + allAssets.size() + " items");

                } catch (Exception e) {
                    Log.e(TAG, "Parsing error", e);
                    showMessage("Failed to parse items");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                restoreUi();
                showMessage("Network error: " + t.getMessage());
            }
        });
    }

    private void restoreUi() {
        swipeRefreshLayout.setRefreshing(false);
        refreshButton.setEnabled(true);
        refreshButton.setText("Refresh");
    }

    /* =========================================================
                         FILTER + STATS
       ========================================================= */

    private void applyLocalFilter() {
        filteredAssets.clear();
        String q = currentSearchQuery.toLowerCase();

        for (AssetItem a : allAssets) {
            if (q.isEmpty()
                    || contains(a.name, q)
                    || contains(a.assetTag, q)
                    || contains(a.rfid, q)
                    || contains(a.model, q)
                    || contains(a.serial, q)) {
                filteredAssets.add(a);
            }
        }

        adapter.updateAssets(filteredAssets);

        tvNoResults.setVisibility(filteredAssets.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(filteredAssets.isEmpty() ? View.GONE : View.VISIBLE);

        updateStats();
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    private void updateStats() {
        tvTotalProducts.setText(String.valueOf(allAssets.size()));

        int tagCount = 0;
        for (AssetItem a : allAssets) {
            if (!TextUtils.isEmpty(a.rfid)) tagCount++;
        }
        tvTotalQuantity.setText(String.valueOf(tagCount));
    }

    private void refreshData() {
        currentSearchQuery = "";
        searchInput.setText("");
        loadAssets(true);
    }

    /* =========================================================
                         NAVIGATION
       ========================================================= */

    private void onAssetClick(AssetItem asset) {
        Intent i = new Intent(this, ProductDetailsActivity.class);
        i.putExtra("assetId", String.valueOf(asset.id));
        i.putExtra("productName", asset.name);
        i.putExtra("productCode", asset.assetTag);
        i.putExtra("rfid", asset.rfid);
        i.putExtra("model", asset.model);
        i.putExtra("serial", asset.serial);
        startActivity(i);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
    }

    private void showMessage(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show();
    }

    /* =========================================================
                         SAFE HELPERS
       ========================================================= */

    private long safeLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0L;
        }
    }

    /* =========================================================
                         ASSET MODEL
       ========================================================= */

    public static class AssetItem {
        public Object id;
        public String externalAssetId;
        public String name;
        public String assetTag;
        public String rfid;
        public String model;
        public String serial;
        public Object locationId;
        public String locationName;

        public static AssetItem fromMap(Map<String, Object> m) {
            AssetItem a = new AssetItem();
            a.id = m.get("id");
            a.externalAssetId = str(m.get("external_asset_id"));
            a.name = str(m.get("name"));
            a.assetTag = str(m.get("asset_tag"));
            a.rfid = str(m.get("rfid"));
            a.model = str(m.get("model_id"));
            a.serial = str(m.get("serial"));
            a.locationId = m.get("location_id");
            a.locationName = str(m.get("location_name"));
            return a;
        }

        private static String str(Object o) {
            return o == null ? null : String.valueOf(o);
        }
    }
    @Override
    protected void onSyncClicked() {
        Log.d(TAG, "Sync icon clicked → refreshing Products");
        refreshData();   // reuse your existing method
    }
    @Override
    protected void onResume() {
        super.onResume();

        long globalVersion = SyncManager.getSyncVersion(this);
        if (globalVersion > lastSeenSyncVersion) {
            lastSeenSyncVersion = globalVersion;
            Log.d(TAG, "Global sync detected → auto refresh Products");
            refreshData();
        }
    }

}
