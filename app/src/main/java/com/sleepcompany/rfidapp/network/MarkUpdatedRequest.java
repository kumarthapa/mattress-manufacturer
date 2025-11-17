package com.sleepcompany.rfidapp.network;

public class MarkUpdatedRequest {
    private String device_id;
    private int updated_version;

    public MarkUpdatedRequest(String device_id, int updated_version) {
        this.device_id = device_id;
        this.updated_version = updated_version;
    }
}
