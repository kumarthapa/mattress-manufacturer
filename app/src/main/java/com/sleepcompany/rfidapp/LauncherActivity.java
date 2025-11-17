package com.sleepcompany.rfidapp;

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

import com.sleepcompany.rfidapp.network.ApiService;
import com.sleepcompany.rfidapp.network.ApiClient;
import com.sleepcompany.rfidapp.network.UpdateCheckRequest;
import com.sleepcompany.rfidapp.network.UpdateCheckResponse;
import com.sleepcompany.rfidapp.network.MarkUpdatedRequest;
import com.sleepcompany.rfidapp.network.GenericResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;

public class LauncherActivity extends AppCompatActivity {

    private static final String PREFS = "update_prefs";
    private static final String KEY_PENDING_UPDATE = "pending_update";
    private static final String KEY_PENDING_LATEST = "pending_latest";
    private static final String KEY_PENDING_APK_URL = "pending_apk_url";
    private static final String KEY_PENDING_USE_PUBLIC = "pending_use_public";

    private static final int REQUEST_INSTALL_PERMISSION = 1200;

    private long downloadId = -1;
    private DownloadReceiver downloadReceiver;

    private AlertDialog progressDialog;
    private ProgressBar progressCircle;
    private TextView textPercent;

    private boolean isDownloading = false;
    private boolean usingPublicDownload = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FORCE DEV MODE BEFORE ANY NETWORK CALL
        ApiClient.setProduction(false);
        ApiClient.resetClients();

        setContentView(R.layout.activity_launcher);

        // 1) Check if we have a pending update that we've already installed (app restarted)
        int currentVersion = BuildConfig.VERSION_CODE;
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int pendingLatest = prefs.getInt(KEY_PENDING_LATEST, -1);
        boolean pending = prefs.getBoolean(KEY_PENDING_UPDATE, false);

        if (pending && pendingLatest > 0 && currentVersion >= pendingLatest) {
            // App has been updated (new version installed). Notify server.
            markDeviceUpdated(pendingLatest);
            // clear pending state
            prefs.edit()
                    .remove(KEY_PENDING_UPDATE)
                    .remove(KEY_PENDING_LATEST)
                    .remove(KEY_PENDING_APK_URL)
                    .remove(KEY_PENDING_USE_PUBLIC)
                    .apply();
            // proceed to login after marking
            goToLogin();
            return;
        }

