package com.sleepcompany.rfidapp.model;

import com.google.gson.annotations.SerializedName;

public class UpdateStageRequest {
    @SerializedName("tag_id")
    private String tagId;

    @SerializedName("stage")
    private String stage;

    @SerializedName("qc_status")
    private String qcStatus;

    @SerializedName("comments")
    private String comments;

    @SerializedName("machine_no")
    private String machineNo;  // optional

    public UpdateStageRequest(String tagId, String stage, String qcStatus, String comments, String machineNo) {
        this.tagId = tagId;
        this.stage = stage;
        this.qcStatus = qcStatus;
        this.comments = comments;
        this.machineNo = machineNo;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getQcStatus() {
        return qcStatus;
    }

    public void setQcStatus(String qcStatus) {
        this.qcStatus = qcStatus;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getMachineNo() {
        return machineNo;
    }

    public void setMachineNo(String machineNo) {
        this.machineNo = machineNo;
    }
}
