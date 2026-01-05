package com.galla.rfidapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.galla.rfidapp.R;
import com.galla.rfidapp.model.AssetNetwork;

import java.util.List;

/**
 * ProductSearchAdapter
 * ---------------------
 * Simplified adapter for Asset search & selection.
 * ❌ Removed: category, quantity, last activity, stock logic
 * ✅ Uses only fields that actually exist in AssetNetwork
 */
public class ProductSearchAdapter extends RecyclerView.Adapter<ProductSearchAdapter.ProductViewHolder> {

    private final List<AssetNetwork> assets;
    private final OnAssetSelectedListener listener;

    public interface OnAssetSelectedListener {
        void onAssetSelected(AssetNetwork asset);
    }

    public ProductSearchAdapter(List<AssetNetwork> assets, OnAssetSelectedListener listener) {
        this.assets = assets;
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
        AssetNetwork asset = assets.get(position);
        holder.bind(asset, listener);
    }

    @Override
    public int getItemCount() {
        return assets != null ? assets.size() : 0;
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvCode;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvCode = itemView.findViewById(R.id.tvProductCode);
        }

        void bind(AssetNetwork asset, OnAssetSelectedListener listener) {

            String name = asset.getName() != null ? asset.getName() : "";
            String code = asset.getAssetTag() != null ? asset.getAssetTag() : "";

            tvName.setText(name);
            tvCode.setText(code.isEmpty() ? "" : "Tag: " + code);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAssetSelected(asset);
                }
            });
        }
    }
}
