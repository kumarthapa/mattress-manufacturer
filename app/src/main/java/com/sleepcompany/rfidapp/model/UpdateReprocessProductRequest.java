package com.sleepcompany.rfidapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * UpdateReprocessProductRequest
 *
 * Used to update editable product fields during Reprocess flow.
 * All fields are OPTIONAL except tag_id.
 * Backend should update only non-null fields.
 */
public class UpdateReprocessProductRequest {

    // ======================
    // REQUIRED
    // ======================
    @SerializedName("tag_id")
    private String tagId;

    // ======================
    // OPTIONAL FIELDS
    // ======================
    @SerializedName("product_name")
    private String productName;

    @SerializedName("sku")
    private String sku;

    @SerializedName("qa_code")
    private String qaCode;

    @SerializedName("size")
    private String size;

    @SerializedName("model")
    private String model;

    @SerializedName("bonding_plan_product_id")
    private Long bondingPlanProductId;

    // ✅ Defect points
    @SerializedName("defects_points")
    private List<String> defectsPoints;

    // ✅ NEW: Remarks
    @SerializedName("remarks")
    private String remarks;

    // ======================
    // CONSTRUCTORS
    // ======================

    /**
     * ✅ FULL constructor (ALL fields)
     */
    public UpdateReprocessProductRequest(
            String tagId,
            String productName,
            String sku,
            String qaCode,
            String size,
            String model,
            Long bondingPlanProductId,
            List<String> defectsPoints,
            String remarks
    ) {
        this.tagId = tagId;
        this.productName = productName;
        this.sku = sku;
        this.qaCode = qaCode;
        this.size = size;
        this.model = model;
        this.bondingPlanProductId = bondingPlanProductId;
        this.defectsPoints = defectsPoints;
        this.remarks = remarks;
    }

    /**
     * 🔄 Without defects & remarks
     */
    public UpdateReprocessProductRequest(
            String tagId,
            String productName,
            String sku,
            String qaCode,
            String size,
            String model,
            Long bondingPlanProductId
    ) {
        this(tagId, productName, sku, qaCode, size, model, bondingPlanProductId, null, null);
    }

    // ======================
    // GETTERS
    // ======================

    public String getTagId() {
        return tagId;
    }

    public String getProductName() {
        return productName;
    }

    public String getSku() {
        return sku;
    }

    public String getQaCode() {
        return qaCode;
    }

    public String getSize() {
        return size;
    }

    public String getModel() {
        return model;
    }

    public Long getBondingPlanProductId() {
        return bondingPlanProductId;
    }

    public List<String> getDefectsPoints() {
        return defectsPoints;
    }

    public String getRemarks() {
        return remarks;
    }

    // ======================
    // SETTERS
    // ======================

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setQaCode(String qaCode) {
        this.qaCode = qaCode;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setBondingPlanProductId(Long bondingPlanProductId) {
        this.bondingPlanProductId = bondingPlanProductId;
    }

    public void setDefectsPoints(List<String> defectsPoints) {
        this.defectsPoints = defectsPoints;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
