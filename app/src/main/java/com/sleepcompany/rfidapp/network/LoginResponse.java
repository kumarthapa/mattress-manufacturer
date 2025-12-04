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

    public boolean isSuccess() {
        return success;
    }
    public String getMessage() {
        return message;
    }
    public LoginData getData() {
        return data;
    }

    public String getToken() {
        return (data != null) ? data.getToken() : null;
    }

    // -------------------- LOGIN DATA --------------------
    public static class LoginData {

        @SerializedName("token")
        private String token;

        @SerializedName("user")
        private User user;

        @SerializedName("permissions")
        private Map<String, List<String>> permissions;

        public String getToken() { return token; }
        public User getUser() { return user; }
        public Map<String, List<String>> getPermissions() { return permissions; }

        // -------------------- USER MODEL --------------------
        public static class User {

            @SerializedName("name")
            private String name;

            @SerializedName("email")
            private String email;

            @SerializedName("user_code")
            private String userCode;

            @SerializedName("location_id")
            private int locationId;

            @SerializedName("location_name")
            private String locationName;

            @SerializedName("working_stages")
            private List<String> workingStages;

            @SerializedName("role")
            private Role role;

            @SerializedName("api_key")
            private String apiKey;

            public String getName() { return name; }
            public String getEmail() { return email; }
            public String getUserCode() { return userCode; }
            public int getLocationId() { return locationId; }
            public String getLocationName() { return locationName; }
            public List<String> getWorkingStages() { return workingStages; }
            public Role getRole() { return role; }
            public String getApiKey() { return apiKey; }

            // ------------- ROLE MODEL --------------
            public static class Role {
                @SerializedName("role_id")
                private int id;

                @SerializedName("role_name")
                private String name;

                @SerializedName("role_code")
                private String code;

                public int getId() { return id; }
                public String getName() { return name; }
                public String getCode() { return code; }
            }
        }
    }
}
