package com.galla.rfidapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model class representing a Bonding Product returned by the bonding API.
 */
public class BondingProduct {

    private int id;

    @SerializedName("product_name")
    private String productName;

    private String model;
    private String serial_no;

    @SerializedName("qa_code")
    private String qaCode;

    private String sku;

    private String size;

    // store raw int from API (0/1)
    @SerializedName("is_write")
    private int isWrite;

    @SerializedName("write_by")
    private Integer writeBy; // Nullable

    @SerializedName("write_date")
    private String writeDate; // Can keep String for simplicity

    @SerializedName("rfid_tag")
    private String rfidTag;

    private int quantity;

    @SerializedName("created_at")
    private String createdAt;

    // ---------- Getters ----------

    public int getId() { return id; }
    public String getProductName() { return productName; }
    public String getModel() { return model; }
    public String getSerialNumber() { return serial_no; }
    public String getQaCode() { return qaCode; }
    public String getSku() { return sku; }
    public String getSize() { return size; }

    // Convenience: treat 1 as true, 0 as false
    public boolean isWritten() { return isWrite == 1; }
    public int getIsWriteRaw() { return isWrite; }

    public Integer getWriteBy() { return writeBy; }
    public String getWriteDate() { return writeDate; }
    public String getRfidTag() { return rfidTag; }
    public int getQuantity() { return quantity; }
    public String getCreatedAt() { return createdAt; }

    // ---------- Setters ----------

    public void setId(int id) { this.id = id; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setModel(String model) { this.model = model; }
    public void setQaCode(String qaCode) { this.qaCode = qaCode; }
    public void setSku(String sku) { this.sku = sku; }
    public void setSize(String size) { this.size = size; }
    public void setIsWrite(int isWrite) { this.isWrite = isWrite; }
    public void setWriteBy(Integer writeBy) { this.writeBy = writeBy; }
    public void setWriteDate(String writeDate) { this.writeDate = writeDate; }
    public void setRfidTag(String rfidTag) { this.rfidTag = rfidTag; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "BondingProduct{" +
                "id=" + id +
                ", productName='" + productName + '\'' +
                ", model='" + model + '\'' +
                ", model='" + serial_no + '\'' +
                ", qaCode='" + qaCode + '\'' +
                ", sku='" + sku + '\'' +
                ", size='" + size + '\'' +
                ", isWrite=" + isWrite +
                ", writeBy=" + writeBy +
                ", writeDate='" + writeDate + '\'' +
                ", rfidTag='" + rfidTag + '\'' +
                ", quantity=" + quantity +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
