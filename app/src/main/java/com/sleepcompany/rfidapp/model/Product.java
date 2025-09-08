package com.sleepcompany.rfidapp.model;

/**
 * Model class representing a Product as returned by the backend API.
 */
public class Product {
    private String id;
    private String product_name;
    private String sku;
    private String size;
    private int quantity;
    private String qc_status;
    private String created_at;

    /** Empty constructor required for Retrofit/Gson JSON mapping */
    public Product() {}

    /**
     * Full constructor for manual instantiation if needed.
     *
     * @param id            Unique product ID
     * @param product_name  Name of the product
     * @param sku           SKU code
     * @param size          Product size
     * @param quantity      Quantity available
     * @param qc_status     QC status (PASS, FAILED, PENDING)
     * @param created_at    Timestamp when created
     */
    public Product(String id,
                   String product_name,
                   String sku,
                   String size,
                   int quantity,
                   String qc_status,
                   String created_at) {
        this.id = id;
        this.product_name = product_name;
        this.sku = sku;
        this.size = size;
        this.quantity = quantity;
        this.qc_status = qc_status;
        this.created_at = created_at;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductName() {
        return product_name;
    }

    public void setProductName(String product_name) {
        this.product_name = product_name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getQcStatus() {
        return qc_status;
    }

    public void setQcStatus(String qc_status) {
        this.qc_status = qc_status;
    }

    public String getCreatedAt() {
        return created_at;
    }

    public void setCreatedAt(String created_at) {
        this.created_at = created_at;
    }
}
