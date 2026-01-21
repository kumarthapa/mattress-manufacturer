package com.treewalker.rfidapp.network;

public class UpdateCheckResponse {
    private boolean update_required;
    private int latest_version_code;
    private String apk_url;
    private String message;

    public boolean isUpdate_required() { return update_required; }
    public int getLatest_version_code() { return latest_version_code; }
    public String getApk_url() { return apk_url; }
    public String getMessage() { return message; }
}
