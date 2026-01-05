package com.galla.rfidapp.network;

import com.galla.rfidapp.model.AssetNetwork;
import java.util.List;

public class ProductsResponse {

    private boolean success;
    private String message;

    // --- Top-level support ---
    private List<AssetNetwork> products;
    private Pagination pagination;

    // --- Wrapped data support ---
    private DataWrapper data;

    // =================================================
    // DATA WRAPPER (Laravel-style pagination support)
    // =================================================
    public static class DataWrapper {
        private List<AssetNetwork> products;
        private Pagination pagination;

        public List<AssetNetwork> getProducts() {
            return products;
        }

        public void setProducts(List<AssetNetwork> products) {
            this.products = products;
        }

        public Pagination getPagination() {
            return pagination;
        }

        public void setPagination(Pagination pagination) {
            this.pagination = pagination;
        }
    }

    // =================================================
    // PAGINATION
    // =================================================
    public static class Pagination {
        private int total;
        private int per_page;
        private int current_page;
        private int last_page;
        private int from;
        private int to;

        public int getTotal() { return total; }
        public int getPer_page() { return per_page; }
        public int getCurrent_page() { return current_page; }
        public int getLast_page() { return last_page; }
        public int getFrom() { return from; }
        public int getTo() { return to; }

        public boolean hasMorePages() {
            return current_page < last_page;
        }
    }

    // =================================================
    // BASIC GETTERS
    // =================================================
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public DataWrapper getData() {
        return data;
    }

    // =================================================
    // UNIFIED ACCESSORS (USE THESE EVERYWHERE)
    // =================================================
    public List<AssetNetwork> getProducts() {
        if (data != null && data.getProducts() != null) {
            return data.getProducts();
        }
        return products;
    }

    public Pagination getPagination() {
        if (data != null && data.getPagination() != null) {
            return data.getPagination();
        }
        return pagination;
    }

    public boolean hasProducts() {
        List<AssetNetwork> list = getProducts();
        return success && list != null && !list.isEmpty();
    }
}
