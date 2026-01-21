package com.treewalker.rfidapp.network;

public class TagSearchRequest {
    private String tag_id;

    public TagSearchRequest(String tagId) {
        this.tag_id = tagId;
    }

    public String getTag_id() {
        return tag_id;
    }

    public void setTag_id(String tag_id) {
        this.tag_id = tag_id;
    }
}
