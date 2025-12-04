package com.sleepcompany.rfidapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sleepcompany.rfidapp.model.Product;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> products;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(List<Product> products, OnProductClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    public void updateProducts(List<Product> newProducts) {
        this.products = newProducts;
        notifyDataSetChanged();
    }

    public void addProducts(List<Product> newProducts) {
        if (this.products != null && newProducts != null) {
            this.products.addAll(newProducts);
            notifyDataSetChanged();
        }
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private TextView productName, sku, size, quantity, qcStatus, currentStage, createdAt;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.tvProductName);
            sku = itemView.findViewById(R.id.tvSku);
            size = itemView.findViewById(R.id.tvSize);
            currentStage = itemView.findViewById(R.id.tvCurrentStage);
            quantity = itemView.findViewById(R.id.tvQuantity);
            qcStatus = itemView.findViewById(R.id.tvQcStatus);
            createdAt = itemView.findViewById(R.id.tvCreatedAt);
        }

        public void bind(Product product) {
            productName.setText(product.getProductName());
            sku.setText("SKU: " + product.getSku());
            size.setText("Size: " + product.getSize());
            quantity.setText("Qty: " + product.getQuantity());
            qcStatus.setText(product.getQcStatus());
            currentStage.setText("Current Stage: " + product.getStage());
            // Set QC status color based on status
            switch (product.getQcStatus()) {
                case "PASS":
                    qcStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_dark));
                    break;
                case "FAIL":
                    qcStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark));
                    break;
                case "PENDING":
                    qcStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_orange_dark));
                    break;
                default:
                    qcStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray));
                    break;
            }

            // Format and display created date
            if (product.getCreatedAt() != null && !product.getCreatedAt().isEmpty()) {
                try {
                    // Assuming the date comes in format "2023-01-01 12:00:00"
                    String formattedDate = formatDate(product.getCreatedAt());
                    createdAt.setText(formattedDate);
                } catch (Exception e) {
                    createdAt.setText(product.getCreatedAt());
                }
            } else {
                createdAt.setText("N/A");
            }
        }

        private String formatDate(String dateString) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(dateString);
                return outputFormat.format(date);
            } catch (Exception e) {
                return dateString; // Return original if parsing fails
            }
        }
    }
}
