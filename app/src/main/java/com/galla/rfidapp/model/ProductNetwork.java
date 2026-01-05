package com.galla.rfidapp.model;

import com.google.gson.annotations.SerializedName;

public class ProductNetwork {

    @SerializedName("id")
    private int id;

    @SerializedName("product_name")
    private String productName;

    @SerializedName("product_code")
    private String productCode;

    @SerializedName("category")
    private String category;

    @SerializedName("price")
    private Double price;

    @SerializedName("expected_life_cycles")
    private Integer expectedLifeCycles;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("last_activity")
    private LastActivity lastActivity;

    // ------------------- INNER CLASS -------------------
    public static class LastActivity {

        @SerializedName("trans_type")
        private String transType;

        @SerializedName("inward")
        private int inward;

        @SerializedName("outward")
        private int outward;

        @SerializedName("opening_stock")
        private int openingStock;

        @SerializedName("closing_stock")
        private int closingStock;

        // backend field "at"
        @SerializedName("at")
        private String activityDate;

        // Getters
        public String getTransType() { return transType; }
        public int getInward() { return inward; }
        public int getOutward() { return outward; }
        public int getOpeningStock() { return openingStock; }
        public int getClosingStock() { return closingStock; }

        // Primary getter name used by adapters: getAt()
        public String getAt() { return activityDate; }

        // Backwards-compatible getter name (optional)
        public String getActivityDate() { return activityDate; }
    }

    // ------------------- GETTERS -------------------

    public int getId() { return id; }

    public String getProductName() { return productName; }

    public String getProductCode() { return productCode; }

    public String getCategory() { return category; }

    public Double getPrice() { return price; }

    public Integer getExpectedLifeCycles() { return expectedLifeCycles; }

    public int getQuantity() { return quantity; }

    public LastActivity getLastActivity() { return lastActivity; }
}
