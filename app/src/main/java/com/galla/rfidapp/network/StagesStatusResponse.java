package com.galla.rfidapp.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 * API response for getting stages, statuses, and defect points.
 */
public class StagesStatusResponse {

    private boolean success;
    private String message;
    private Data data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Data getData() { return data; }

    public static class Data {

        private Map<String, String> stages;
        private Map<String, String> status;

        // Stage-wise defect points
        @SerializedName("defect_points")
        private Map<String, List<Map<String, String>>> defectPoints;

        // ⭐ NEW: allowed_stages from backend
        @SerializedName("allowed_stages")
        private List<String> allowedStages;

        // ----------- GETTERS -----------
        public Map<String, String> getStages() {
            return stages;
        }

        public Map<String, String> getStatus() {
            return status;
        }

        public Map<String, List<Map<String, String>>> getDefectPoints() {
            return defectPoints;
        }

        public List<String> getAllowedStages() {
            return allowedStages;
        }
    }




}
