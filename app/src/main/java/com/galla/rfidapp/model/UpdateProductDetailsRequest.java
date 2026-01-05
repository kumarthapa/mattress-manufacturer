package com.galla.rfidapp.model;

import com.google.gson.annotations.SerializedName;

public class UpdateProductDetailsRequest {
    @SerializedName("tag_id")
    private String tagId;

    @SerializedName("product_name")
    private String productName;

    // NEW: optional sku field (null when not updating sku)
    @SerializedName("sku")
    private String sku;

    // Constructors

    // new constructor to pass both productName and sku (either may be null)
    public UpdateProductDetailsRequest(String tagId, String productName, String sku) {
        this.tagId = tagId;
        this.productName = productName;
        this.sku = sku;
    }

    public String getTagId() { return tagId; }
    public String getProductName() { return productName; }
    public String getSku() { return sku; }

    public void setTagId(String tagId) { this.tagId = tagId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setSku(String sku) { this.sku = sku; }
}
