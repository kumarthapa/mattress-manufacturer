package com.galla.rfidapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.galla.rfidapp.ProductsActivity;
import com.galla.rfidapp.R;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying Asset rows (Audit / Inventory view).
 * Uses item_product_laundry.xml (asset version).
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.AssetViewHolder> {

    private List<ProductsActivity.AssetItem> assets;
    private OnAssetClickListener listener;

    public interface OnAssetClickListener {
        void onAssetClick(ProductsActivity.AssetItem asset);
    }

    public ProductAdapter(List<ProductsActivity.AssetItem> assets,
                          OnAssetClickListener listener) {
        this.assets = assets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AssetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_laundry, parent, false);
        return new AssetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssetViewHolder holder, int position) {
        ProductsActivity.AssetItem asset = assets.get(position);
        holder.bind(asset);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAssetClick(asset);
            }
        });
    }

    @Override
    public int getItemCount() {
        return assets != null ? assets.size() : 0;
    }

    public void updateAssets(List<ProductsActivity.AssetItem> newAssets) {
        this.assets = newAssets;
        notifyDataSetChanged();
    }

    /* =====================================================
                         VIEW HOLDER
       ===================================================== */
    static class AssetViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvAssetTag;
        private final TextView tvRfid;
        private final TextView tvLocation;

        AssetViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvProductName);
            tvAssetTag = itemView.findViewById(R.id.tvProductCode);
            tvRfid = itemView.findViewById(R.id.tvQuantity);
            tvLocation = itemView.findViewById(R.id.tvLastActivityType);
        }

        void bind(ProductsActivity.AssetItem asset) {

            String name = safe(asset.name);
            String assetTag = safe(
                    asset.assetTag != null
                            ? asset.assetTag
                            : asset.externalAssetId
            );
            String rfid = safe(asset.rfid);
            String location = safe(
                    asset.locationName != null
                            ? asset.locationName
                            : (asset.locationId != null ? "Location #" + asset.locationId : null)
            );

            tvName.setText(name);
            tvAssetTag.setText(String.format(Locale.getDefault(),
                    "Asset Tag: %s", assetTag));

            tvRfid.setText(String.format(Locale.getDefault(),
                    "RFID: %s", rfid));

            tvLocation.setText(String.format(Locale.getDefault(),
                    "Location: %s", location));
        }

        private String safe(String v) {
            return (v == null || v.trim().isEmpty()) ? "" : v;
        }
    }
}
