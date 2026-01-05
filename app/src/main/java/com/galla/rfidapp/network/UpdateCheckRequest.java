package com.galla.rfidapp.network;

public class UpdateCheckRequest {
    private String device_id;
    private int current_version_code;

    public UpdateCheckRequest(String device_id, int current_version_code) {
        this.device_id = device_id;
        this.current_version_code = current_version_code;
    }

    // getters / setters (optional)
    public String getDevice_id() { return device_id; }
    public int getCurrent_version_code() { return current_version_code; }
}
