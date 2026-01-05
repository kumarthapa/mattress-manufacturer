package com.galla.rfidapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Single recent activity entry as returned by the API.
 */
public class RecentActivity {
    @SerializedName("product_id")
    public Integer productId;

    @SerializedName("product_name")
    public String productName;

    @SerializedName("rfid_tag")
    public String rfidTag;

    // 'stage' value may still be returned by API for historical reasons; keep it if present.
    @SerializedName("stage")
    public String stage;

    @SerializedName("status")
    public String status;

    @SerializedName("defects")
    public Integer defects;

    @SerializedName("remarks")
    public String remarks;

    // Server returns ISO datetime string; keep as String for display/parsing later.
    @SerializedName("changed_at")
    public String changedAt;
}
