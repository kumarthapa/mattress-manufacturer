package com.galla.rfidapp.model;

import com.google.gson.annotations.SerializedName;

public class AssetNetwork {

    private Integer id;
    private String name;

    @SerializedName("asset_tag")
    private String assetTag;

    private String rfid;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public String getRfid() {
        return rfid;
    }
}
