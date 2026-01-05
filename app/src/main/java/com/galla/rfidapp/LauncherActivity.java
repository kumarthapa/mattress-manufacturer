package com.galla.rfidapp;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.galla.rfidapp.network.ApiService;
import com.galla.rfidapp.network.ApiClient;
import com.galla.rfidapp.network.UpdateCheckRequest;
import com.galla.rfidapp.network.UpdateCheckResponse;
import com.galla.rfidapp.network.MarkUpdatedRequest;
import com.galla.rfidapp.network.GenericResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;

public class LauncherActivity extends AppCompatActivity {

    private static final String PREFS = "update_prefs";
    private static final String KEY_PENDING_UPDATE = "pending_update";
    private static final String KEY_PENDING_LATEST = "pending_latest";
    private static final String KEY_PENDING_APK_URL = "pending_apk_url";
    private static final String KEY_PENDING_PUBLIC = "pending_public";

    public static final String EXTRA_NEXT_SCREEN = "next_screen";

    private long downloadId = -1;
    private boolean isDownloading = false;
    private boolean usingPublicDownload = false;

    private AlertDialog progressDialog;
    private ProgressBar progressCircle;
    private TextView textPercent;

    private DownloadReceiver downloadReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ApiClient.setProduction(true);   // enable PRODUCTION mode
        ApiClient.resetClients();

        setContentView(R.layout.activity_launcher);

        Log.i(TAG, "LauncherActivity started");

        // 1) Check if pending update was installed — send current device version to server if pending.
        if (handlePendingUpdateInstalled()) return;

