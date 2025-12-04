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
 * Retrofit API client with AuthInterceptor and detailed logging.
 * Provides:
 *  - getClient(context)    -> includes Authorization header when token saved
 *  - getPublicClient()     -> public client WITHOUT Authorization header (for verifyLicense, login)
 *
 * Note: call ApiClient.getPublicClient().create(ApiService.class) for public endpoints.
 */
public class ApiClient {

    private static final String TAG = "ApiClient";

    // Two separate Retrofit instances: one that injects auth header, one public for unauthenticated endpoints
    private static Retrofit retrofitWithAuth = null;
    private static Retrofit retrofitPublic = null;

    // Real production API path
    //private static final String BASE_URL_PRODUCTION = "https://apps.galla.ai/sleepcompany/api/";
    //dev web login =  https://apps.galla.ai/dev/auth/login
    private static final String BASE_URL_PRODUCTION = "https://apps.galla.ai/dev/api/";
    private static final String NGROK_URL = "https://3a3382ff2ee3.ngrok-free.app/api/";
    private static boolean IS_PRODUCTION = true;
    // Note: If you want to run in production, set this to true.
    // Then go to LauncherActivity inside the onCreate() method and call:
    // ApiClient.setProduction(true); // Enable PRODUCTION mode


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
            // Ensure retrofitWithAuth will be rebuilt to pick up new token (if needed)
            retrofitWithAuth = null;
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
        retrofitWithAuth = null;
        Log.d(TAG, "Token cleared");
    }

    /** Switch
     *  (production / dev-ngrok) */
    public static void setProduction(boolean isProd) {
        if (IS_PRODUCTION != isProd) {
            IS_PRODUCTION = isProd;
            retrofitWithAuth = null; // rebuild Retrofit with new base URL
            retrofitPublic = null;
            Log.d(TAG, "Environment switched. Production: " + IS_PRODUCTION);
        }
    }
    public static void resetClients() {
        retrofitPublic = null;
        retrofitWithAuth = null;
        Log.e(TAG, "Retrofit clients RESET");
    }
    private static String getBaseUrl() {
        return IS_PRODUCTION ? BASE_URL_PRODUCTION : NGROK_URL;
    }

    /** Get Retrofit instance that includes Authorization header (reads token from prefs) */
    public static Retrofit getClient(final Context context) {
        if (retrofitWithAuth == null) {
            // Logging interceptor (BODY) — useful for debugging
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d(TAG, message));
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Auth Interceptor that attaches Bearer token if available
            Interceptor authInterceptor = chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder();

                String token = getToken(context);
                if (token != null && !token.isEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer " + token);
                }

                Request newRequest = requestBuilder.build();
                Log.d(TAG, "➡️ Requesting URL: " + newRequest.url());
                Log.d(TAG, "➡️ Headers: " + newRequest.headers());

                Response response;
                try {
                    response = chain.proceed(newRequest);
                } catch (IOException e) {
                    Log.e(TAG, "❌ Network call failed: " + e.getMessage(), e);
                    throw e;
                }

                if (!response.isSuccessful()) {
                    Log.e(TAG, "⚠️ API Error: " + response.code() + " - " + response.message());
                }
                return response;
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(authInterceptor)
                    .build();

            String baseUrl = getBaseUrl();
            Log.d(TAG, "Building Retrofit (with auth) with baseUrl: " + baseUrl);

            retrofitWithAuth = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofitWithAuth;
    }

    /**
     * Get Retrofit instance for public endpoints (NO Authorization header).
     * Use this for: device/verify-license, user/login (before token is issued), any public route.
     *
     * Example: ApiClient.getPublicClient().create(ApiService.class)
     */
    public static Retrofit getPublicClient() {
        if (retrofitPublic == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d(TAG, message));
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Public client: no auth interceptor, only logging (and optionally default headers)
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        // optional small interceptor to log URL/headers consistently
                        Request req = chain.request();
                        Log.d(TAG, "➡️ Public Request URL: " + req.url());
                        Log.d(TAG, "➡️ Public Request Headers: " + req.headers());
                        Response resp;
                        try {
                            resp = chain.proceed(req);
                        } catch (IOException e) {
                            Log.e(TAG, "❌ Public network call failed: " + e.getMessage(), e);
                            throw e;
                        }
                        if (!resp.isSuccessful()) {
                            Log.e(TAG, "⚠️ Public API Error: " + resp.code() + " - " + resp.message());
                        }
                        return resp;
                    })
                    .build();

            String baseUrl = getBaseUrl();
            Log.d(TAG, "Building Retrofit (public) with baseUrl: " + baseUrl);

            retrofitPublic = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofitPublic;
    }
}
