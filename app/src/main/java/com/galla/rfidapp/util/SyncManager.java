package com.galla.rfidapp.util;

import android.content.Context;

public class SyncManager {


    private static final String PREF = "app_sync";
    private static final String KEY_VERSION = "sync_version";

    public static void triggerSync(Context ctx) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_VERSION, System.currentTimeMillis())
                .apply();
    }

    public static long getSyncVersion(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getLong(KEY_VERSION, 0);
    }
}
