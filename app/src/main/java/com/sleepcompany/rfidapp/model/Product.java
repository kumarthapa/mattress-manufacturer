package com.sleepcompany.rfidapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents a Product as returned by backend API.
 * Includes latest process history and defect points.
 */
public class Product {

    @SerializedName("tag_id")
    private String tagId;

    private String id;
    private String product_name;
    private String sku;
    private String size;
    private String qa_code;
    private int quantity;
    private String status;

    // Latest process info (from product_process_history)
    private String stage;
    private String latest_stage;
    private String latest_status;
    private String latest_remarks;

    // Latest defect points
    @SerializedName("latest_defects_points")
    private List<String> latestDefectsPoints;

    private String created_at;

    public Product(String id, String product_name, String sku, String size, int quantity,
                   String latest_stage,String stage, String latest_status,String qa_code,
                   String latest_remarks,
                   List<String> latestDefectsPoints, String tagId, String created_at) {
        this.id = id;
        this.product_name = product_name;
        this.sku = sku;
        this.size = size;
        this.qa_code = qa_code;
        this.quantity = quantity;
        this.latest_stage = latest_stage;
        this.stage = stage;
        this.latest_status = latest_status;
        this.latest_remarks = latest_remarks;
        this.latestDefectsPoints = latestDefectsPoints;
        this.tagId = tagId;
        this.created_at = created_at;
    }

    // Getters and setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductName() { return product_name; }
    public void setProductName(String product_name) { this.product_name = product_name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getSize() { return size; }
    public String getQAcode() { return qa_code; }

    public void setSize(String size) { this.size = size; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getQcStatus() { return status; }
    public void setQcStatus(String status) { this.status = status; }

    public String getLatestStage() { return latest_stage; }
    public String getStage() { return stage; }
    public void setLatestStage(String latest_stage) { this.latest_stage = latest_stage; }

    public String getLatestStatus() { return latest_status; }
    public void setLatestStatus(String latest_status) { this.latest_status = latest_status; }

    public String getLatestRemarks() { return latest_remarks; }
    public void setLatestRemarks(String latest_remarks) { this.latest_remarks = latest_remarks; }

    public List<String> getLatestDefectsPoints() { return latestDefectsPoints; }
    public void setLatestDefectsPoints(List<String> latestDefectsPoints) { this.latestDefectsPoints = latestDefectsPoints; }

    public String getTagId() { return tagId; }
    public void setTagId(String tagId) { this.tagId = tagId; }

    public String getCreatedAt() { return created_at; }
    public void setCreatedAt(String created_at) { this.created_at = created_at; }
}
