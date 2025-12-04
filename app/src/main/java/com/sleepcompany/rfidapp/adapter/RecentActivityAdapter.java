package com.sleepcompany.rfidapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.sleepcompany.rfidapp.R;
import com.sleepcompany.rfidapp.model.RecentActivity;

import java.util.ArrayList;
import java.util.List;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.VH> {

    private final List<RecentActivity> items = new ArrayList<>();

    public void setItems(List<RecentActivity> data) {
        items.clear();
        if (data != null && !data.isEmpty()) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        RecentActivity r = items.get(position);

        // Product name
        holder.tvProductName.setText(r.product_name != null ? r.product_name : "—");

        // Stage + status + machine
        StringBuilder stageStatus = new StringBuilder();
        stageStatus.append(r.stage != null ? r.stage : "—");
        stageStatus.append(" • ");
        stageStatus.append(r.status != null ? r.status : "—");
        if (r.machine_no != null && !r.machine_no.trim().isEmpty()) {
            stageStatus.append(" • ").append(r.machine_no);
        }
        holder.tvStageStatus.setText(stageStatus.toString());

        // Changed at
        holder.tvChangedAt.setText(r.changed_at != null ? r.changed_at : "—");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialTextView tvProductName, tvStageStatus, tvChangedAt;
        MaterialCardView rootCard;

        VH(@NonNull View itemView) {
            super(itemView);
            rootCard = itemView.findViewById(R.id.rootCard);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvStageStatus = itemView.findViewById(R.id.tvStageStatus);
            tvChangedAt = itemView.findViewById(R.id.tvChangedAt);
        }
    }
}
