package com.treewalker.rfidapp.network;

public class CreateLicenseRequest {
    private String device_id;
    private String end_date; // ISO date string (yyyy-MM-dd) or your preferred format
    private String status;

    public CreateLicenseRequest(String device_id, String end_date, String status) {
        this.device_id = device_id;
        this.end_date = end_date;
        this.status = status;
    }

    // getters & setters (if needed by Gson/Retrofit)
    public String getDevice_id() { return device_id; }
    public void setDevice_id(String device_id) { this.device_id = device_id; }
    public String getEnd_date() { return end_date; }
    public void setEnd_date(String end_date) { this.end_date = end_date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
