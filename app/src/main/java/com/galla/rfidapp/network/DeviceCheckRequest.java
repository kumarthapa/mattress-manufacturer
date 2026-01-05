package com.galla.rfidapp.network;

public class DeviceCheckRequest {
    private String device_id;

    public DeviceCheckRequest(String device_id) {
        this.device_id = device_id;
    }

    public String getDevice_id() { return device_id; }
    public void setDevice_id(String device_id) { this.device_id = device_id; }

    public static class UpdateCheckResponse {
        private boolean update_required;
        private int latest_version_code;
        private String apk_url;
        private String message;

        public boolean isUpdate_required() { return update_required; }
        public int getLatest_version_code() { return latest_version_code; }
        public String getApk_url() { return apk_url; }
        public String getMessage() { return message; }
    }
}
