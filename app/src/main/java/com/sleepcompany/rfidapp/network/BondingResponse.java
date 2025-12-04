package com.sleepcompany.rfidapp.network;

import java.util.List;

public class BondingResponse {
    private boolean success;
    private String message;
    private DataWrapper data;

    public static class DataWrapper {
        private List<BondingProduct> products;
        private Pagination pagination;

        public List<BondingProduct> getProducts() {
            return products;
        }

        public void setProducts(List<BondingProduct> products) {
            this.products = products;
        }

        public Pagination getPagination() {
            return pagination;
        }

        public void setPagination(Pagination pagination) {
            this.pagination = pagination;
        }
    }

    public static class Pagination {
        private int total;
        private int per_page;
        private int current_page;
        private int last_page;
        private int from;
        private int to;

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

        public boolean hasMorePages() {
            return current_page < last_page;
        }
    }

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

    public List<BondingProduct> getProducts() {
        return data != null ? data.getProducts() : null;
    }

    public Pagination getPagination() {
        return data != null ? data.getPagination() : null;
    }

    public boolean hasProducts() {
        return success && data != null && data.getProducts() != null && !data.getProducts().isEmpty();
    }

    public static class UpdateCheckRequest {
        private String device_id;
        private int current_version_code;

        public UpdateCheckRequest(String device_id, int current_version_code) {
            this.device_id = device_id;
            this.current_version_code = current_version_code;
        }

        // getters / setters (optional)
        public String getDevice_id() { return device_id; }
        public int getCurrent_version_code() { return current_version_code; }
    }
}

