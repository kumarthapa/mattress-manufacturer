package com.galla.rfidapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButtonToggleGroup;
import android.widget.Toast; // add to imports
public class StockAuditActivity extends BaseDrawerActivity {

    // Toggle
    private MaterialButtonToggleGroup toggleGroup;

    // Scan input
    private EditText etScanCode;
    private Button btnCam;

    // INCLUDED ITEM ROOTS
    private View item1, item2, item3;

    // Item 1 views
    private TextView tvItem1Name, tvItem1Barcode, tvItem1SystemQty, tvItem1PhysicalQty;
    private Button btnItem1Plus, btnItem1Minus;

    // Item 2 views
    private TextView tvItem2Name, tvItem2Barcode, tvItem2SystemQty, tvItem2PhysicalQty;
    private Button btnItem2Plus, btnItem2Minus;

    // Item 3 views
    private TextView tvItem3Name, tvItem3Barcode, tvItem3SystemQty, tvItem3PhysicalQty;
    private Button btnItem3Plus, btnItem3Minus;

    // Action buttons
    private Button btnClear, btnSave;

    // Qty values
    private int item1Qty = 5;
    private int item2Qty = 3;
    private int item3Qty = 2;
    // UI toggles (new)
    private View barcodeControls;
    private Button btnStartRfid;
    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_stock_audit;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViews();
        loadHardcodedData();
        setupActions();
        setupModeToggle(); // <-- attach toggle listener after views are initialized
    }

    private void initViews() {

        // Toggle
        toggleGroup = findViewById(R.id.toggleGroupAudit);

        // Scan
        etScanCode = findViewById(R.id.etScanCode);
        btnCam = findViewById(R.id.btnCam);

        // INCLUDED ITEM ROOTS
        item1 = findViewById(R.id.item1);
        item2 = findViewById(R.id.item2);
        item3 = findViewById(R.id.item3); // <-- fixed: previously used item2 for item3

        // ITEM 1
        tvItem1Name = item1.findViewById(R.id.tvItemName);
        tvItem1Barcode = item1.findViewById(R.id.tvItemBarcode);
        tvItem1SystemQty = item1.findViewById(R.id.tvItemSystemQty);
        tvItem1PhysicalQty = item1.findViewById(R.id.tvItemPhysicalQty);
        btnItem1Plus = item1.findViewById(R.id.btnPlus);
        btnItem1Minus = item1.findViewById(R.id.btnMinus);

        // ITEM 2
        tvItem2Name = item2.findViewById(R.id.tvItemName);
        tvItem2Barcode = item2.findViewById(R.id.tvItemBarcode);
        tvItem2SystemQty = item2.findViewById(R.id.tvItemSystemQty);
        tvItem2PhysicalQty = item2.findViewById(R.id.tvItemPhysicalQty);
        btnItem2Plus = item2.findViewById(R.id.btnPlus);
        btnItem2Minus = item2.findViewById(R.id.btnMinus);

        // ITEM 3
        tvItem3Name = item3.findViewById(R.id.tvItemName);
        tvItem3Barcode = item3.findViewById(R.id.tvItemBarcode);
        tvItem3SystemQty = item3.findViewById(R.id.tvItemSystemQty);
        tvItem3PhysicalQty = item3.findViewById(R.id.tvItemPhysicalQty);
        btnItem3Plus = item3.findViewById(R.id.btnPlus);
        btnItem3Minus = item3.findViewById(R.id.btnMinus);

        // Buttons
        btnClear = findViewById(R.id.btnClear);
        btnSave = findViewById(R.id.btnSave);

        // after etScanCode and btnCam:
        barcodeControls = findViewById(R.id.barcodeControls);
        btnStartRfid = findViewById(R.id.btnStartRfid);

// placeholder click until you add UHF logic later
        btnStartRfid.setOnClickListener(v ->
                Toast.makeText(this, "RFID scanning will be added later", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * HARD-CODED STOCK DATA
     */
    private void loadHardcodedData() {

        // ITEM 1
        tvItem1Name.setText("Potato Chips");
        tvItem1Barcode.setText("123456677");
        tvItem1SystemQty.setText("System Stock : 100");
        tvItem1PhysicalQty.setText(String.valueOf(item1Qty));

        // ITEM 2
        tvItem2Name.setText("Mango Juice");
        tvItem2Barcode.setText("123456678");
        tvItem2SystemQty.setText("System Stock : 80");
        tvItem2PhysicalQty.setText(String.valueOf(item2Qty));

        // ITEM 3
        tvItem3Name.setText("Parle Ji");
        tvItem3Barcode.setText("123456679");
        tvItem3SystemQty.setText("System Stock : 100");
        tvItem3PhysicalQty.setText(String.valueOf(item3Qty));
    }

    private void setupActions() {

        // ITEM 1 + / -
        btnItem1Plus.setOnClickListener(v -> {
            item1Qty++;
            tvItem1PhysicalQty.setText(String.valueOf(item1Qty));
        });

        btnItem1Minus.setOnClickListener(v -> {
            if (item1Qty > 0) {
                item1Qty--;
                tvItem1PhysicalQty.setText(String.valueOf(item1Qty));
            }
        });

        // ITEM 2 + / -
        btnItem2Plus.setOnClickListener(v -> {
            item2Qty++;
            tvItem2PhysicalQty.setText(String.valueOf(item2Qty));
        });

        btnItem2Minus.setOnClickListener(v -> {
            if (item2Qty > 0) {
                item2Qty--;
                tvItem2PhysicalQty.setText(String.valueOf(item2Qty));
            }
        });

        // ITEM 3 + / -
        btnItem3Plus.setOnClickListener(v -> {
            item3Qty++;
            tvItem3PhysicalQty.setText(String.valueOf(item3Qty));
        });

        btnItem3Minus.setOnClickListener(v -> {
            if (item3Qty > 0) {
                item3Qty--;
                tvItem3PhysicalQty.setText(String.valueOf(item3Qty));
            }
        });

        // CLEAR
        btnClear.setOnClickListener(v -> {
            etScanCode.setText("");
            item1Qty = 0;
            item2Qty = 0;
            item3Qty = 0;
            tvItem1PhysicalQty.setText("0");
            tvItem2PhysicalQty.setText("0");
            tvItem3PhysicalQty.setText("0");
        });

        // SAVE
        btnSave.setOnClickListener(v -> {
            // Later:
            // - Save to DB
            // - Send to API
            // - Calculate variance
        });
    }

    /**
     * Toggle handler for Barcode <-> RFID
     */
    private void setupModeToggle() {
        // Ensure there is a default selection (Barcode) in the toggle group in your XML.
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return; // only handle when a button becomes checked

            if (checkedId == R.id.btnRfid) {
                // RFID mode activated
                setRfidMode(true);
            } else if (checkedId == R.id.btnBarcode) {
                // Barcode mode activated
                setRfidMode(false);
            }
        });

        // initialize UI according to default selection
        // If Barcode is default selected in layout, call setRfidMode(false)
        setRfidMode(false);
    }

    /**
     * Enable/disable UI elements based on RFID mode
     * @param rfid true = RFID mode, false = Barcode mode
     */
    private void setRfidMode(boolean rfid) {

        // Toggle scan UI
        if (barcodeControls != null)
            barcodeControls.setVisibility(rfid ? View.GONE : View.VISIBLE);

        if (btnStartRfid != null)
            btnStartRfid.setVisibility(rfid ? View.VISIBLE : View.GONE);

        // Barcode input enable / disable
        if (etScanCode != null) {
            etScanCode.setEnabled(!rfid);
            etScanCode.setFocusable(!rfid);
            etScanCode.setFocusableInTouchMode(!rfid);
            etScanCode.setHint(rfid ? "RFID scanning active" : "Scan barcode here");
        }

        if (btnCam != null)
            btnCam.setEnabled(!rfid);

        // Hide + / - buttons during RFID
        int qtyVisibility = rfid ? View.GONE : View.VISIBLE;

        if (btnItem1Plus != null) btnItem1Plus.setVisibility(qtyVisibility);
        if (btnItem1Minus != null) btnItem1Minus.setVisibility(qtyVisibility);

        if (btnItem2Plus != null) btnItem2Plus.setVisibility(qtyVisibility);
        if (btnItem2Minus != null) btnItem2Minus.setVisibility(qtyVisibility);

        if (btnItem3Plus != null) btnItem3Plus.setVisibility(qtyVisibility);
        if (btnItem3Minus != null) btnItem3Minus.setVisibility(qtyVisibility);

        // Visual cue: qty is read-only in RFID
        if (tvItem1PhysicalQty != null) tvItem1PhysicalQty.setEnabled(!rfid);
        if (tvItem2PhysicalQty != null) tvItem2PhysicalQty.setEnabled(!rfid);
        if (tvItem3PhysicalQty != null) tvItem3PhysicalQty.setEnabled(!rfid);
    }


}
