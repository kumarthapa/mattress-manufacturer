package com.sleepcompany.rfidapp.network;

import com.google.gson.annotations.SerializedName;

public class BondingProductsRequest {

    // For QA update
    @SerializedName("product_id")
    private int productId;

    @SerializedName("qa_code")
    private String qaCode;

    @SerializedName("rfid_tag")
    private String rfidTag;

    @SerializedName("lastCode")
    private String lastCode;

    // For search
    @SerializedName("search")
    private String search;

    @SerializedName("page")
    private int page = 1; // default
    @SerializedName("limit")
    private int limit = 20; // default

    // Default constructor
    public BondingProductsRequest() {}

    // Constructor for QA update
    public BondingProductsRequest(int productId, String qaCode, String rfidTag, String lastCode) {
        this.productId = productId;
        this.qaCode = qaCode;
        this.rfidTag = rfidTag;
        this.lastCode = lastCode;
    }

    // Getters and setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getQaCode() { return qaCode; }
    public void setQaCode(String qaCode) { this.qaCode = qaCode; }

    public String getRfidTag() { return rfidTag; }
    public void setRfidTag(String rfidTag) { this.rfidTag = rfidTag; }

    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }


    public void getLastCode(String lastCode) { this.lastCode = lastCode; }
    public void setLastCode(String lastCode) { this.lastCode = lastCode; }
}
