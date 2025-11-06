package com.sleepcompany.rfidapp.model;

/**
 * Request model for fetching stages & status.
 */
public class StagesStatusRequest {

    private String stage;
    private String status;

    private String remarks;

    public StagesStatusRequest(String stage, String status, String remarks) {
        this.stage = stage;
        this.status = status;
        this.status = remarks;
    }

    public String getCurrent_stage() { return stage; }
    public String getCurrent_status() { return status; }
    public String getCurrent_remarks() { return remarks; }
}
