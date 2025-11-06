package com.sleepcompany.rfidapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.seuic.uhf.UHFService;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrefHelper {
    private static final String TAG = "PrefHelper";
    public static final String PREFS_NAME = "app_prefs";

    // Public keys
    public static final String KEY_PRINTER_MAC = "printer_mac";
    public static final String KEY_RFID_POWER = "rfid_power";

    // Login keys
    public static final String KEY_REMEMBER = "remember_me";
    public static final String KEY_SAVED_USER = "saved_user";
    public static final String KEY_SAVED_PASS = "saved_pass";
    private static final String KEY_PERMISSIONS_JSON = "permissions_json";

    // ---------------- Device license verification ----------------
    private static final String KEY_LICENSE_VERIFIED = "license_verified";
    private static final String KEY_LICENSE_KEY = "license_key";
    private static final String KEY_LICENSE_END_DATE = "license_end_date";


    // ---------------- SharedPrefs helpers ----------------
    public static void savePrinterMac(Context context, String mac) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PRINTER_MAC, mac).apply();
    }

    public static String getPrinterMac(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PRINTER_MAC, null);
    }

    public static void clearPrinterMac(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_PRINTER_MAC).apply();
    }

    public static void saveCredentials(Context ctx, String user, String pass, boolean remember) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean(KEY_REMEMBER, remember);
        if (remember) {
            e.putString(KEY_SAVED_USER, user);
            e.putString(KEY_SAVED_PASS, pass);
        } else {
            e.remove(KEY_SAVED_USER);
            e.remove(KEY_SAVED_PASS);
            e.remove(KEY_REMEMBER);
        }
        e.apply();
    }

    public static boolean isRemembered(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_REMEMBER, false);
    }

    public static String getSavedUser(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_SAVED_USER, "");
    }

    public static String getSavedPass(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_SAVED_PASS, "");
    }

    public static void clearSessionData(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        e.remove("auth_token");
        e.remove("user_id");
        e.remove("user_profile_json");
        e.remove("last_sync_time");
        e.apply();
    }

    public static String dumpAllPrefs(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            sb.append(entry.getKey()).append(" => ").append(String.valueOf(entry.getValue())).append("\n");
        }
        return sb.toString();
    }

    public static void savePermissions(Context ctx, Map<String, List<String>> permissions) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = new Gson().toJson(permissions == null ? new HashMap<>() : permissions);
        prefs.edit().putString(KEY_PERMISSIONS_JSON, json).apply();
    }

    public static Map<String, List<String>> getPermissions(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_PERMISSIONS_JSON, null);
        if (json == null) return new HashMap<>();
        Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
        return new Gson().fromJson(json, type);
    }

    public static boolean hasPermission(Context ctx, String permission) {
        Map<String, List<String>> perms = getPermissions(ctx);
        if (perms == null || perms.isEmpty()) return false;
        for (List<String> list : perms.values()) {
            if (list != null && list.contains(permission)) return true;
        }
        return false;
    }

    // ---------------- RFID Power prefs ----------------
    public static void saveRfidPower(Context context, int power) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_RFID_POWER, power).apply();
    }

    public static int getRfidPower(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Default to 30 dBm if not set
        return prefs.getInt(KEY_RFID_POWER, 30);
    }

    public static void clearRfidPower(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_RFID_POWER).apply();
    }

    // ---------------- Helper to read current power from UHFService ----------------
    /**
     * Result holder for read attempt
     */
    public static class PowerResult {
        public final Integer power; // null if not readable
        public final String method; // method name used or null
        public PowerResult(Integer power, String method) {
            this.power = power;
            this.method = method;
        }
    }

    /**
     * Try to read current power from the provided UHFService instance.
     * This method does not touch UI. Caller should show Toast/logs.
     * Returns null if reading failed.
     */
    public static PowerResult readCurrentRfidPower(UHFService device) {
        if (device == null) {
            Log.w(TAG, "readCurrentRfidPower: device is null");
            return null;
        }

        String[] getterCandidates = new String[]{
                "getPower",
                "getOutputPower",
                "getRfPower",
                "getTxPower",
                "getPowerDbm"
        };

        Integer currentPower = null;
        String usedMethod = null;

        for (String name : getterCandidates) {
            try {
                Method m = device.getClass().getMethod(name);
                m.setAccessible(true);
                Object result = m.invoke(device);
                if (result instanceof Integer) {
                    currentPower = (Integer) result;
                    usedMethod = name;
                    break;
                } else if (result != null) {
                    try {
                        currentPower = Integer.parseInt(String.valueOf(result));
                        usedMethod = name;
                        break;
                    } catch (NumberFormatException ignored) {}
                }
            } catch (NoSuchMethodException e) {
                // continue
            } catch (Exception e) {
                Log.w(TAG, "readCurrentRfidPower: " + name + " failed: " + e.getMessage());
            }
        }

        if (currentPower != null) return new PowerResult(currentPower, usedMethod);

        // Fallback: try getParameters(paramId) for likely ids (non-destructive read)
        try {
            Method getParams = device.getClass().getMethod("getParameters", int.class);
            Integer maybe = null;
            String methodUsed = null;
            for (int paramId = 0; paramId <= 40; paramId++) {
                try {
                    Object res = getParams.invoke(device, paramId);
                    if (res != null) {
                        String s = String.valueOf(res);
                        try {
                            int v = Integer.parseInt(s);
                            if (v >= 0 && v <= 40) { // heuristic
                                maybe = v;
                                methodUsed = "getParameters(" + paramId + ")";
                                break;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                } catch (Exception ignored) {}
            }
            if (maybe != null) return new PowerResult(maybe, methodUsed);
        } catch (NoSuchMethodException ignored) {
            // nothing else to try
        } catch (Exception e) {
            Log.w(TAG, "readCurrentRfidPower: getParameters fallback failed: " + e.getMessage());
        }

        return null;
    }
// ------------------ License verification check ------------------------
    public static void setLicenseVerified(Context ctx, boolean verified) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LICENSE_VERIFIED, verified)
                .apply();
    }

    public static boolean isLicenseVerified(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_LICENSE_VERIFIED, false);
    }

    public static void saveLicenseInfo(Context ctx, String key, String endDate) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LICENSE_KEY, key)
                .putString(KEY_LICENSE_END_DATE, endDate)
                .apply();
    }


}



