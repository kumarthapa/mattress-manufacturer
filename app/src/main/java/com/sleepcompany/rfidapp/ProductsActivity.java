package com.sleepcompany.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import com.sleepcompany.rfidapp.model.Product;
import com.sleepcompany.rfidapp.network.ApiClient;
import com.sleepcompany.rfidapp.network.ApiService;
import com.sleepcompany.rfidapp.network.ProductsRequest;
import com.sleepcompany.rfidapp.network.ProductsResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductsActivity extends BaseDrawerActivity {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private MaterialButton refreshButton;
    private ExtendedFloatingActionButton scaneTag;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputEditText searchInput;

    private TextView tvActiveProducts;
    private TextView tvPassedProducts;
    private TextView tvNoResults;

    private final List<Product> products = new ArrayList<>();
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private String currentSearchQuery = "";

    private final android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY = 800;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_products;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String token = ApiClient.getToken(this);
        if (token == null || token.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        setupSearchFunctionality();

        loadProducts(true);
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Production Tracking");
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.productsRecyclerView);
        refreshButton = findViewById(R.id.refreshProducts);
        scaneTag = findViewById(R.id.scaneTag);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        searchInput = findViewById(R.id.searchInput);

        tvActiveProducts = findViewById(R.id.tvActiveProducts);
        tvPassedProducts = findViewById(R.id.tvPassedProducts);
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

        // ✅ No click listener at all
        adapter = new ProductAdapter(products, null);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMoreData) return;

                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;

                int visible = lm.getChildCount();
                int total = lm.getItemCount();
                int firstVisible = lm.findFirstVisibleItemPosition();

                if (firstVisible + visible >= total - 3) {
                    loadProducts(false);
                }
            }
        });
    }

    private void setupClickListeners() {
        refreshButton.setOnClickListener(v -> refreshData());

        scaneTag.setOnClickListener(v ->
                startActivity(new Intent(this, ScannerActivity.class)));

        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
    }

    private void setupSearchFunctionality() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    currentSearchQuery = s.toString().trim();
                    refreshData();
                };
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadProducts(boolean isRefresh) {
        if (isLoading) return;
        isLoading = true;

        if (isRefresh) {
            currentPage = 1;
            hasMoreData = true;
            swipeRefreshLayout.setRefreshing(true);
        }

        refreshButton.setEnabled(false);
        refreshButton.setText("Loading...");

        ProductsRequest request = new ProductsRequest();
        request.setSearch(currentSearchQuery);
        request.setStatus("all");
        request.setPage(currentPage);
        request.setLimit(20);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.getProducts(request).enqueue(new Callback<ProductsResponse>() {

            @Override
            public void onResponse(Call<ProductsResponse> call, Response<ProductsResponse> response) {
                isLoading = false;
                swipeRefreshLayout.setRefreshing(false);
                refreshButton.setEnabled(true);
                refreshButton.setText("Refresh");

                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    showMessage("Failed to load products");
                    return;
                }

                ProductsResponse body = response.body();
                List<Product> newProducts = body.getProducts();

                if (isRefresh) products.clear();

                if (newProducts != null && !newProducts.isEmpty()) {
                    products.addAll(newProducts);
                    adapter.updateProducts(products);
                    currentPage++;

                    ProductsResponse.Pagination p = body.getPagination();
                    hasMoreData = p != null && p.getCurrent_page() < p.getLast_page();

                    updateStats(products, p);
                    tvNoResults.setVisibility(View.GONE);
                } else if (products.isEmpty()) {
                    tvNoResults.setVisibility(View.VISIBLE);
                    tvNoResults.setText("No products found");
                }
            }

            @Override
            public void onFailure(Call<ProductsResponse> call, Throwable t) {
                isLoading = false;
                swipeRefreshLayout.setRefreshing(false);
                refreshButton.setEnabled(true);
                refreshButton.setText("Refresh");
                showMessage("Network error");
            }
        });
    }

    private void updateStats(List<Product> list, ProductsResponse.Pagination pagination) {
        tvActiveProducts.setText(
                String.valueOf(pagination != null ? pagination.getTotal() : list.size())
        );

        int passed = 0;
        for (Product p : list) {
            if ("PASS".equalsIgnoreCase(p.getQcStatus())) passed++;
        }
        tvPassedProducts.setText(String.valueOf(passed));
    }

    private void refreshData() {
        loadProducts(true);
    }

    private void showMessage(String msg) {
        Snackbar.make(recyclerView, msg, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
