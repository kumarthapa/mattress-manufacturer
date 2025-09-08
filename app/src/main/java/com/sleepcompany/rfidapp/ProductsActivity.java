package com.sleepcompany.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
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
    private ExtendedFloatingActionButton fabAddProduct;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputEditText searchInput;

    private TextView tvActiveProducts;
    private TextView tvPassedProducts;

    private List<Product> products = new ArrayList<>();
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private String currentSearchQuery = "";

    private android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY = 800;

    private static final String TAG = "ProductsActivity";

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_products;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for token first
        String token = ApiClient.getToken(this);
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "No token found. Redirecting to LoginActivity.");
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
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Production Tracking");
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.productsRecyclerView);
        refreshButton = findViewById(R.id.refreshProducts);
        fabAddProduct = findViewById(R.id.fabAddProduct);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        searchInput = findViewById(R.id.searchInput);

        tvActiveProducts = findViewById(R.id.tvActiveProducts);
        tvPassedProducts = findViewById(R.id.tvPassedProducts);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        adapter = new ProductAdapter(products, this::onProductClick);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();
                if (layoutManager != null && !isLoading && hasMoreData && dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (pastVisibleItems + visibleItemCount >= totalItemCount - 3) {
                        loadProducts(false);
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        refreshButton.setOnClickListener(v -> refreshData());

        fabAddProduct.setOnClickListener(v -> Snackbar.make(v, "Add Product feature coming soon!", Snackbar.LENGTH_SHORT)
                .setAnchorView(fabAddProduct)
                .show());

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::refreshData);
            swipeRefreshLayout.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light
            );
        }
    }

    private void setupSearchFunctionality() {
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                    searchRunnable = () -> {
                        String query = s.toString().trim();
                        if (!query.equals(currentSearchQuery)) {
                            currentSearchQuery = query;
                            refreshData();
                        }
                    };
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            searchInput.setOnEditorActionListener((v, actionId, event) -> {
                performSearch();
                return true;
            });
        }
    }

    private void performSearch() {
        String query = searchInput != null ? searchInput.getText().toString().trim() : "";
        if (!query.equals(currentSearchQuery)) {
            currentSearchQuery = query;
            refreshData();
        }
        if (searchInput != null) searchInput.clearFocus();
    }

    private void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailsActivity.class);
        intent.putExtra("productId", product.getId());
        intent.putExtra("productName", product.getProductName());
        intent.putExtra("sku", product.getSku());
        intent.putExtra("size", product.getSize());
        intent.putExtra("quantity", product.getQuantity());
        intent.putExtra("qcStatus", product.getQcStatus());
        intent.putExtra("createdAt", product.getCreatedAt());
        startActivity(intent);
    }

    private void loadProducts(boolean isRefresh) {
        Log.d(TAG, "loadProducts called. Page: " + currentPage + ", Search: " + currentSearchQuery);
        Log.d(TAG, "Current Token: " + ApiClient.getToken(this));

        if (isLoading) return;
        isLoading = true;

        if (isRefresh) {
            currentPage = 1;
            hasMoreData = true;
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
        }

        refreshButton.setEnabled(false);
        refreshButton.setText("Loading...");

        ProductsRequest request = new ProductsRequest();
        request.setSearch(currentSearchQuery);
        request.setStatus("all");
        request.setPage(currentPage);
        request.setLimit(20);

        Log.d(TAG, "Request body: " + request.toString());

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.getPlanProducts(request).enqueue(new Callback<ProductsResponse>() {
            @Override
            public void onResponse(Call<ProductsResponse> call, Response<ProductsResponse> response) {
                isLoading = false;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                refreshButton.setEnabled(true);
                refreshButton.setText("Refresh");

                Log.d(TAG, "API Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ProductsResponse productsResponse = response.body();
                    List<Product> newProducts = productsResponse.getProducts();

                    if (isRefresh) products.clear();
                    if (newProducts != null && !newProducts.isEmpty()) {
                        products.addAll(newProducts);
                        adapter.updateProducts(products);
                        currentPage++;

                        ProductsResponse.Pagination pagination = productsResponse.getPagination();
                        hasMoreData = pagination != null && pagination.getCurrent_page() < pagination.getLast_page();
                        updateStats(products, pagination);
                    } else {
                        hasMoreData = false;
                        if (isRefresh && products.isEmpty()) {
                            showMessage("No products found");
                            updateStats(new ArrayList<>(), null);
                        }
                    }

                    if (isRefresh) showMessage("Products loaded successfully");

                } else {
                    String errorMsg = response.code() == 401
                            ? "Authentication required. Please login again."
                            : response.code() == 403
                            ? "Access denied. Check your permissions."
                            : response.code() >= 500
                            ? "Server error. Please try again later."
                            : (response.body() != null && !TextUtils.isEmpty(response.body().getMessage()))
                            ? response.body().getMessage()
                            : "Failed to load products";

                    if (response.code() == 401) {
                        // Token invalid, redirect to login
                        ApiClient.clearToken(ProductsActivity.this);
                        startActivity(new Intent(ProductsActivity.this, LoginActivity.class));
                        finish();
                    }

                    showMessage(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ProductsResponse> call, Throwable t) {
                isLoading = false;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                refreshButton.setEnabled(true);
                refreshButton.setText("Refresh");

                String errorMsg = "Network error";
                if (t.getMessage() != null) {
                    if (t.getMessage().contains("timeout")) errorMsg = "Request timeout. Check your connection.";
                    else if (t.getMessage().contains("Unable to resolve host")) errorMsg = "No internet connection";
                    else errorMsg = "Network error: " + t.getMessage();
                }
                Log.e(TAG, errorMsg, t);
                showMessage(errorMsg);
            }
        });
    }

    private void updateStats(List<Product> productList, ProductsResponse.Pagination pagination) {
        if (tvActiveProducts != null) {
            int totalCount = pagination != null ? pagination.getTotal() : productList.size();
            tvActiveProducts.setText(String.valueOf(totalCount));
        }

        if (tvPassedProducts != null) {
            long passedCount = productList.stream().filter(p -> "PASS".equals(p.getQcStatus())).count();
            tvPassedProducts.setText(String.valueOf(passedCount));
        }
    }

    private void refreshData() {
        loadProducts(true);
    }

    private void showMessage(String message) {
        if (recyclerView != null && !TextUtils.isEmpty(message)) {
            Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT)
                    .setAnchorView(fabAddProduct)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchHandler != null && searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
