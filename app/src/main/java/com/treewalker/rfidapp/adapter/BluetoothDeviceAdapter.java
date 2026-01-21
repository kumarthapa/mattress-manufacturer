package com.treewalker.rfidapp.adapter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.treewalker.rfidapp.R;

import java.util.ArrayList;
import java.util.List;


public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.VH> {

    public interface Callback {
        void onPairRequested(BluetoothDevice device);
        void onConnectRequested(BluetoothDevice device);
    }

    private final List<BluetoothDevice> devices = new ArrayList<>();
    private final List<Boolean> paired = new ArrayList<>();
    private final Callback callback;

    // NEW: track selected device mac (saved in SharedPreferences by Activity)
    private String selectedMac = null;

    public BluetoothDeviceAdapter(Callback callback) {
        this.callback = callback;
    }

    // NEW: setter to inform adapter about currently selected device
    @SuppressLint("NotifyDataSetChanged")
    public void setSelectedMac(String mac) {
        this.selectedMac = mac;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<BluetoothDevice> deviceList) {
        devices.clear();
        paired.clear();
        if (deviceList != null) {
            for (BluetoothDevice d : deviceList) {
                devices.add(d);
                paired.add(d.getBondState() == BluetoothDevice.BOND_BONDED);
            }
        }
        notifyDataSetChanged();
    }

    public void updateDevice(BluetoothDevice device) {
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getAddress().equals(device.getAddress())) {
                devices.set(i, device);
                paired.set(i, device.getBondState() == BluetoothDevice.BOND_BONDED);
                notifyItemChanged(i);
                return;
            }
        }
        // if not present, add
        devices.add(device);
        paired.add(device.getBondState() == BluetoothDevice.BOND_BONDED);
        notifyItemInserted(devices.size() - 1);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_device, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BluetoothDevice device = devices.get(position);
        boolean isPaired = paired.get(position);
        boolean isSelected = device.getAddress() != null && device.getAddress().equals(selectedMac);

        holder.tvName.setText(device.getName() == null ? "Unknown" : device.getName());
        holder.tvMac.setText(device.getAddress());
        holder.tvPaired.setVisibility(isPaired ? View.VISIBLE : View.GONE);

        // If this device was previously selected and it is paired, disable action and mark it as Selected
        if (isSelected && isPaired) {
            holder.btnAction.setText("Selected");
            holder.btnAction.setEnabled(false);
        } else {
            // normal states
            holder.btnAction.setEnabled(true);
            if (isPaired) holder.btnAction.setText("Connect");
            else holder.btnAction.setText("Pair");
        }

        holder.btnAction.setOnClickListener(v -> {
            // re-evaluate states at click time
            boolean pairedNow = device.getBondState() == BluetoothDevice.BOND_BONDED;
            boolean selectedNow = device.getAddress() != null && device.getAddress().equals(selectedMac);

            if (selectedNow && pairedNow) {
                // already selected & paired -> no action (button should be disabled already)
                return;
            }

            if (pairedNow) {
                callback.onConnectRequested(device);
            } else {
                callback.onPairRequested(device);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            // short tap will also perform same as action except for already-selected+paired
            boolean pairedNow = device.getBondState() == BluetoothDevice.BOND_BONDED;
            boolean selectedNow = device.getAddress() != null && device.getAddress().equals(selectedMac);
            if (selectedNow && pairedNow) return; // do nothing
            if (pairedNow) callback.onConnectRequested(device);
            else callback.onPairRequested(device);
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvMac, tvPaired;
        MaterialButton btnAction;

        public VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDeviceName);
            tvMac = itemView.findViewById(R.id.tvDeviceMac);
            tvPaired = itemView.findViewById(R.id.tvPaired);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}