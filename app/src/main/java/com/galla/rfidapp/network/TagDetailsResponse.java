package com.galla.rfidapp.network;

import com.google.gson.annotations.SerializedName;
import com.galla.rfidapp.model.ProductNetwork;

public class TagDetailsResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Data data;

    // ------------ Inner Data Wrapper ------------ //
    public static class Data {

        @SerializedName("tag")
        private Tag tag;

        @SerializedName("product")
        private ProductNetwork product;   // <-- REUSE your existing Product model

        public Tag getTag() {
            return tag;
        }

        public ProductNetwork getProduct() {
            return product;
        }
    }

    // ------------ Tag Model ------------ //
    public static class Tag {

        @SerializedName("id")
        private int id;

        @SerializedName("epc_code")
        private String epcCode;

        @SerializedName("tag_code")
        private String tagCode;

        @SerializedName("product_id")
        private Integer productId;

        @SerializedName("location_id")
        private Integer locationId;

        @SerializedName("status")
        private String status;

        @SerializedName("last_scanned_at")
        private String lastScannedAt;

        // Getters
        public int getId() { return id; }
        public String getEpcCode() { return epcCode; }
        public String getTagCode() { return tagCode; }
        public Integer getProductId() { return productId; }
        public Integer getLocationId() { return locationId; }
        public String getStatus() { return status; }
        public String getLastScannedAt() { return lastScannedAt; }
    }

    // ------------ Outer Getters ------------ //
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Data getData() { return data; }
}
