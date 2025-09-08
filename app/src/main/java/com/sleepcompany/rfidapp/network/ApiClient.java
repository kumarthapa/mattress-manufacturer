package com.sleepcompany.rfidapp.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit API client with AuthInterceptor and logging.
 */
public class ApiClient {

    private static final String TAG = "ApiClient";
    private static Retrofit retrofit = null;

    private static final String BASE_URL_PRODUCTION = "https://yourproductionbackend.com/api/";
    private static final String BASE_URL_DEVELOPMENT = "https://96976ab0d067.ngrok-free.app/api/";
    private static boolean IS_PRODUCTION = false;

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_TOKEN = "auth_token";

    /** Save token to SharedPreferences and reset Retrofit. */
    public static void setToken(Context context, String authToken) {
        if (authToken != null && !authToken.isEmpty()) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_TOKEN, authToken).apply();
            Log.d(TAG, "Token saved ✅: " + authToken);
            retrofit = null; // reset Retrofit to include auth header next time
        } else {
            Log.e(TAG, "Token is null or empty, not saved ❌");
        }
    }


    /** Get saved token from SharedPreferences. */
    public static String getToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    /** Check if a token exists in SharedPreferences. */
    public static boolean hasToken(Context context) {
        return getToken(context) != null && !getToken(context).isEmpty();
    }

    /** Clear token (logout) and reset Retrofit. */
    public static void clearToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
        Log.d(TAG, "Token cleared");
        retrofit = null;
    }

    /** Switch between production and development environments. */
    public static void setProduction(boolean isProd) {
        if (IS_PRODUCTION != isProd) {
            IS_PRODUCTION = isProd;
            Log.d(TAG, "Environment switched. Production: " + IS_PRODUCTION);
            retrofit = null;
        }
    }

    /** Get Retrofit client instance with AuthInterceptor and logging. */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d(TAG, message));
            logging.setLevel(IS_PRODUCTION
                    ? HttpLoggingInterceptor.Level.NONE
                    : HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(new AuthInterceptor(context))
                    .build();

            String baseUrl = IS_PRODUCTION ? BASE_URL_PRODUCTION : BASE_URL_DEVELOPMENT;
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
