package com.sleepcompany.rfidapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public static class Product {
        public String id, name, currentStage;
        public int progress; // 0-100

        public Product(String id, String name, String currentStage, int progress) {
            this.id = id;
            this.name = name;
            this.currentStage = currentStage;
            this.progress = progress;
        }
    }

    private List<Product> productList;
    private OnItemClickListener listener;

    public ProductAdapter(List<Product> products, OnItemClickListener listener) {
        this.productList = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductAdapter.ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.productName.setText(product.name);
        holder.productId.setText("ID: " + product.id);
        holder.productProgress.setProgress(product.progress);
        holder.currentStage.setText("Current Stage: " + product.currentStage);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView productName, productId, currentStage;
        public ProgressBar productProgress;

        public ViewHolder(View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productId = itemView.findViewById(R.id.productId);
            productProgress = itemView.findViewById(R.id.productProgress);
            currentStage = itemView.findViewById(R.id.currentStage);
        }
    }
}
