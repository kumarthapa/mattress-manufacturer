package com.galla.rfidapp.network;

import com.google.gson.annotations.SerializedName;

public class LicenseResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    // top-level status (added)
    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private LicenseData data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public LicenseData getData() { return data; }
    public String getStatus() { return status; } // new getter

    public static class LicenseData {
        @SerializedName("device_id") public String deviceId;
        @SerializedName("license_key") public String licenseKey;
        @SerializedName("start_date") public String startDate;
        @SerializedName("end_date") public String endDate;
        @SerializedName("status") public String status; // may be null if server used top-level
    }
}
