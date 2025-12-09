package com.sleepcompany.rfidapp;

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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.sleepcompany.rfidapp.adapter.ProductAdapter;
import com.sleepcompany.rfidapp.model.ProductNetwork;
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

    private TextView tvTotalProducts;
    private TextView tvTotalQuantity;
    private TextView tvNoResults;

    private List<ProductNetwork> products = new ArrayList<>();
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

        // Check token
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
            getSupportActionBar().setTitle("Products");
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.productsRecyclerView);
        refreshButton = findViewById(R.id.refreshProducts);
        scaneTag = findViewById(R.id.scaneTag);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        searchInput = findViewById(R.id.searchInput);

        tvTotalProducts = findViewById(R.id.tvActiveProducts);   // repurposed
        tvTotalQuantity = findViewById(R.id.tvPassedProducts);   // repurposed
        tvNoResults = findViewById(R.id.tvNoResults);

        // ensure RecyclerView scrolls independently
        if (recyclerView != null) {
            recyclerView.setNestedScrollingEnabled(true);
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light
            );
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager lm = new LinearLayoutManager(this);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(lm);
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
    }

    private void setupClickListeners() {
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> refreshData());
        }

        if (scaneTag != null) {
            scaneTag.setOnClickListener(v ->
                    startActivity(new Intent(ProductsActivity.this, TagDetailsActivity.class)));
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::refreshData);
        }
    }

    private void setupSearchFunctionality() {
        if (searchInput == null) return;

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                final CharSequence text = s;
                searchRunnable = () -> {
                    String query = text.toString().trim();
                    if (!query.equals(currentSearchQuery)) {
                        currentSearchQuery = query;
                        refreshData();
                    }
                };
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });
    }

    private void performSearch() {
        Log.d(TAG, "performSearch called");
        String query = searchInput != null ? searchInput.getText().toString().trim() : "";
        if (!query.equals(currentSearchQuery)) {
            currentSearchQuery = query;
            refreshData();
        }
        if (searchInput != null) searchInput.clearFocus();
    }

    private void onProductClick(ProductNetwork product) {
        Intent intent = new Intent(this, ProductDetailsActivity.class);
        intent.putExtra("productId", product.getId());
        intent.putExtra("productName", product.getProductName());
        intent.putExtra("productCode", product.getProductCode());
        intent.putExtra("quantity", product.getQuantity());
        // if last activity present, pass some fields
        if (product.getLastActivity() != null) {
            intent.putExtra("lastTransType", product.getLastActivity().getTransType());
            intent.putExtra("lastClosingStock", product.getLastActivity().getClosingStock());
            intent.putExtra("lastAt", product.getLastActivity().getAt());
        }
        startActivity(intent);
    }

    private void loadProducts(boolean isRefresh) {
        Log.d(TAG, "loadProducts called. Page: " + currentPage + ", Search: " + currentSearchQuery);

        if (isLoading) return;
        isLoading = true;

        if (isRefresh) {
            currentPage = 1;
            hasMoreData = true;
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
        }

        if (refreshButton != null) {
            refreshButton.setEnabled(false);
            refreshButton.setText("Loading...");
        }

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
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (refreshButton != null) {
                    refreshButton.setEnabled(true);
                    refreshButton.setText("Refresh");
                }

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ProductsResponse productsResponse = response.body();
                    List<ProductNetwork> newProducts = productsResponse.getProducts();
                    Log.d(TAG, "onResponse: loaded " + (newProducts != null ? newProducts.size() : 0) + " items");

                    if (isRefresh) products.clear();
                    if (newProducts != null && !newProducts.isEmpty()) {
                        products.addAll(newProducts);
                        if (adapter != null) adapter.updateProducts(products);
                        currentPage++;

                        ProductsResponse.Pagination pagination = productsResponse.getPagination();
                        hasMoreData = pagination != null && pagination.getCurrent_page() < pagination.getLast_page();
                        updateStats(products, pagination);

                        if (tvNoResults != null) tvNoResults.setVisibility(View.GONE);
                        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                    } else {
                        Log.w(TAG, "No products found");
                        hasMoreData = false;
                        if (isRefresh && products.isEmpty()) {
                            if (tvNoResults != null) {
                                tvNoResults.setVisibility(View.VISIBLE);
                                tvNoResults.setText("No products found");
                            }
                            updateStats(new ArrayList<>(), null);
                            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
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
                if (refreshButton != null) {
                    refreshButton.setEnabled(true);
                    refreshButton.setText("Refresh");
                }

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

    private void updateStats(List<ProductNetwork> productList, ProductsResponse.Pagination pagination) {
        if (tvTotalProducts != null) {
            int totalCount = pagination != null ? pagination.getTotal() : productList.size();
            tvTotalProducts.setText(String.valueOf(totalCount));
        }

        if (tvTotalQuantity != null) {
            long totalQty = 0;
            try {
                for (ProductNetwork p : productList) {
                    totalQty += p.getQuantity();
                }
            } catch (Exception e) {
                totalQty = 0;
            }
            tvTotalQuantity.setText(String.valueOf(totalQty));
        }
    }

    private void refreshData() {
        loadProducts(true);
    }

    private void showMessage(String message) {
        if (recyclerView != null && !TextUtils.isEmpty(message)) {
            Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
