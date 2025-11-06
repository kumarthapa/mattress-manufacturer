package com.sleepcompany.rfidapp.model;

import com.google.gson.annotations.SerializedName;

public class RecentActivity {

    @SerializedName("product_id")
    public Integer product_id;

    @SerializedName("product_name")
    public String product_name;

    @SerializedName("rfid_tag")
    public String rfid_tag;

    @SerializedName("stage")
    public String stage;

    @SerializedName("status")
    public String status;

    @SerializedName("machine_no")
    public String machine_no;

    @SerializedName("comments")
    public String comments;

    @SerializedName("changed_at")
    public String changed_at; // consider Date if backend uses ISO8601
}
