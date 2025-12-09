package com.sleepcompany.rfidapp.network;

public class ProductsRequest {

    private String search;
    private String status;
    private String product_code;
    private String rfid_code;
    private String start_date;
    private String end_date;
    private Integer location_id;

    private int page;
    private int limit;

    public ProductsRequest() {
        this.page = 1;
        this.limit = 20;
        this.status = "all";
    }

    // GETTERS & SETTERS

    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProduct_code() { return product_code; }
    public void setProduct_code(String product_code) { this.product_code = product_code; }

    public String getRfid_code() { return rfid_code; }
    public void setRfid_code(String rfid_code) { this.rfid_code = rfid_code; }

    public String getStart_date() { return start_date; }
    public void setStart_date(String start_date) { this.start_date = start_date; }

    public String getEnd_date() { return end_date; }
    public void setEnd_date(String end_date) { this.end_date = end_date; }

    public Integer getLocation_id() { return location_id; }
    public void setLocation_id(Integer location_id) { this.location_id = location_id; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
