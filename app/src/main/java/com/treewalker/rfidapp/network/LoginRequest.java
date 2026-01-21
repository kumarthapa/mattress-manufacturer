package com.treewalker.rfidapp.network;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("license_key")
    private String licenseKey;

    @SerializedName("username")
    private String username;

    @SerializedName("password")
    private String password;

    public LoginRequest(String licenseKey, String username, String password) {
        this.licenseKey = licenseKey;
        this.username = username;
        this.password = password;
    }

    // getters (Retrofit/Gson uses reflection but getters useful in code)
    public String getLicenseKey() { return licenseKey; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    // setters if needed
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
}
