package com.sleepcompany.rfidapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ProductsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private MaterialButton refreshButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        // Enable back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Products");
        }

        recyclerView    = findViewById(R.id.productsRecyclerView);
        refreshButton   = findViewById(R.id.refreshProducts);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Sample data
        List<ProductAdapter.Product> products = new ArrayList<>();
        products.add(new ProductAdapter.Product("MTR001","Premium Memory Foam Mattress","Assembly",60));
        products.add(new ProductAdapter.Product("MTR002","Hybrid Spring Mattress","QC Check",75));
        products.add(new ProductAdapter.Product("MTR003","Organic Cotton Mattress","Shipping",100));
        products.add(new ProductAdapter.Product("MTR004","Latex Hybrid Mattress","Raw Material",25));
        products.add(new ProductAdapter.Product("MTR005","Cooling Gel Mattress","Cutting",40));

        adapter = new ProductAdapter(products, product -> {
            Intent intent = new Intent(ProductsActivity.this, ProductDetailsActivity.class);
            intent.putExtra("productId", product.id);
            intent.putExtra("productName", product.name);
            intent.putExtra("productStage", product.currentStage);
            intent.putExtra("productProgress", product.progress);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        refreshButton.setOnClickListener(v -> {
            // TODO: implement refresh logic
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
