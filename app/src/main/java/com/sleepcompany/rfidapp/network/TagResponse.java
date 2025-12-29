package com.sleepcompany.rfidapp.network;

import com.google.gson.annotations.SerializedName;
import com.sleepcompany.rfidapp.model.BondingProductModel;
import com.sleepcompany.rfidapp.model.Product;

/**
 * API response for fetching product by tag ID.
 * JSON always returns:
 *
 * {
 *   "success": true,
 *   "message": "...",
 *   "product": { ... }
 * }
 */
public class TagResponse {

    private boolean success;
    private String message;

    // ✅ ONLY JSON-MAPPED FIELD
    @SerializedName("product")
    private BondingProductModel bondingProductModel;

    // ❌ NOT mapped by Gson (derived manually)
    private transient Product legacyProduct;

    // ---------------- BASIC ----------------

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    // =========================
    // REPROCESS FLOW (PRIMARY)
    // =========================
    public BondingProductModel getReprocessProduct() {
        return bondingProductModel;
    }

    // =========================
    // LEGACY FLOW (DERIVED)
    // =========================
    public Product getProduct() {
        if (legacyProduct != null) return legacyProduct;
        if (bondingProductModel == null) return null;

        // 🔁 Convert BondingProductModel → Product
        legacyProduct = new Product(
                bondingProductModel.getId(),
                bondingProductModel.getProductName(),
                bondingProductModel.getSku(),
                bondingProductModel.getSize(),
                bondingProductModel.getQuantity(),
                bondingProductModel.getLatestStage(),
                bondingProductModel.getLatestStage(), // stage
                bondingProductModel.getLatestStatus(),
                bondingProductModel.getQAcode(),
                bondingProductModel.getLatestRemarks(),
                bondingProductModel.getReferenceCode(),
                bondingProductModel.getLatestDefectsPoints(),
                bondingProductModel.getTagId(),
                bondingProductModel.getCreatedAt()
        );

        return legacyProduct;
    }
}
