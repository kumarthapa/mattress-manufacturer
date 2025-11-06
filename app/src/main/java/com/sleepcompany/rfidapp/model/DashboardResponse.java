package com.sleepcompany.rfidapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class DashboardResponse {
    @SerializedName("success")
    public Boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public Data data;

    public static class Data {
        @SerializedName("kpis")
        public Kpis kpis;

        @SerializedName("stages")
        public Map<String, Integer> stages;

        @SerializedName("recent_activities")
        public List<RecentActivity> recent_activities;
    }
}
