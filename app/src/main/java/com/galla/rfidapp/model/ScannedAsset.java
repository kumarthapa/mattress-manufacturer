package com.galla.rfidapp.model;

public class ScannedAsset {

    private String assetTag;
    private String rfid;
    private boolean found;   // matched with backend asset list

    public ScannedAsset(String assetTag, String rfid) {
        this.assetTag = assetTag;
        this.rfid = rfid;
        this.found = false;
    }

    // ---------------- getters ----------------

    public String getAssetTag() {
        return assetTag;
    }

    public String getRfid() {
        return rfid;
    }

    public boolean isFound() {
        return found;
    }

    // ---------------- setters ----------------

    public void setFound(boolean found) {
        this.found = found;
    }
}
