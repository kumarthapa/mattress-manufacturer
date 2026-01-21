package com.treewalker.rfidapp.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.treewalker.rfidapp.R;
import com.treewalker.rfidapp.model.RecentActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

        // PRODUCT NAME
        holder.tvProductName.setText(
                r.productName != null ? r.productName : "Unnamed Product"
        );

        // STATUS ONLY (since stages removed)
        String status = (r.status != null ? r.status : "—");
        holder.tvStageStatus.setText("Status: " + status);

        // RFID TAG OPTIONAL
        String rfid = (r.rfidTag != null ? r.rfidTag : "No Tag");
        holder.tvRfidTag.setText("Tag: " + rfid);

        // DATE FORMAT
        holder.tvChangedAt.setText(formatDate(r.changedAt));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** -----------------------------------------
     * Format API datetime → "12 Jan, 10:22 AM"
     * ----------------------------------------- */
    private String formatDate(String raw) {
        if (raw == null) return "—";

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat output = new SimpleDateFormat("dd MMM, hh:mm a");

        try {
            Date d = input.parse(raw);
            return output.format(d);
        } catch (ParseException e) {
            return raw;
        }
    }

    /** -----------------------------------------
     * ViewHolder
     * ----------------------------------------- */
    static class VH extends RecyclerView.ViewHolder {
        MaterialTextView tvProductName, tvStageStatus, tvChangedAt, tvRfidTag;
        MaterialCardView rootCard;

        VH(@NonNull View itemView) {
            super(itemView);
            rootCard = itemView.findViewById(R.id.rootCard);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvStageStatus = itemView.findViewById(R.id.tvStageStatus);
            tvRfidTag = itemView.findViewById(R.id.tvRfidTag);
            tvChangedAt = itemView.findViewById(R.id.tvChangedAt);
        }
    }
}
