package com.sleepcompany.rfidapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * KPI model matches the backend JSON keys used by Dashboard API.
 * Using wrapper types (Integer / Double) to be safe if backend omits a field.
 */
public class Kpis {
    @SerializedName("total_today")
    public Integer total_today;

    @SerializedName("total_month")
    public Integer total_month;

    @SerializedName("pass_today")
    public Integer pass_today;

    @SerializedName("pending_today")
    public Integer pending_today;

    @SerializedName("defects_today")
    public Integer defects_today;

    @SerializedName("efficiency_percent")
    public Double efficiency_percent;

    @SerializedName("defect_rate_percent")
    public Double defect_rate_percent;
}
