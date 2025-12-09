package com.sleepcompany.rfidapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Clean KPI model for the new dashboard structure.
 * Only includes fields actually used in the updated UI.
 */
public class Kpis {

    // Total RFID tags
    @SerializedName("total_tags")
    public Integer total_tags;

    // Total inward movements
    @SerializedName("total_inward")
    public Integer total_inward;

    // Total outward movements
    @SerializedName("total_outward")
    public Integer total_outward;

    // Total products
    @SerializedName("total_products")
    public Integer total_products;

    // Mapped tags
    @SerializedName("total_tags_mapped")
    public Integer total_tags_mapped;

    // Unmapped tags
    @SerializedName("total_tags_unmapped")
    public Integer total_tags_unmapped;
}
