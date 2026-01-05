package com.galla.rfidapp.repository;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.galla.rfidapp.network.ApiClient;
import com.galla.rfidapp.network.ApiService;
import com.galla.rfidapp.network.LoginRequest;
import com.galla.rfidapp.network.LoginResponse;
import com.galla.rfidapp.network.ProductsRequest;
import com.galla.rfidapp.network.ProductsResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository to handle API calls (login, products) with token management.
 */
public class DataRepository {

    private static final String TAG = "DataRepository";
    private static DataRepository instance;
    private ApiService apiService;
    private final Context context;

    // Singleton pattern
    public static DataRepository getInstance(Context context) {
        if (instance == null) {
            instance = new DataRepository(context.getApplicationContext());
        }
        return instance;
    }

    private DataRepository(Context context) {
        this.context = context;
        apiService = ApiClient.getClient(context).create(ApiService.class);
    }

    // ------------------ LOGIN ------------------


    // change method signature to accept licenseKey
    public void loginUser(String licenseKey, String username, String password, final LoginCallback callback) {
        // Build request with licenseKey
        LoginRequest loginRequest = new LoginRequest(licenseKey, username, password);

        apiService.loginUser(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getData() != null) {

                    String token = response.body().getData().getToken();
                    if (!TextUtils.isEmpty(token)) {
                        ApiClient.setToken(context, token); // Save token for future API calls
                        Log.d(TAG, "Token saved successfully");
                        callback.onSuccess();
                    } else {
                        callback.onError("Token missing in login response");
                    }

                } else {
                    String errorMsg = "Login failed";
                    if (response.body() != null && !TextUtils.isEmpty(response.body().getMessage())) {
                        errorMsg = response.body().getMessage();
                    } else if (response.code() == 401) {
                        errorMsg = "Authentication failed. Please check credentials.";
                        Log.w(TAG, "401 Unauthenticated");
                    }
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network error";
                Log.e(TAG, "Login error: " + errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }


    // ------------------ FETCH PRODUCTS ------------------
    public void fetchProducts(ProductsRequest productsRequest, final ProductsCallback callback) {
        // Refresh apiService to include the latest token
        apiService = ApiClient.getClient(context).create(ApiService.class);

        apiService.getProducts(productsRequest).enqueue(new Callback<ProductsResponse>() {
            @Override
            public void onResponse(Call<ProductsResponse> call, Response<ProductsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body());
                } else {
                    String errorMsg = "Failed to fetch products";
                    if (response.body() != null && !TextUtils.isEmpty(response.body().getMessage())) {
                        errorMsg = response.body().getMessage();
                    } else if (response.code() == 401) {
                        errorMsg = "Unauthorized. Please login again.";
                        Log.w(TAG, "401 Unauthenticated");
                    } else if (response.code() >= 500) {
                        errorMsg = "Server error. Try again later.";
                    }
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ProductsResponse> call, Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network error";
                Log.e(TAG, "Fetch products error: " + errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }

    // ------------------ CALLBACK INTERFACES ------------------
    public interface LoginCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ProductsCallback {
        void onSuccess(ProductsResponse productsResponse);
        void onError(String message);
    }

    // Optional: clear singleton instance (useful on logout)
    public static void clearInstance() {
        instance = null;
    }
}
