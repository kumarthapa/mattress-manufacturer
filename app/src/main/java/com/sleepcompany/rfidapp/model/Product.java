package com.sleepcompany.rfidapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model class representing a Product as returned by the backend API.
 * Includes current stage, QC status, and latest process history.
 */
public class Product {
    @SerializedName("tag_id")
    private String tagId;
    private String id;
    private String product_name;
    private String sku;
    private String size;
    private int quantity;
    private String qc_status;
    private String current_stage;

    // Latest process info (from product_process_history)
    private String latest_stage;
    private String latest_status;
    private String latest_machine_no;
    private String latest_comments;

    private String created_at;

    /** Empty constructor required for Retrofit/Gson JSON mapping */
    public Product() {}

    /**
     * Full constructor for manual instantiation if needed.
     */
    public Product(String id, String product_name, String sku, String size, int quantity,
                   String qc_status, String current_stage,
                   String latest_stage, String latest_status, String latest_machine_no, String latest_comments,
                   String created_at) {
        this.id = id;
        this.product_name = product_name;
        this.sku = sku;
        this.size = size;
        this.quantity = quantity;
        this.qc_status = qc_status;
        this.current_stage = current_stage;
        this.latest_stage = latest_stage;
        this.latest_status = latest_status;
        this.latest_machine_no = latest_machine_no;
        this.latest_comments = latest_comments;
        this.created_at = created_at;
    }

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductName() { return product_name; }
    public void setProductName(String product_name) { this.product_name = product_name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getQcStatus() { return qc_status; }
    public void setQcStatus(String qc_status) { this.qc_status = qc_status; }

    public String getCurrentStage() { return current_stage; }
    public void setCurrentStage(String current_stage) { this.current_stage = current_stage; }

    public String getLatestStage() { return latest_stage; }
    public void setLatestStage(String latest_stage) { this.latest_stage = latest_stage; }

    public String getLatestStatus() { return latest_status; }
    public void setLatestStatus(String latest_status) { this.latest_status = latest_status; }

    public String getLatestMachineNo() { return latest_machine_no; }
    public void setLatestMachineNo(String latest_machine_no) { this.latest_machine_no = latest_machine_no; }

    public String getLatestComments() { return latest_comments; }
    public void setLatestComments(String latest_comments) { this.latest_comments = latest_comments; }

    public String getCreatedAt() { return created_at; }
    public void setCreatedAt(String created_at) { this.created_at = created_at; }

    private String rfid_tag;  // Add this field to hold the RFID tag (use the same name as backend JSON)

    // Add rfid_tag getter and setter


    public String getTagId() {
        return tagId;
    }
    public void setTagId(String tagId) {
        this.tagId = tagId;
    }
}
