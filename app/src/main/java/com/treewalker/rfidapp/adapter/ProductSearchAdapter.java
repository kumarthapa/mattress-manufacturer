package com.treewalker.rfidapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.treewalker.rfidapp.R;
import com.treewalker.rfidapp.model.ProductNetwork;
import com.treewalker.rfidapp.model.ProductNetwork.LastActivity;

import java.util.List;

public class ProductSearchAdapter extends RecyclerView.Adapter<ProductSearchAdapter.ProductViewHolder> {

    private final List<ProductNetwork> products;
    private final OnProductSelectedListener listener;

    public interface OnProductSelectedListener {
        void onProductSelected(ProductNetwork product);
    }

    public ProductSearchAdapter(List<ProductNetwork> products, OnProductSelectedListener listener) {
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_search, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductNetwork product = products.get(position);
        holder.bind(product, listener);
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvProductName, tvProductCode, tvCategory, tvQuantity, tvLastActivity;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductCode = itemView.findViewById(R.id.tvProductCode);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvLastActivity = itemView.findViewById(R.id.tvLastActivity);
        }

        public void bind(ProductNetwork product, OnProductSelectedListener listener) {

            tvProductName.setText(product.getProductName());
            tvProductCode.setText("Code: " + product.getProductCode());
            tvCategory.setText("Category: " + product.getCategory());
            tvQuantity.setText("Qty: " + product.getQuantity());

            // Last activity handling
            LastActivity la = product.getLastActivity();
            if (la != null) {
                String activityText = safeString(la.getTransType())
                        + " | IN: " + la.getInward()
                        + " | OUT: " + la.getOutward()
                        + "\nStock: " + la.getOpeningStock() + " → " + la.getClosingStock();

                tvLastActivity.setText(activityText);
            } else {
                tvLastActivity.setText("No activity yet");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductSelected(product);
                }
            });
        }

        private static String safeString(String s) {
            return s == null ? "" : s;
        }
    }
}
