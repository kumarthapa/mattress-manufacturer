package com.sleepcompany.rfidapp.network;

import com.sleepcompany.rfidapp.model.BondingProductModel;

/**
 * API response for fetching product by tag ID.
 */
public class ReprocessTagResponse {

    private boolean success;
    private String message;
    private BondingProductModel product;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public BondingProductModel getProduct() { return product; }
    public void setProduct(BondingProductModel product) { this.product = product; }
}