        // 2) Otherwise check update from server (non-blocking)
        checkUpdateFromServer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister receiver if registered
        try {
            if (downloadReceiver != null) {
                unregisterReceiver(downloadReceiver);
                downloadReceiver = null;
                Log.i(TAG, "DownloadReceiver unregistered");
            }
        } catch (IllegalArgumentException ignored) {
            // Not registered — ignore
        }
    }

    // ---------------------------------------------------------
    // (A) HANDLE PENDING UPDATE INSTALLED
    // ---------------------------------------------------------
    private boolean handlePendingUpdateInstalled() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean pending = prefs.getBoolean(KEY_PENDING_UPDATE, false);

        if (pending) {
            int currentVersion = BuildConfig.VERSION_CODE;

            Log.i(TAG, "Pending update found. Reporting current installed version: " + currentVersion);

            // Inform server that this device now has currentVersion installed
            markDeviceUpdated(currentVersion);

            // Clear pending flags
            prefs.edit().clear().apply();

            // Continue normal flow
            redirectToNext();
            return true;
        }

        Log.i(TAG, "No pending update found");
        return false;
    }

    // ---------------------------------------------------------
    // (B) CHECK UPDATE FROM SERVER
    // ---------------------------------------------------------
    private void checkUpdateFromServer() {

        UpdateCheckRequest req = new UpdateCheckRequest(
                getAndroidId(this),
                BuildConfig.VERSION_CODE
        );

        ApiService api = ApiClient.getPublicClient().create(ApiService.class);

        Log.i(TAG, "Checking update from server for device: " + getAndroidId(this) + " currentVersion: " + BuildConfig.VERSION_CODE);

        api.checkUpdate(req).enqueue(new Callback<UpdateCheckResponse>() {
            @Override
            public void onResponse(Call<UpdateCheckResponse> call, Response<UpdateCheckResponse> resp) {

                if (!resp.isSuccessful() || resp.body() == null) {
                    Log.w(TAG, "checkUpdate response unsuccessful or empty, code: " + (resp != null ? resp.code() : "null"));
                    redirectToNext();
                    return;
                }

                UpdateCheckResponse body = resp.body();

                Log.i(TAG, "checkUpdate response body: update_required=" + body.isUpdate_required()
                        + ", latest_version_code=" + body.getLatest_version_code()
                        + ", apk_url=" + body.getApk_url());

                // If the server doesn't require update, continue
                if (!body.isUpdate_required()) {
                    redirectToNext();
                    return;
                }

                // Server requested an update. Save pending state (no extra checks).
                SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
                e.putBoolean(KEY_PENDING_UPDATE, true);
                e.putInt(KEY_PENDING_LATEST, body.getLatest_version_code());
                e.putString(KEY_PENDING_APK_URL, body.getApk_url());
                e.apply();

                Log.i(TAG, "Saved pending update info to prefs and showing dialog");

                showUpdateDialog(body.getApk_url(), body.getLatest_version_code());
            }

            @Override
            public void onFailure(Call<UpdateCheckResponse> call, Throwable t) {
                Log.w(TAG, "Update check failed: " + (t != null ? t.getMessage() : "unknown"), t);
                redirectToNext();
            }
        });
    }

    // ---------------------------------------------------------
    // (C) UPDATE DIALOG
    // ---------------------------------------------------------
    private void showUpdateDialog(String apkUrl, int latestVersion) {

        new AlertDialog.Builder(this)
                .setTitle("Update Available")
                .setMessage("A new version is available. Update now?")
                .setCancelable(false)
                .setPositiveButton("Update", (d, w) -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                            !getPackageManager().canRequestPackageInstalls()) {

                        startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + getPackageName())));

                        Toast.makeText(this, "Enable permission and retry.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    downloadApk(apkUrl);
                })
                .setNegativeButton("Skip", (d, w) -> {
                    // If user skips, still clear pending state so they aren't forced next app start.
                    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                    prefs.edit().clear().apply();
                    redirectToNext();
                })
                .show();
    }

    // ---------------------------------------------------------
    // (D) DOWNLOAD APK
    // ---------------------------------------------------------
    private void downloadApk(String apkUrl) {
        showProgressDialog();

        if (apkUrl == null || apkUrl.isEmpty()) {
            Toast.makeText(this, "Invalid APK URL", Toast.LENGTH_LONG).show();
            hideProgressDialog();
            redirectToNext();
            return;
        }

        String fileName = apkUrl.substring(apkUrl.lastIndexOf("/") + 1);
        if (fileName == null || fileName.isEmpty()) fileName = "rfid_update.apk";

        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(apkUrl));
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        req.setTitle("Downloading update...");
        req.setDescription("Updating application");

        File dest;

        if (Build.VERSION.SDK_INT <= 16) {
            // Very old devices — use public downloads
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            usingPublicDownload = true;
        } else {
            // Modern devices — use app-private external files dir
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            dest = new File(dir, fileName);
            req.setDestinationUri(Uri.fromFile(dest));
            usingPublicDownload = false;
        }

        // Save the public/private choice and apk url so installApk() can pick correct location
        SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        e.putBoolean(KEY_PENDING_PUBLIC, usingPublicDownload);
        e.putString(KEY_PENDING_APK_URL, apkUrl);
        e.putBoolean(KEY_PENDING_UPDATE, true); // ensure pending remains true during download
        e.apply();

        Log.i(TAG, "Enqueuing download: fileName=" + fileName + " usingPublicDownload=" + usingPublicDownload);

        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadId = dm.enqueue(req);

        registerReceiver();
        startProgressUpdater();
        isDownloading = true;
    }

    // ---------------------------------------------------------
    // (E) INSTALL APK
    // ---------------------------------------------------------
    private void installApk() {

        // Load pending update data
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String apkUrl = prefs.getString(KEY_PENDING_APK_URL, "");
        boolean savedPublic = prefs.getBoolean(KEY_PENDING_PUBLIC, false);

        // Extract filename from URL (e.g., app-debug.apk)
        String fileName = "";
        if (apkUrl != null && apkUrl.lastIndexOf("/") != -1) {
            fileName = apkUrl.substring(apkUrl.lastIndexOf("/") + 1);
        }

        if (fileName.isEmpty()) {
            Log.w(TAG, "installApk: filename empty, aborting");
            Toast.makeText(this, "APK filename not found", Toast.LENGTH_LONG).show();
            return;
        }

        File apk;
        if (savedPublic || usingPublicDownload) {
            // APK downloaded to public storage (/Download)
            apk = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        } else {
            // APK downloaded to app-private storage
            apk = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
        }

        Log.i(TAG, "Installing APK from path: " + apk.getAbsolutePath());

        if (!apk.exists()) {
            Toast.makeText(this, "APK not found: " + apk.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.w(TAG, "APK not found at path: " + apk.getAbsolutePath());
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apk);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // ---------------------------------------------------------
    // (F) PENDING UPDATE CONFIRM WITH SERVER
    // ---------------------------------------------------------
    private void markDeviceUpdated(int version) {
        ApiService api = ApiClient.getPublicClient().create(ApiService.class);
        Log.i(TAG, "Calling markUpdated with version: " + version);
        api.markUpdated(new MarkUpdatedRequest(getAndroidId(this), version))
                .enqueue(new Callback<GenericResponse>() {
                    @Override public void onResponse(Call<GenericResponse> call, Response<GenericResponse> resp) {
                        Log.i(TAG, "markUpdated response: code=" + (resp != null ? resp.code() : -1)
                                + " body=" + (resp != null && resp.body() != null ? resp.body().toString() : "null"));
                    }
                    @Override public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Log.w(TAG, "markUpdated failed: " + (t != null ? t.getMessage() : "unknown"));
                    }
                });
    }

    // ---------------------------------------------------------
    // (G) AUTO REDIRECT AFTER CHECK
    // ---------------------------------------------------------
    private void redirectToNext() {
        String next = getIntent().getStringExtra(EXTRA_NEXT_SCREEN);

        if (next != null) {
            try {
                Class<?> cls = Class.forName(next);
                startActivity(new Intent(this, cls));
                finish();
                return;
            } catch (Exception ignored) {}
        }

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // ---------------------------------------------------------
    // (H) UTIL
    // ---------------------------------------------------------
    public static String getAndroidId(Context ctx) {
        return Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    // ---------------------------------------------------------
    // RECEIVER + PROGRESS
    // ---------------------------------------------------------
    private void registerReceiver() {
        if (downloadReceiver == null) {
            downloadReceiver = new DownloadReceiver();
            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(downloadReceiver, filter);
            }
            Log.i(TAG, "DownloadReceiver registered");
        }
    }

    private void startProgressUpdater() {
        new Thread(() -> {
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            while (isDownloading) {
                try {
                    Cursor c = dm.query(new DownloadManager.Query().setFilterById(downloadId));
                    if (c != null && c.moveToFirst()) {
                        long total = c.getLong(c.getColumnIndexOrThrow(
                                DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        long downloaded = c.getLong(c.getColumnIndexOrThrow(
                                DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

                        int percent = total > 0 ? (int) ((downloaded * 100) / total) : 0;

                        runOnUiThread(() -> {
                            if (progressCircle != null) progressCircle.setProgress(percent);
                            if (textPercent != null) textPercent.setText(percent + "%");
                        });

                        c.close();
                    }
                } catch (Exception ex) {
                    Log.w(TAG, "Progress updater exception: " + ex.getMessage(), ex);
                }

                try { Thread.sleep(250); } catch (Exception ignored) {}
            }
        }).start();
    }

    private void showProgressDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_update_download, null);
        progressCircle = view.findViewById(R.id.progressCircular);
        textPercent = view.findViewById(R.id.textPercent);

        progressDialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();
        progressDialog.show();
    }

    public class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            Log.i(TAG, "DownloadReceiver onReceive id=" + id + " expected=" + downloadId);

            if (id == downloadId) {
                isDownloading = false;
                hideProgressDialog();
                installApk();
            }
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }
}
