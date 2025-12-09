package com.sleepcompany.rfidapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

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

        // Keep if recent list coming, or remove it if not needed
        @SerializedName("recent_activities")
        public List<RecentActivity> recent_activities;
    }
}
