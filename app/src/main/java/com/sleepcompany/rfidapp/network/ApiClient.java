package com.sleepcompany.rfidapp.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

/**
 * Retrofit API client with AuthInterceptor and logging.
 * Optimized for speed and memory caching of token.
 */
public class ApiClient {

    private static final String TAG = "ApiClient";
    private static Retrofit retrofit = null;

    private static final String BASE_URL_PRODUCTION = "https://yourproductionbackend.com/api/";
    private static final String NGROK_URL = "https://7b19afb5881a.ngrok-free.app/api/";
    private static boolean IS_PRODUCTION = false;

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_TOKEN = "auth_token";

    // In-memory cache for token to avoid repeated SharedPreferences reads
    private static String cachedToken = null;

    /** Save token to SharedPreferences and memory cache */
    public static void setToken(Context context, String authToken) {
        if (authToken != null && !authToken.isEmpty()) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_TOKEN, authToken).apply();
            cachedToken = authToken;
            Log.d(TAG, "Token saved ✅: " + authToken);
        } else {
            Log.e(TAG, "Token is null or empty, not saved ❌");
        }
    }

    /** Get token from memory cache or SharedPreferences */
    public static String getToken(Context context) {
        if (cachedToken != null) return cachedToken;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        cachedToken = prefs.getString(KEY_TOKEN, null);
        return cachedToken;
    }

    /** Clear token on logout */
    public static void clearToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
        cachedToken = null;
        Log.d(TAG, "Token cleared");
    }

    /** Switch environment */
    public static void setProduction(boolean isProd) {
        if (IS_PRODUCTION != isProd) {
            IS_PRODUCTION = isProd;
            retrofit = null; // rebuild Retrofit with new base URL
            Log.d(TAG, "Environment switched. Production: " + IS_PRODUCTION);
        }
    }

    /** Get Retrofit instance */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // Logging only in development
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d(TAG, message));
            logging.setLevel(IS_PRODUCTION ? HttpLoggingInterceptor.Level.NONE : HttpLoggingInterceptor.Level.BODY);

            // Auth Interceptor
            Interceptor authInterceptor = chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder();

                String token = getToken(context);
                if (token != null) {
                    requestBuilder.addHeader("Authorization", "Bearer " + token);
                }

                return chain.proceed(requestBuilder.build());
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(authInterceptor)
                    .build();

            String baseUrl = IS_PRODUCTION ? BASE_URL_PRODUCTION : NGROK_URL;
            Log.d(TAG, "Building Retrofit with baseUrl: " + baseUrl);

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}
