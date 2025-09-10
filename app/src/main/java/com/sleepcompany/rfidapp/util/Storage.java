package com.sleepcompany.rfidapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONObject;

public class Storage {


    private static JSONObject user;
    private static JSONObject config;

    public static JSONObject getUser(Context context) {
        if(user == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            try {
                user = new JSONObject(pref.getString("scannerUser", null));
            } catch (Exception e) {
                user = null;
            }
        }
        return user;
    }

    public static Boolean setUser(Context context, JSONObject userJson) {
        user = userJson;
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString("scannerUser", user.toString());
        return editor.commit();
    }

    public static void deleteUser(Context context) {
        user = null;
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove("scannerUser");
        editor.apply();
    }

    public static JSONObject getConfig(Context context) {
        if(config == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            try {
                config = new JSONObject(pref.getString("scannerConfig", null));
            } catch (Exception e) {
                config = null;
            }
        }
        return config;
    }

    public static Boolean setConfig(Context context, JSONObject config) {
        Storage.config = config;
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString("scannerConfig", config.toString());
        return editor.commit();
    }


}
