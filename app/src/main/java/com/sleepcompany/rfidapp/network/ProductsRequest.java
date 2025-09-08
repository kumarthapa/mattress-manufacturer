package com.sleepcompany.rfidapp.network;

public class ProductsRequest {
    private String search;
    private String status;
    private String start_date;
    private String end_date;
    private int page;
    private int limit;

    // Default constructor
    public ProductsRequest() {
        this.page = 1;
        this.limit = 20;
        this.status = "all";
    }

    // Constructor with common parameters
    public ProductsRequest(String search, String status, int page, int limit) {
        this.search = search;
        this.status = status;
        this.page = page;
        this.limit = limit;
    }

    // Getters and setters
    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStart_date() { return start_date; }
    public void setStart_date(String start_date) { this.start_date = start_date; }

    public String getEnd_date() { return end_date; }
    public void setEnd_date(String end_date) { this.end_date = end_date; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
