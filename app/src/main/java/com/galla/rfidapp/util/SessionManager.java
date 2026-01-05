package com.galla.rfidapp.util;

import android.content.Context;
import android.util.Log;

import com.galla.rfidapp.network.ApiClient;

public class SessionManager {
    private static final String TAG = "SessionManager";

    /**
     * Perform a safe logout: clears auth/session token and runtime user data,
     * but intentionally keeps printer selection and remembered credentials.
     */
    public static void logoutKeepPreferences(Context context) {
        // 1) Clear auth token via ApiClient helper
        ApiClient.clearToken(context);

        // 2) Clear session-only keys in shared prefs (PrefHelper will remove specific session keys)
        PrefHelper.clearSessionData(context);

        Log.d(TAG, "logoutKeepPreferences completed: token & session keys cleared; printer & credentials kept.");
    }
}
