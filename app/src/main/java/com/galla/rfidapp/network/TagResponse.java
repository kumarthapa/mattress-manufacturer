package com.galla.rfidapp.network;

import com.galla.rfidapp.model.AssetNetwork;

/**
 * API response for fetching product by tag ID.
 */
public class TagResponse {

    private boolean success;
    private String message;
    private AssetNetwork product;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public AssetNetwork getProduct() { return product; }
    public void setProduct(AssetNetwork product) { this.product = product; }
}
