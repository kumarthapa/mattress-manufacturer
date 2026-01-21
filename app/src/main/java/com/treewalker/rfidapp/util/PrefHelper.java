package com.treewalker.rfidapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.seuic.uhf.UHFService;

import org.json.JSONObject;

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

    // Printer language keys (added)
    private static final String KEY_PRINTER_LANGUAGE_GLOBAL = "printer_language_global";
    private static final String KEY_PRINTER_LANGUAGE_MAP = "printer_language_map"; // JSON map per-MAC

    // Login keys
    public static final String KEY_REMEMBER = "remember_me";
    public static final String KEY_SAVED_USER = "saved_user";
    public static final String KEY_SAVED_PASS = "saved_pass";
    private static final String KEY_PERMISSIONS_JSON = "permissions_json";

    // NEW: Allowed stage cache
    private static final String KEY_ALLOWED_STAGES = "allowed_stages";

    // ---------------- Device license verification ----------------
    private static final String KEY_LICENSE_VERIFIED = "license_verified";
    private static final String KEY_LICENSE_KEY = "license_key";
    private static final String KEY_LICENSE_END_DATE = "license_end_date";
    // USER INFO (for drawer header)
    private static final String KEY_USER_NAME = "header_user_name";
    private static final String KEY_WORKING_STAGE = "header_working_stage";
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

    // ---------------- ALLOWED STAGES CACHE ----------------

    public static void saveAllowedStages(Context ctx, List<String> stages) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ALLOWED_STAGES, new Gson().toJson(stages)).apply();
    }

    public static List<String> getAllowedStages(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ALLOWED_STAGES, "[]");
        return new Gson().fromJson(json, new TypeToken<List<String>>() {}.getType());
    }

    // ---------------- RFID Power prefs ----------------
    public static void saveRfidPower(Context context, int power) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_RFID_POWER, power).apply();
    }

    public static int getRfidPower(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int value = prefs.getInt(KEY_RFID_POWER, -1);

        // If user has never set value, or value is invalid → default to 5
        if (value < 10 || value > 30) {
            return 10;
        }
        return value;
    }


    public static void clearRfidPower(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_RFID_POWER).apply();
    }

    // ---------------- Helper to read current power ----------------
    public static class PowerResult {
        public final Integer power;
        public final String method;

        public PowerResult(Integer power, String method) {
            this.power = power;
            this.method = method;
        }
    }

    public static PowerResult readCurrentRfidPower(UHFService device) {
        if (device == null) {
            Log.w(TAG, "readCurrentRfidPower: device is null");
            return null;
        }

        String[] getterCandidates = new String[]{
                "getPower", "getOutputPower", "getRfPower", "getTxPower", "getPowerDbm"
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
                    currentPower = Integer.parseInt(String.valueOf(result));
                    usedMethod = name;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (currentPower != null) return new PowerResult(currentPower, usedMethod);

        try {
            Method getParams = device.getClass().getMethod("getParameters", int.class);
            for (int paramId = 0; paramId <= 40; paramId++) {
                try {
                    Object res = getParams.invoke(device, paramId);
                    if (res != null) {
                        int v = Integer.parseInt(String.valueOf(res));
                        if (v >= 0 && v <= 40) {
                            return new PowerResult(v, "getParameters(" + paramId + ")");
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        return null;
    }

    // ---------------- License verification ----------------
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

    // ---------------- USER INFO (NAME + WORKING STAGES) ----------------
    /** Save username + working stage for drawer header */
    public static void saveHeaderUser(Context ctx, String userName, String workingStage) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_USER_NAME, userName == null ? "" : userName)
                .putString(KEY_WORKING_STAGE, workingStage == null ? "" : workingStage)
                .apply();
    }

    /** Get saved username (fallback: "User") */
    public static String getHeaderUserName(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "User");
    }

    /** Get saved working stage (fallback: "-") */
    public static String getHeaderWorkingStage(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_WORKING_STAGE, "-");
    }

    // ---------------- PRINTER LANGUAGE STORAGE ----------------

    /**
     * Save printer language globally (default for new printers)
     * Example values: "ZPL", "TSPL", "ESC_POS", "AUTO"
     */
    public static void savePrinterLanguageGlobal(Context ctx, String lang) {
        if (lang == null) lang = "AUTO";
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PRINTER_LANGUAGE_GLOBAL, lang).apply();
    }

    /**
     * Get global fallback printer language
     * Returns "AUTO" if not set.
     */
    public static String getPrinterLanguageGlobal(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String v = prefs.getString(KEY_PRINTER_LANGUAGE_GLOBAL, null);
        return v != null ? v : "AUTO";
    }

    /**
     * Save language for a specific MAC address
     * Stored as JSON:  {"AA:BB:CC:DD:EE:FF": "TSPL"}
     */
    public static void savePrinterLanguageForMac(Context ctx, String mac, String lang) {
        if (mac == null) return;
        if (lang == null) lang = "AUTO";

        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_PRINTER_LANGUAGE_MAP, "{}");

        try {
            JSONObject obj = new JSONObject(json);
            obj.put(mac, lang);
            prefs.edit().putString(KEY_PRINTER_LANGUAGE_MAP, obj.toString()).apply();
        } catch (Exception e) {
            Log.w(TAG, "savePrinterLanguageForMac failed: " + e.getMessage(), e);
            // fallback: overwrite map with a simple JSON containing this mapping
            try {
                JSONObject fallback = new JSONObject();
                fallback.put(mac, lang);
                prefs.edit().putString(KEY_PRINTER_LANGUAGE_MAP, fallback.toString()).apply();
            } catch (Exception ex) {
                Log.w(TAG, "savePrinterLanguageForMac fallback also failed: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Get printer language for a specific MAC.
     * If not stored → return GLOBAL language
     * If still null → return "AUTO"
     */
    public static String getPrinterLanguageForMac(Context ctx, String mac) {
        if (mac == null) return "AUTO";

        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_PRINTER_LANGUAGE_MAP, "{}");

        try {
            JSONObject obj = new JSONObject(json);
            if (obj.has(mac)) {
                String v = obj.getString(mac);
                if (v != null && !v.isEmpty()) return v;
            }
        } catch (Exception e) {
            Log.w(TAG, "getPrinterLanguageForMac parse failed: " + e.getMessage());
        }

        // fallback to global
        String global = getPrinterLanguageGlobal(ctx);
        return global != null ? global : "AUTO";
    }
    public static boolean applySavedRfidPower(Context ctx, UHFService device) {
        if (device == null) {
            Log.w(TAG, "applySavedRfidPower: device is null");
            return false;
        }

        int power = getRfidPower(ctx);
        if (power < 0 || power > 40) power = 30;

        String[] setters = {
                "setPower", "setOutputPower", "setRfPower",
                "setTxPower", "setPowerDbm"
        };

        for (String method : setters) {
            try {
                Method m = device.getClass().getMethod(method, int.class);
                m.invoke(device, power);
                Log.d(TAG, "RF Power applied using " + method + "(" + power + ")");
                return true;
            } catch (Exception ignored) {}
        }

        // Fallback method: setParameters(id, value)
        try {
            Method sp = device.getClass().getMethod("setParameters", int.class, int.class);
            int[] tryIds = {0, 1, 2, 3, 4, 18, 19};

            for (int id : tryIds) {
                try {
                    Object r = sp.invoke(device, id, power);
                    boolean ok = (r instanceof Boolean && (Boolean) r);
                    if (ok) {
                        Log.d(TAG, "RF Power applied using setParameters(" + id + "," + power + ")");
                        return true;
                    }
                } catch (Exception ignored2) {}
            }
        } catch (Exception ignored3) {}

        Log.w(TAG, "applySavedRfidPower: NO suitable setter found");
        return false;
    }

}
