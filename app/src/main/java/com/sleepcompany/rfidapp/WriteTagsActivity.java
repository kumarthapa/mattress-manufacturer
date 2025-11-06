package com.sleepcompany.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import com.sleepcompany.rfidapp.adapter.ProductSearchAdapter;
import com.sleepcompany.rfidapp.network.ApiClient;
import com.sleepcompany.rfidapp.network.ApiService;
import com.sleepcompany.rfidapp.network.BondingProduct;
import com.sleepcompany.rfidapp.network.BondingProductsRequest;
import com.sleepcompany.rfidapp.network.BondingResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WriteTagsActivity extends BaseDrawerActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText etSearch;
    private MaterialButton btnSearch;
    private RecyclerView rvSearchResults;
    private TextView tvNoResults;
    private CircularProgressIndicator progressIndicator;

    private ProductSearchAdapter adapter;
    private List<BondingProduct> searchResults = new ArrayList<>();

    private static final int SEARCH_DELAY = 800;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private static final int REQ_WRITE_DETAIL = 101;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_write_tags;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();

        // Load all products by default
        performSearch();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        tvNoResults = findViewById(R.id.tvNoResults);
        progressIndicator = findViewById(R.id.progressIndicator);

        if (rvSearchResults != null) rvSearchResults.setVisibility(View.VISIBLE);
        if (tvNoResults != null) tvNoResults.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Bonding: Write RFID Tags");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupRecyclerView() {
        adapter = new ProductSearchAdapter(searchResults, this::onProductSelected);
        rvSearchResults.setAdapter(adapter);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> performSearch());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> performSearch();
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }
        });
    }

    private void performSearch() {
        String searchTerm = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";

        showProgress(true);

        BondingProductsRequest request = new BondingProductsRequest();
        request.setSearch(searchTerm);
        request.setPage(1);
        request.setLimit(200);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.getPlanProducts(request).enqueue(new Callback<BondingResponse>() {
            @Override
            public void onResponse(Call<BondingResponse> call, Response<BondingResponse> response) {
                showProgress(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    searchResults.clear();
                    List<BondingProduct> fetched = response.body().getProducts();

                    if (fetched != null && !fetched.isEmpty()) {
                        searchResults.addAll(fetched);
                        adapter.notifyDataSetChanged();

                        if (rvSearchResults != null) rvSearchResults.setVisibility(View.VISIBLE);
                        if (tvNoResults != null) tvNoResults.setVisibility(View.GONE);
                    } else {
                        if (rvSearchResults != null) rvSearchResults.setVisibility(View.GONE);
                        if (tvNoResults != null) {
                            tvNoResults.setVisibility(View.VISIBLE);
                            tvNoResults.setText("No products found.");
                        }
                    }
                } else {
                    Toast.makeText(WriteTagsActivity.this,
                            "Failed to fetch products: " + (response.body() != null ? response.body().getMessage() : "Unknown error"),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BondingResponse> call, Throwable t) {
                showProgress(false);
                Toast.makeText(WriteTagsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onProductSelected(BondingProduct product) {
        Intent i = new Intent(this, WriteTagDetailActivity.class);
        i.putExtra(WriteTagDetailActivity.EXTRA_PRODUCT_ID, product.getId());
        i.putExtra(WriteTagDetailActivity.EXTRA_QA_CODE, product.getQaCode());
        i.putExtra(WriteTagDetailActivity.EXTRA_PRODUCT_NAME, product.getProductName());
        i.putExtra(WriteTagDetailActivity.EXTRA_PRODUCT_MODEL, product.getModel());

        startActivityForResult(i, REQ_WRITE_DETAIL);
    }

    private void showProgress(boolean show) {
        if (progressIndicator != null) progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        if (btnSearch != null) btnSearch.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
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
