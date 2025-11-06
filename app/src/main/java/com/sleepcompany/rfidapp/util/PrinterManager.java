package com.sleepcompany.rfidapp.util;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.sleepcompany.rfidapp.zpSDK.BluetoothSocket;

import java.io.UnsupportedEncodingException;

/**
 * Bluetooth-only PrinterManager for zpSDK printer helper using PrefHelper.
 * Ensures callbacks are always executed on the main (UI) thread.
 */
public class PrinterManager {

    public enum ConnectionType { NONE, BLUETOOTH }

    public interface PrinterCallback {
        void onPrinterConnected(@NonNull String deviceInfo);
        void onPrinterDisconnected();
        void onPrintSuccess();
        void onPrintError(@NonNull String error);
    }

    private final Context context;
    private final Activity activity;
    private final PrinterCallback callback;

    private ConnectionType currentConnection = ConnectionType.NONE;
    private boolean isConnected = false;
    private String deviceInfo = "";

    private final BluetoothAdapter btAdapter;
    private static final String ZGBK = "GBK";

    // Handler bound to main Looper so we can post callback notifications on UI thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PrinterManager(@NonNull Context context, @NonNull Activity activity, @NonNull PrinterCallback callback) {
        this.context = context.getApplicationContext();
        this.activity = activity;
        this.callback = callback;

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            postError("Bluetooth hardware not available");
        }
    }

    /** Connect to the saved printer from PrefHelper (synchronous ConnectPrinter call). */
    public void connectBluetooth() {
        String macAddress = PrefHelper.getPrinterMac(context);
        Log.d("PRINTER", "connectBluetooth: saved MAC = " + macAddress);
        Log.d("PRINTER", "connectBluetooth: all prefs =>\n" + PrefHelper.dumpAllPrefs(context));

        if (TextUtils.isEmpty(macAddress)) {
            postError("No saved printer found. Please save printer MAC first.");
            return;
        }

        if (btAdapter == null) {
            postError("Bluetooth hardware not available");
            return;
        }

        if (!btAdapter.isEnabled()) {
            postError("Bluetooth is disabled. Please enable Bluetooth.");
            return;
        }

        BluetoothDevice device;
        try {
            device = btAdapter.getRemoteDevice(macAddress);
        } catch (IllegalArgumentException e) {
            postError("Saved printer not found: " + e.getMessage());
            return;
        }

        boolean success;
        try {
            success = BluetoothSocket.getInstance().ConnectPrinter(macAddress);
        } catch (Throwable t) {
            success = false;
        }

        if (success) {
            currentConnection = ConnectionType.BLUETOOTH;
            isConnected = true;
            deviceInfo = device.getName() != null ? device.getName() : macAddress;
            postConnected("Bluetooth: " + deviceInfo);
        } else {
            isConnected = false;
            currentConnection = ConnectionType.NONE;
            Log.e("PRINTER", "Failed to connect to Bluetooth printer");
            //postError("Failed to connect to Bluetooth printer");
        }
    }

    /** Disconnect Bluetooth socket */
    public void disconnect() {
        if (currentConnection == ConnectionType.BLUETOOTH) {
            try {
                BluetoothSocket.getInstance().disconnect();
            } catch (Exception ignored) {}
        }
        currentConnection = ConnectionType.NONE;
        isConnected = false;
        deviceInfo = "";

        // clear saved printer when disconnecting
        //PrefHelper.clearPrinterMac(context);

        postDisconnected();
    }


    /* ---------------- printing API ---------------- */

    public void printText(@NonNull String text) {
        if (!ensureConnected()) return;

        new Thread(() -> {
            try {
                byte[] data = text.getBytes(ZGBK);
                BluetoothSocket.getInstance().Write(data); // may block
                // Give printer a short delay to finish
                Thread.sleep(200);
                postPrintSuccess(); // now safe to call auto-save
            } catch (Exception e) {
                postError("Print failed: " + e.getMessage());
            }
        }).start();
    }


    public void printProductLabel(String qaCode, String productName, String size, String stage, String status,String sku) {
        if (!ensureConnected()) return;
        Log.d("PRINTER", "Printing product label" + productName);
        String zpl = generateProductLabelZPL(qaCode, productName, size, stage, status,sku);
        printText(zpl);
    }

    public void checkPrinterStatus() {
        if (!ensureConnected()) return;
        byte[] statusCmd = new byte[]{0x1d, (byte) 0x99, 0x00, 0x00};
        printBytes(statusCmd);
    }

    private boolean ensureConnected() {
        if (!isConnected || currentConnection != ConnectionType.BLUETOOTH) {
            postError("Printer not connected (Bluetooth)");
            return false;
        }
        return true;
    }

    private void printBytes(byte[] bytes) {
        if (bytes == null) return;
        if (currentConnection == ConnectionType.BLUETOOTH) {
            BluetoothSocket.getInstance().Write(bytes);
        } else {
            postError("No bluetooth connection");
        }
    }

    /* ---------------- ZPL builders ---------------- */

    private String generateProductLabelZPL(String qaCode, String productName, String size, String stage, String status, String sku) {
        qaCode = qaCode == null ? "" : qaCode;
        productName = productName == null ? "" : productName;
        size = size == null ? "" : size;
        sku = sku == null ? "" : sku;

        return "^XA" +
                // Make text appear bolder
                "^MD30" +

                // QA Code — full left, font size 30
                "^FO50,40^A0N,30,30^FD" + escapeZpl(qaCode) + "^FS" +

                // QR Code (left side) — SMALL: model 2, magnification 3
                // Include QA, prefix to ensure full QA encoded
                "^FO50,120^BQN,2,3^FDQA," + escapeZpl(qaCode) + "^FS" +

                // All product detail text fields at FO200 with same font size (30) and bold appearance
                "^FO160,120^A0N,30,30^FDName: " + escapeZpl(productName) + "^FS" +
                "^FO160,170^A0N,30,30^FDSize: " + escapeZpl(size) + "^FS" +
                "^FO160,220^A0N,30,30^FDSKU: " + escapeZpl(sku) + "^FS" +
                // Uncomment if you want stage/status printed (same style)
                // "^FO200,270^A0N,30,30^FDStage: " + escapeZpl(stage) + "^FS" +
                // "^FO200,320^A0N,30,30^FDStatus: " + escapeZpl(status) + "^FS" +

                "^XZ";
    }




    private String escapeZpl(String s) {
        if (s == null) return "";
        return s.replace("\r", "").replace("\n", " ");
    }

    /* ---------------- main-thread callback helpers ---------------- */

    private void postConnected(final String info) {
        mainHandler.post(() -> {
            try { callback.onPrinterConnected(info); } catch (Exception ignored) {}
        });
    }

    private void postDisconnected() {
        mainHandler.post(() -> {
            try { callback.onPrinterDisconnected(); } catch (Exception ignored) {}
        });
    }

    private void postPrintSuccess() {
        mainHandler.post(() -> {
            try { callback.onPrintSuccess(); } catch (Exception ignored) {}
        });
    }

    private void postError(final String err) {
        mainHandler.post(() -> {
            try { callback.onPrintError(err); } catch (Exception ignored) {}
        });
    }

    /* ---------------- getters ---------------- */

    public boolean isConnected() {
        return isConnected;
    }


    public ConnectionType getCurrentConnection() {
        return currentConnection;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }
}
