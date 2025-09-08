package com.sleepcompany.rfidapp.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor to add Authorization header to all API requests.
 * Logs requests and handles missing token gracefully.
 */
public class AuthInterceptor implements Interceptor {

    private static final String TAG = "AuthInterceptor";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_TOKEN = "auth_token";

    private final Context appContext;

    public AuthInterceptor(Context context) {
        this.appContext = context.getApplicationContext(); // Use app context to prevent leaks
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, null);

        Request.Builder builder = originalRequest.newBuilder()
                .header("Content-Type", "application/json");

        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
            Log.d(TAG, "Adding Authorization header");
        } else {
            Log.d(TAG, "No token found. Proceeding without Authorization header");
        }

        Request request = builder.build();
        Response response = chain.proceed(request);

        // Handle 401 Unauthorized globally
        if (response.code() == 401) {
            Log.w(TAG, "Received 401 Unauthenticated for URL: " + request.url());
            // Optional: trigger logout or redirect to login here
            // Example: ApiClient.clearToken(appContext);
        }

        return response;
    }
}
