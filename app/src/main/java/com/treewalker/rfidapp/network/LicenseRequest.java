
package com.treewalker.rfidapp.network;

import com.google.gson.annotations.SerializedName;

public class LicenseRequest {
    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("license_key")
    private String licenseKey;

    @SerializedName("serial_number")
    private String serialNumber;

    public LicenseRequest(String deviceId, String licenseKey, String serialNumber) {
        this.deviceId = deviceId;
        this.licenseKey = licenseKey;
        this.serialNumber = serialNumber;
    }
    // getters/setters if needed
}
