package com.sleepcompany.rfidapp.network;

import com.sleepcompany.rfidapp.model.Product;
import java.util.List;

public class ProductsResponse {
    private boolean success;
    private String message;

    // Support old/new API: top-level fields
    private List<Product> products;
    private Pagination pagination;

    // ...and also existing data wrapper (if server uses it)
    private DataWrapper data;

    // Nested class for the "data" wrapper (kept for backward compatibility)
    public static class DataWrapper {
        private List<Product> products;
        private Pagination pagination;

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

    // Pagination info
    public static class Pagination {
        private int total;
        private int per_page;
        private int current_page;
        private int last_page;
        private int from;
        private int to;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getPer_page() { return per_page; }
        public void setPer_page(int per_page) { this.per_page = per_page; }

        public int getCurrent_page() { return current_page; }
        public void setCurrent_page(int current_page) { this.current_page = current_page; }

        public int getLast_page() { return last_page; }
        public void setLast_page(int last_page) { this.last_page = last_page; }

        public int getFrom() { return from; }
        public void setFrom(int from) { this.from = from; }

        public int getTo() { return to; }
        public void setTo(int to) { this.to = to; }

        public boolean hasMorePages() {
            return current_page < last_page;
        }
    }

    // Getters / setters for main fields
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public DataWrapper getData() { return data; }
    public void setData(DataWrapper data) { this.data = data; }

    // Top-level products/pagination getters (may be null if server uses data wrapper)
    public List<Product> getProductsTopLevel() { return products; }
    public void setProductsTopLevel(List<Product> products) { this.products = products; }

    public Pagination getPaginationTopLevel() { return pagination; }
    public void setPaginationTopLevel(Pagination pagination) { this.pagination = pagination; }

    // Unified getters that return whichever is present (wrapper or top-level)
    public List<Product> getProducts() {
        if (data != null && data.getProducts() != null) return data.getProducts();
        return products;
    }

    public Pagination getPagination() {
        if (data != null && data.getPagination() != null) return data.getPagination();
        return pagination;
    }

    // Convenience
    public boolean hasProducts() {
        List<Product> p = getProducts();
        return success && p != null && !p.isEmpty();
    }
}
