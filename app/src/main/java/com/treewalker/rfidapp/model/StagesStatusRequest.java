package com.treewalker.rfidapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request model for fetching stages & statuses.
 */
public class StagesStatusRequest {

    @SerializedName("latest_stage")
    private String latestStage;

    @SerializedName("latest_status")
    private String latestStatus;

    @SerializedName("latest_remarks")
    private String latestRemarks;

    public StagesStatusRequest(String stage, String status, String remarks) {
        this.latestStage = stage;
        this.latestStatus = status;
        this.latestRemarks = remarks;
    }

    public String getLatestStage() { return latestStage; }
    public String getLatestStatus() { return latestStatus; }
    public String getLatestRemarks() { return latestRemarks; }
}
