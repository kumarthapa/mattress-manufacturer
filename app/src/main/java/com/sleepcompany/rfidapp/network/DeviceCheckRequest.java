package com.sleepcompany.rfidapp.network;

public class DeviceCheckRequest {
    private String device_id;

    public DeviceCheckRequest(String device_id) {
        this.device_id = device_id;
    }

    public String getDevice_id() { return device_id; }
    public void setDevice_id(String device_id) { this.device_id = device_id; }
}
