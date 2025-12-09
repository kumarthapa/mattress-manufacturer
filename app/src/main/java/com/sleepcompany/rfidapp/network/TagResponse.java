package com.sleepcompany.rfidapp.network;

import com.sleepcompany.rfidapp.model.ProductNetwork;

/**
 * API response for fetching product by tag ID.
 */
public class TagResponse {

    private boolean success;
    private String message;
    private ProductNetwork product;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public ProductNetwork getProduct() { return product; }
    public void setProduct(ProductNetwork product) { this.product = product; }
}