        // 2) Otherwise call server to check if update required dynamically
        checkUpdateFromServer();
    }

    // ---------------------------
    // Server check
    // ---------------------------
    private void checkUpdateFromServer() {
        String androidId = getAndroidId(this);
        int currentVersion = BuildConfig.VERSION_CODE;

        UpdateCheckRequest req = new UpdateCheckRequest(androidId, currentVersion);
        ApiService api = ApiClient.getPublicClient().create(ApiService.class);

        api.checkUpdate(req).enqueue(new Callback<UpdateCheckResponse>() {
            @Override
            public void onResponse(Call<UpdateCheckResponse> call, Response<UpdateCheckResponse> response) {
                if (!isFinishing()) {
                    if (response.isSuccessful() && response.body() != null) {
                        UpdateCheckResponse body = response.body();
                        if (body.isUpdate_required() && currentVersion < body.getLatest_version_code()) {
                            // save pending info
                            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                    .putBoolean(KEY_PENDING_UPDATE, true)
                                    .putInt(KEY_PENDING_LATEST, body.getLatest_version_code())
                                    .putString(KEY_PENDING_APK_URL, body.getApk_url())
                                    .apply();

                            showUpdateDialog(body.getApk_url(), body.getLatest_version_code(), true, false);
                        } else {
                            // not required -> continue
                            goToLogin();
                        }
                    } else {
                        Toast.makeText(LauncherActivity.this, "Update check failed", Toast.LENGTH_SHORT).show();
                        goToLogin();
                    }
                }
            }

            @Override
            public void onFailure(Call<UpdateCheckResponse> call, Throwable t) {
                Log.e(TAG, "checkUpdate error: " + t.getMessage());
                Toast.makeText(LauncherActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    // ---------------------------
    // Dialog to prompt update
    // ---------------------------
    private void showUpdateDialog(String apkUrl, int latestVersion, boolean force, boolean usePublicDownload) {
        new AlertDialog.Builder(this)
                .setTitle("Update Available")
                .setMessage("A new version is available. Update now?")
                .setCancelable(!force)
                .setPositiveButton("Update Now", (d, w) -> {
                    // Check install permission for O+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!getPackageManager().canRequestPackageInstalls()) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                            Toast.makeText(this, "Enable install permission & try again.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    // mark pending and start download
                    SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
                    e.putBoolean(KEY_PENDING_UPDATE, true);
                    e.putInt(KEY_PENDING_LATEST, latestVersion);
                    e.putString(KEY_PENDING_APK_URL, apkUrl);
                    e.putBoolean(KEY_PENDING_USE_PUBLIC, usePublicDownload);
                    e.apply();

                    this.usingPublicDownload = usePublicDownload;
                    startDownload(apkUrl, usePublicDownload);
                })
                .setNegativeButton(force ? "Exit" : "Skip", (d, w) -> {
                    if (force) finish();
                    else goToLogin();
                })
                .show();
    }

    // ---------------------------
    // Start download (chooses destination for old vs modern devices)
    // ---------------------------
    private void startDownload(String apkUrl, boolean usePublic) {
        try {
            showProgressDialog();

            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(apkUrl));
            req.setTitle(getString(R.string.app_name) + " Update");
            req.setDescription("Downloading update…");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // on very old devices (Android 16) use public Download folder for installer compatibility
            if (usePublic || Build.VERSION.SDK_INT <= 16) {
                // public download
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "rfid_update.apk");
                usingPublicDownload = true;
            } else {
                // app-specific external files dir (FileProvider fallback)
                File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (dir != null) {
                    File apkFile = new File(dir, "rfid_update.apk");
                    req.setDestinationUri(Uri.fromFile(apkFile));
                } else {
                    // fallback to public
                    req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "rfid_update.apk");
                    usingPublicDownload = true;
                }
            }

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            downloadId = dm.enqueue(req);

            registerDownloadReceiver();
            startProgressUpdater();

        } catch (Exception ex) {
            hideProgressDialog();
            Log.e(TAG, "startDownload error: " + ex.getMessage(), ex);
            Toast.makeText(this, "Download failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            goToLogin();
        }
    }

    // ---------------------------
    // Progress UI
    // ---------------------------
    private void showProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_update_download, null);
        progressCircle = view.findViewById(R.id.progressCircular);
        textPercent = view.findViewById(R.id.textPercent);
        builder.setView(view);
        builder.setCancelable(false);
        progressDialog = builder.create();
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    // ---------------------------
    // Progress updater thread
    // ---------------------------
    private void startProgressUpdater() {
        isDownloading = true;

        new Thread(() -> {
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            while (isDownloading) {
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(downloadId);
                Cursor cursor = dm.query(q);

                if (cursor != null && cursor.moveToFirst()) {
                    int downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int total = cursor.getInt(cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    if (total > 0) {
                        int percent = (int) ((downloaded * 100L) / total);
                        runOnUiThread(() -> {
                            progressCircle.setIndeterminate(false);
                            progressCircle.setMax(100);
                            progressCircle.setProgress(percent);
                            textPercent.setText(percent + "%");
                        });
                    }
                    cursor.close();
                }

                try { Thread.sleep(300); } catch (Exception ignored) {}
            }
        }).start();
    }

    // ---------------------------
    // Register download receiver
    // ---------------------------
    private void registerDownloadReceiver() {
        if (downloadReceiver == null) {
            downloadReceiver = new DownloadReceiver();
            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            else
                registerReceiver(downloadReceiver, filter);
        }
    }

    private void unregisterDownloadReceiver() {
        if (downloadReceiver != null) {
            try { unregisterReceiver(downloadReceiver); } catch (Exception ignored) {}
            downloadReceiver = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDownloading = false;
        unregisterDownloadReceiver();
        hideProgressDialog();
    }

    // ---------------------------
    // Download complete receiver
    // ---------------------------
    public class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadId) {
                isDownloading = false;
                hideProgressDialog();

                // prompt install depending on target device
                promptInstall();
            }
        }
    }

    // ---------------------------
    // Prompt installer (handles old + modern)
    // ---------------------------
    private void promptInstall() {
        try {
            File apk;
            if (usingPublicDownload) {
                apk = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "rfid_update.apk");
            } else {
                apk = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "rfid_update.apk");
            }

            if (!apk.exists()) {
                Toast.makeText(this, "Downloaded APK not found!", Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !usingPublicDownload) {
                // modern devices: use FileProvider
                Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apk);
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                // old devices: use file:// URI
                Uri uri = Uri.fromFile(apk);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "promptInstall error: " + e.getMessage(), e);
            Toast.makeText(this, "Install error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------
    // Mark updated API call
    // ---------------------------
    private void markDeviceUpdated(int installedVersion) {
        String androidId = getAndroidId(this);
        MarkUpdatedRequest req = new MarkUpdatedRequest(androidId, installedVersion);
        ApiService api = ApiClient.getPublicClient().create(ApiService.class);

        api.markUpdated(req).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.i(TAG, "Marked device as updated on server.");
                } else {
                    Log.w(TAG, "Failed to mark updated on server.");
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Log.e(TAG, "markUpdated error: " + t.getMessage());
            }
        });
    }

    // ---------------------------
    // Utility
    // ---------------------------
    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    public static String getAndroidId(Context ctx) {
        try {
            String id = android.provider.Settings.Secure.getString(ctx.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            return id != null ? id : "";
        } catch (Exception e) {
            Log.w(TAG, "Couldn't read ANDROID_ID: " + e.getMessage());
            return "";
        }
    }
}
