package com.sleepcompany.rfidapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class QcActivity extends AppCompatActivity {

    private Spinner qcProductSpinner;
    private LinearLayout qcChecklistContainer, qcItemsLayout;
    private Button approveQCBtn, rejectQCBtn;

    // Sample products and QC items (replace with your real data)
    private String[] products = {"Select Product for QC", "Premium Memory Foam Mattress", "Hybrid Spring Mattress", "Organic Cotton Mattress"};
    private String[] qcItems = {
            "Dimensions accurate to specification",
            "No visible defects or tears",
            "Proper firmness level",
            "Correct labeling and tags",
            "Clean and free of debris",
            "RFID tag properly embedded"
    };

    private List<CheckBox> qcCheckBoxes = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qc);

        qcProductSpinner = findViewById(R.id.qcProductSpinner);
        qcChecklistContainer = findViewById(R.id.qcChecklistContainer);
        qcItemsLayout = findViewById(R.id.qcItemsLayout);
        approveQCBtn = findViewById(R.id.approveQCBtn);
        rejectQCBtn = findViewById(R.id.rejectQCBtn);

        // Setup product spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, products);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        qcProductSpinner.setAdapter(adapter);

        qcProductSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    qcChecklistContainer.setVisibility(View.GONE);
                } else {
                    qcChecklistContainer.setVisibility(View.VISIBLE);
                    populateQcChecklist();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                qcChecklistContainer.setVisibility(View.GONE);
            }
        });

        approveQCBtn.setOnClickListener(v -> {
            if (isChecklistApproved()) {
                Toast.makeText(QcActivity.this, "QC Approved", Toast.LENGTH_SHORT).show();
                // TODO: Handle approval logic
            } else {
                Toast.makeText(QcActivity.this, "Please complete all required QC checks", Toast.LENGTH_SHORT).show();
            }
        });

        rejectQCBtn.setOnClickListener(v -> {
            Toast.makeText(QcActivity.this, "QC Rejected", Toast.LENGTH_SHORT).show();
            // TODO: Handle rejection logic
        });
    }

    private void populateQcChecklist() {
        qcItemsLayout.removeAllViews();
        qcCheckBoxes.clear();

        for (String item : qcItems) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(item);
            qcItemsLayout.addView(checkBox);
            qcCheckBoxes.add(checkBox);
        }
    }

    private boolean isChecklistApproved() {
        for (CheckBox cb : qcCheckBoxes) {
            if (!cb.isChecked()) return false;
        }
        return true;
    }
}
