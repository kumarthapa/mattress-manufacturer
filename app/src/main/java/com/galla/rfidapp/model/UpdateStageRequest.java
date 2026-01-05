package com.galla.rfidapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * Request model for updating a product stage.
 */
public class UpdateStageRequest {

    @SerializedName("tag_id")
    private String tagId;

    @SerializedName("stage")
    private String stage;

    @SerializedName("status")
    private String status;

    // Defects points as JSON array, e.g., ["over_size","colour_issue"]
    @SerializedName("defects_points")
    private List<String> defectsPoints;

    @SerializedName("remarks")
    private String remarks;

    public UpdateStageRequest(String tagId, String stage, String status, List<String> defectsPoints, String remarks) {
        this.tagId = tagId;
        this.stage = stage;
        this.status = status;
        // ensure we never send null, always array
        this.defectsPoints = defectsPoints != null ? defectsPoints : new ArrayList<>();
        this.remarks = remarks;
    }

    // Getters
    public String getTagId() { return tagId; }
    public String getStage() { return stage; }
    public String getStatus() { return status; }
    public List<String> getDefectsPoints() { return defectsPoints; }
    public String getRemarks() { return remarks; }

    // Setters
    public void setTagId(String tagId) { this.tagId = tagId; }
    public void setStage(String stage) { this.stage = stage; }
    public void setStatus(String status) { this.status = status; }
    public void setDefectsPoints(List<String> defectsPoints) {
        this.defectsPoints = defectsPoints != null ? defectsPoints : new ArrayList<>();
    }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
