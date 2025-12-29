package com.sleepcompany.rfidapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.sleepcompany.rfidapp.R;
import com.sleepcompany.rfidapp.network.BondingProduct;

import java.util.ArrayList;
import java.util.List;

public class ProductSelectAdapter extends RecyclerView.Adapter<ProductSelectAdapter.VH> {

    public interface OnSelect {
        void onSelected(BondingProduct product);
    }

    private final List<BondingProduct> original;
    private final List<BondingProduct> working;
    private final OnSelect listener;

    public ProductSelectAdapter(List<BondingProduct> items, OnSelect listener) {
        this.original = items == null ? new ArrayList<>() : new ArrayList<>(items);
        this.working = new ArrayList<>(this.original);
        this.listener = listener;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_select_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        BondingProduct p = working.get(position);
        holder.tvSl.setText(String.valueOf(position + 1));
        holder.tvQa.setText(p.getQaCode() == null ? "-" : p.getQaCode());
        holder.tvName.setText(p.getProductName() == null ? "-" : p.getProductName());
        holder.tvModel.setText(p.getModel() == null ? "-" : p.getModel());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSelected(p);
        });
    }

    @Override
    public int getItemCount() {
        return working.size();
    }

    public void filter(String q) {
        working.clear();
        if (q == null || q.trim().isEmpty()) {
            working.addAll(original);
        } else {
            String low = q.toLowerCase();
            for (BondingProduct p : original) {
                if (p == null) continue;
                boolean match = false;
                if (p.getQaCode() != null && p.getQaCode().toLowerCase().contains(low)) match = true;
                if (!match && p.getModel() != null && p.getModel().toLowerCase().contains(low)) match = true;
                if (!match && p.getProductName() != null && p.getProductName().toLowerCase().contains(low)) match = true;
                if (!match && String.valueOf(p.getId()).contains(low)) {
                    match = true;
                }

                if (match) working.add(p);
            }
        }
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvSl, tvQa, tvName, tvModel;
        VH(View v) {
            super(v);
            tvSl = v.findViewById(R.id.tvRowSl);
            tvQa = v.findViewById(R.id.tvRowQa);
            tvName = v.findViewById(R.id.tvRowName);
            tvModel = v.findViewById(R.id.tvRowModel);
        }
    }
}
