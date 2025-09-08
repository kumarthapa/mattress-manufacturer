package com.sleepcompany.rfidapp.network;

import com.sleepcompany.rfidapp.model.Product;

import java.util.List;

public class ProductsResponse {
    private boolean success;
    private String message;
    private DataWrapper data;

    // Nested class for the "data" wrapper
    public static class DataWrapper {
        private List<Product> products;
        private Pagination pagination;

        // Getters and setters
        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public Pagination getPagination() {
            return pagination;
        }

        public void setPagination(Pagination pagination) {
            this.pagination = pagination;
        }
    }

    // Nested class for pagination info
    public static class Pagination {
        private int total;
        private int per_page;
        private int current_page;
        private int last_page;
        private int from;
        private int to;

        // Getters and setters
        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getPer_page() {
            return per_page;
        }

        public void setPer_page(int per_page) {
            this.per_page = per_page;
        }

        public int getCurrent_page() {
            return current_page;
        }

        public void setCurrent_page(int current_page) {
            this.current_page = current_page;
        }

        public int getLast_page() {
            return last_page;
        }

        public void setLast_page(int last_page) {
            this.last_page = last_page;
        }

        public int getFrom() {
            return from;
        }

        public void setFrom(int from) {
            this.from = from;
        }

        public int getTo() {
            return to;
        }

        public void setTo(int to) {
            this.to = to;
        }

        // Utility method to check if there are more pages
        public boolean hasMorePages() {
            return current_page < last_page;
        }
    }

    // Main class getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DataWrapper getData() {
        return data;
    }

    public void setData(DataWrapper data) {
        this.data = data;
    }

    // Utility methods for easier access
    public List<Product> getProducts() {
        return data != null ? data.getProducts() : null;
    }

    public Pagination getPagination() {
        return data != null ? data.getPagination() : null;
    }

    // Check if response has products
    public boolean hasProducts() {
        return success && data != null && data.getProducts() != null && !data.getProducts().isEmpty();
    }
}
