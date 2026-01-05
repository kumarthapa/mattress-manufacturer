package com.galla.rfidapp.repository;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.galla.rfidapp.network.ApiClient;
import com.galla.rfidapp.network.ApiService;
import com.galla.rfidapp.network.LoginRequest;
import com.galla.rfidapp.network.LoginResponse;
import com.galla.rfidapp.model.AssetNetwork;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DataRepository
 * -----------------------------
 * Handles ONLY:
 *  - Login
 *  - Fetching all assets (single source of truth)
 *
 * ❌ Removed: Products, ProductsRequest, ProductsResponse
 * ❌ Removed: getProducts()
 */
public class DataRepository {

    private static final String TAG = "DataRepository";
    private static DataRepository instance;
    private ApiService apiService;
    private final Context context;

    // ------------------ SINGLETON ------------------
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
    public void loginUser(String licenseKey, String username, String password, final LoginCallback callback) {

        LoginRequest loginRequest = new LoginRequest(licenseKey, username, password);

        apiService.loginUser(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {

                    String token = response.body().getData().getToken();
                    if (!TextUtils.isEmpty(token)) {
                        ApiClient.setToken(context, token);
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
                    }
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network error";
                Log.e(TAG, "Login error", t);
                callback.onError(errorMsg);
            }
        });
    }

    // ------------------ FETCH ALL ASSETS ------------------
    public void fetchAllAssets(final AssetsCallback callback) {

        apiService = ApiClient.getClient(context).create(ApiService.class);

        apiService.getAllAssetDetails().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to fetch assets (HTTP " + response.code() + ")");
                    return;
                }

                try {
                    Object assetsObj = response.body().get("assets");
                    if (!(assetsObj instanceof List<?>)) {
                        callback.onError("Invalid assets response");
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rawAssets = (List<Map<String, Object>>) assetsObj;
                    callback.onSuccess(rawAssets);

                } catch (Exception e) {
                    Log.e(TAG, "Parsing assets failed", e);
                    callback.onError("Failed to parse assets");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network error";
                Log.e(TAG, "Fetch assets error", t);
                callback.onError(errorMsg);
            }
        });
    }

    // ------------------ CALLBACKS ------------------
    public interface LoginCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface AssetsCallback {
        void onSuccess(List<Map<String, Object>> rawAssets);
        void onError(String message);
    }

    // ------------------ CLEANUP ------------------
    public static void clearInstance() {
        instance = null;
    }
}
