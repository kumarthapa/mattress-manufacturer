package com.sleepcompany.rfidapp.network;

import com.sleepcompany.rfidapp.model.Product;

/**
 * API response for fetching product by tag ID.
 */
public class TagResponse {

    private boolean success;
    private String message;
    private Product product;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
