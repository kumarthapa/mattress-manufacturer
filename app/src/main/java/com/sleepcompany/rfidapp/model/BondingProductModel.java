package com.sleepcompany.rfidapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * BondingProductModel
 *
 * Represents a Product returned by backend APIs.
 * Used across:
 *  - RFID Scan
 *  - Reprocess flow
 *  - Stage update
 *  - Printing
 *  - Model (QA) selection
 */
public class BondingProductModel {

    // ====================
    // BASIC IDENTIFIERS
    // ====================

    @SerializedName("id")
    private String id;

    @SerializedName("tag_id")
    private String tagId;

    /**
     * ✅ REQUIRED for Reprocess save flow
     */
    @SerializedName("bonding_plan_product_id")
    private Long bondingPlanProductId;

    // ====================
    // PRODUCT INFORMATION
    // ====================

    @SerializedName("product_name")
    private String productName;

    /**
     * Selected from Model popup
     */
    @SerializedName("model")
    private String model;

    @SerializedName("sku")
    private String sku;

    @SerializedName("reference_code")
    private String referenceCode;

    @SerializedName("size")
    private String size;

    @SerializedName("qa_code")
    private String qaCode;

    @SerializedName("quantity")
    private int quantity;

    // ====================
    // PROCESS / QC STATUS
    // ====================

    /**
     * Legacy QC status
     */
    @SerializedName("status")
    private String status;

    /**
     * Legacy stage
     */
    @SerializedName("stage")
    private String stage;

    /**
     * ✅ PRIMARY stage field
     */
    @SerializedName("latest_stage")
    private String latestStage;

    /**
     * ✅ PRIMARY QC status field
     */
    @SerializedName("latest_status")
    private String latestStatus;

    @SerializedName("latest_remarks")
    private String latestRemarks;

    @SerializedName("latest_defects_points")
    private List<String> latestDefectsPoints;

    // ====================
    // META
    // ====================

    @SerializedName("created_at")
    private String createdAt;

    // ====================
    // CONSTRUCTOR
    // ====================

    /**
     * Required empty constructor for Gson
     */
    public BondingProductModel() {
    }

    // ====================
    // GETTERS
    // ====================

    public String getId() {
        return id;
    }

    public String getTagId() {
        return tagId;
    }

    public Long getBondingPlanProductId() {
        return bondingPlanProductId;
    }

    public String getProductName() {
        return productName;
    }

    public String getModel() {
        return model;
    }

    public String getSku() {
        return sku;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public String getSize() {
        return size;
    }

    public String getQAcode() {
        return qaCode;
    }

    public int getQuantity() {
        return quantity;
    }

    /**
     * Legacy QC status (avoid in new code)
     */
    public String getQcStatus() {
        return status;
    }

    /**
     * Legacy stage (avoid in new code)
     */
    public String getStage() {
        return stage;
    }

    public String getLatestStage() {
        return latestStage;
    }

    public String getLatestStatus() {
        return latestStatus;
    }

    public String getLatestRemarks() {
        return latestRemarks;
    }

    public List<String> getLatestDefectsPoints() {
        return latestDefectsPoints;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // ====================
    // SETTERS
    // ====================

    public void setId(String id) {
        this.id = id;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    /**
     * ✅ FIX: required by ReprocessActivity
     */
    public void setBondingPlanProductId(Long bondingPlanProductId) {
        this.bondingPlanProductId = bondingPlanProductId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setQAcode(String qaCode) {
        this.qaCode = qaCode;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setQcStatus(String status) {
        this.status = status;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public void setLatestStage(String latestStage) {
        this.latestStage = latestStage;
    }

    public void setLatestStatus(String latestStatus) {
        this.latestStatus = latestStatus;
    }

    public void setLatestRemarks(String latestRemarks) {
        this.latestRemarks = latestRemarks;
    }

    public void setLatestDefectsPoints(List<String> latestDefectsPoints) {
        this.latestDefectsPoints = latestDefectsPoints;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
