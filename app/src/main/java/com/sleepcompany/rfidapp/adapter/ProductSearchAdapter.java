package com.sleepcompany.rfidapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sleepcompany.rfidapp.R;
import com.sleepcompany.rfidapp.network.BondingProduct;

import java.util.List;

public class ProductSearchAdapter extends RecyclerView.Adapter<ProductSearchAdapter.ProductViewHolder> {

    private final List<BondingProduct> products;
    private final OnProductSelectedListener listener;

    public interface OnProductSelectedListener {
        void onProductSelected(BondingProduct product);
    }

    public ProductSearchAdapter(List<BondingProduct> products, OnProductSelectedListener listener) {
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
        BondingProduct product = products.get(position);
        holder.bind(product, listener);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvQaCode, tvProductName, tvProductModel, serialNumber;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQaCode = itemView.findViewById(R.id.tvQaCode);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductModel = itemView.findViewById(R.id.tvProductModel);
            serialNumber = itemView.findViewById(R.id.serialNumber);
        }

        public void bind(BondingProduct product, OnProductSelectedListener listener) {
            tvQaCode.setText(product.getQaCode() != null ? product.getQaCode() : "-");
            tvProductName.setText(product.getProductName() != null ? "Name: " + product.getProductName() : "-");
            tvProductModel.setText(product.getModel() != null ? "Model: " + product.getModel() : "-");
            serialNumber.setText(product.getSerialNumber() != 0 ? "Sl No: " + product.getSerialNumber() : "-");

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductSelected(product);
                }
            });
        }
    }
}
