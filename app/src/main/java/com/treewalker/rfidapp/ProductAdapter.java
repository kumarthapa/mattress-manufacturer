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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying ProductNetwork items in the products list.
 * Place this file in com/sleepcompany/rfidapp/adapter/ProductAdapter.java
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<ProductNetwork> products;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(ProductNetwork product);
    }

    public ProductAdapter(List<ProductNetwork> products, OnProductClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_laundry, parent, false);  // ensure this XML exists
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductNetwork product = products.get(position);
        holder.bind(product);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    public void updateProducts(List<ProductNetwork> newProducts) {
        this.products = newProducts;
        notifyDataSetChanged();
    }

    public void addProducts(List<ProductNetwork> newProducts) {
        if (this.products != null && newProducts != null) {
            this.products.addAll(newProducts);
            notifyDataSetChanged();
        }
    }

    /* ===================================================
                     VIEW HOLDER
    =================================================== */
    static class ProductViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvProductName, tvProductCode, tvQuantity;
        private final TextView tvLastActivityType, tvOpening, tvInward, tvOutward, tvClosing, tvUpdatedAt;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductCode = itemView.findViewById(R.id.tvProductCode);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);

            tvLastActivityType = itemView.findViewById(R.id.tvLastActivityType);
            tvOpening = itemView.findViewById(R.id.tvOpeningStock);
            tvInward = itemView.findViewById(R.id.tvInward);
            tvOutward = itemView.findViewById(R.id.tvOutward);
            tvClosing = itemView.findViewById(R.id.tvClosingStock);
            tvUpdatedAt = itemView.findViewById(R.id.tvUpdatedAt);
        }

        public void bind(ProductNetwork product) {

            tvProductName.setText(product.getProductName() != null ? product.getProductName() : "-");
            tvProductCode.setText("Code: " + (product.getProductCode() != null ? product.getProductCode() : "-"));
            tvQuantity.setText("Qty: " + product.getQuantity());

            LastActivity last = product.getLastActivity();

            if (last != null) {
                tvLastActivityType.setText("Last: " + safeString(last.getTransType()));
                tvOpening.setText("Open: " + last.getOpeningStock());
                tvInward.setText("In: " + last.getInward());
                tvOutward.setText("Out: " + last.getOutward());
                tvClosing.setText("Close: " + last.getClosingStock());

                String at = last.getActivityDate(); // matches model getter name
                if (at != null && !at.isEmpty()) {
                    tvUpdatedAt.setText(formatDate(at));
                } else {
                    tvUpdatedAt.setText("Updated: N/A");
                }
            } else {
                tvLastActivityType.setText("Last: N/A");
                tvOpening.setText("Open: 0");
                tvInward.setText("In: 0");
                tvOutward.setText("Out: 0");
                tvClosing.setText("Close: 0");
                tvUpdatedAt.setText("Updated: N/A");
            }
        }

        private static String safeString(String s) {
            return s == null ? "" : s;
        }

        private String formatDate(String dateString) {
            try {
                SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat output = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                Date date = input.parse(dateString);
                return output.format(date);
            } catch (ParseException e) {
                return dateString;
            }
        }
    }
}
