package com.sleepcompany.rfidapp.network;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class LoginResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private LoginData data;

    // Getters and Setters
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

    public LoginData getData() {
        return data;
    }

    public void setData(LoginData data) {
        this.data = data;
    }

    // Convenience method to get token directly
    public String getToken() {
        return (data != null) ? data.getToken() : null;
    }
    // Inner class for data
    public static class LoginData {

        @SerializedName("token")
        private String token;

        @SerializedName("user")
        private User user;

        // add this
        @SerializedName("permissions")
        private Map<String, List<String>> permissions;

        // Add getters/setters
        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        // new getters/setters for permissions
        public Map<String, List<String>> getPermissions() { return permissions; }
        public void setPermissions(Map<String, List<String>> permissions) { this.permissions = permissions; }

        // You can create a nested User class if needed
        public static class User {
            @SerializedName("name")
            private String name;

            @SerializedName("email")
            private String email;

            @SerializedName("user_code")
            private String userCode;

            // Getters and setters
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }

            public String getEmail() { return email; }
            public void setEmail(String email) { this.email = email; }

            public String getUserCode() { return userCode; }
            public void setUserCode(String userCode) { this.userCode = userCode; }
        }
    }
}
