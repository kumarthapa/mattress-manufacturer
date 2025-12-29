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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Flexible PrinterManager supporting ZPL (via existing zpSDK or RFCOMM fallback),
 * TSPL (native RFCOMM) and ESC_POS (native RFCOMM).
 *
 * - Chooses command language per-MAC via PrefHelper.getPrinterLanguageForMac(...)
 * - Uses zpSDK when available for ZPL (keeps compatibility), otherwise uses native socket
 * - Ensures callbacks are posted on main thread
 * - Only allows connections for MACs in ALLOWED_MACS (app locked to selected printers)
 */
public class PrinterManager {

    public enum ConnectionType { NONE, BLUETOOTH }
    // optional language keys (strings used in PrefHelper)
    private static final String LANG_AUTO = "AUTO";
    private static final String LANG_ZPL = "ZPL";
    private static final String LANG_TSPL = "TSPL";
    private static final String LANG_ESC = "ESC_POS";

    // ---------- ONLY ALLOWED MACS (app will block other printers) ----------
    private static final String[] ALLOWED_MACS = new String[]{
            "DC:0D:30:1F:65:20",   // DCode DC-3M (example) TSPL
            "DC:0D:30:1F:65:BD",   // DCode DC-3M (example) TSPL
            "DC:0D:30:1F:63:10",   // DCode DC-3M (example) TSPL
            "DC:0D:30:1F:63:E7",   // DCode DC-3M (example) TSPL
            "DC:0D:30:1F:62:F1",   // DCode DC-3M (example) TSPL ------- new
            "00:32:04:81:17:45",  // CC3 ZPL
            "00:40:43:37:68:21",  // CC3 ZPL
            "C0:40:43:37:68:21",  // CC3 ZPL
            "00:42:18:85:58:42",  // CC3 ZPL
            "44:B7:D0:2B:FC:D6",  // Desktop printer --- New
    };
    // ---------------------------------------------------------------------

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

    // whether current connection uses vendor SDK (zpSDK) or native RFCOMM socket
    private boolean usingSdk = false;

    // native RFCOMM socket (android.bluetooth.BluetoothSocket) kept closed when not used
    private android.bluetooth.BluetoothSocket nativeSocket = null;

    private final BluetoothAdapter btAdapter;
    private static final String DEFAULT_CHARSET_ZPL = "UTF-8";
    private static final String DEFAULT_CHARSET_TSPL = "GBK";
    private static final String DEFAULT_CHARSET_ESC = "UTF-8";

    // Handler bound to main Looper so we can post callback notifications on UI thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // SPP UUID for RFCOMM
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public PrinterManager(@NonNull Context context, @NonNull Activity activity, @NonNull PrinterCallback callback) {
        this.context = context.getApplicationContext();
        this.activity = activity;
        this.callback = callback;

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            postError("Bluetooth hardware not available");
        }

        // -------------------- PREDEFINED PRINTER LANGUAGE MAP --------------------
        // This writes predefined MAC->LANG mappings into PrefHelper so PrinterManager
        // can pick protocol automatically for known MACs.
        try {
            Map<String, String> predefined = new HashMap<>();
            predefined.put("DC:0D:30:1F:65:20", LANG_TSPL);     // DCode DC-3M
            predefined.put("DC:0D:30:1F:65:BD", LANG_TSPL);     // DCode DC-3M
            predefined.put("DC:0D:30:1F:63:10", LANG_TSPL);     // DCode DC-3M
            predefined.put("DC:0D:30:1F:63:E7", LANG_TSPL);     // DCode DC-3M -----------
            predefined.put("DC:0D:30:1F:62:F1", LANG_TSPL);     // DCode DC-3M -----------new
            predefined.put("00:32:04:81:17:45", LANG_ZPL);      // CC3 ZPL
            predefined.put("00:40:43:37:68:21", LANG_ZPL);      // CC3 ZPL
            predefined.put("C0:40:43:37:68:21", LANG_ZPL);      // CC3 ZPL
            predefined.put("00:42:18:85:58:42", LANG_ZPL);      // CC3 ZPL
            predefined.put("44:B7:D0:2B:FC:D6", LANG_AUTO);      // Desktop printer -- new

            //predefined.put("11:22:33:44:55:66", LANG_ESC);      // Generic thermal receipt

            for (Map.Entry<String, String> entry : predefined.entrySet()) {
                try {
                    Log.i("PRINTER", "Saving predefined lang for " + entry.getKey() + " = " + entry.getValue());
                    PrefHelper.savePrinterLanguageForMac(this.context, entry.getKey(), entry.getValue());
                } catch (Throwable t) {
                    Log.w("PRINTER", "Failed to save predefined lang for " + entry.getKey() + ": " + t.getMessage());
                }
            }
            Log.i("PRINTER", "Predefined printer language map loaded.");
        } catch (Throwable ignored) {}
        // -------------------------------------------------------------------------
    }

    /**
     * Connect using saved MAC in PrefHelper.
     * Behavior:
     * - check allowed MACs (reject if not allowed)
     * - read language for mac via PrefHelper.getPrinterLanguageForMac
     * - if ZPL: try zpSDK.ConnectPrinter(mac) then fallback to native RFCOMM
     * - if TSPL/ESC_POS/AUTO: use native RFCOMM
     *
     * This method is synchronous (blocks while attempting a connection) and posts callbacks on UI thread.
     */
    public void connectBluetooth() {
        String macAddress = PrefHelper.getPrinterMac(context);
        Log.d("PRINTER", "connectBluetooth: saved MAC = " + macAddress);
        Log.d("PRINTER", "connectBluetooth: all prefs =>\n" + PrefHelper.dumpAllPrefs(context));

        if (TextUtils.isEmpty(macAddress)) {
            postError("No saved printer found. Please save printer MAC first.");
            return;
        }

        // ---------- CHECK IF PRINTER IS ALLOWED ----------
        if (!isMacAllowed(macAddress)) {
            postError("Printer not supported. This app works only with approved printers.");
            Log.e("PRINTER", "Blocked unsupported printer MAC: " + macAddress);
            return;
        }
        // --------------------------------------------------

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

        // determine language for this Mac
        String lang = null;
        try {
            lang = PrefHelper.getPrinterLanguageForMac(context, macAddress);
        } catch (Throwable t) {
            Log.w("PRINTER", "getPrinterLanguageForMac threw: " + t.getMessage());
        }
        if (lang == null) {
            try { lang = PrefHelper.getPrinterLanguageGlobal(context); } catch (Throwable ignored) {}
        }
        if (lang == null) lang = LANG_AUTO;

        boolean connected = false;
        String info = macAddress;

        // If ZPL, first try vendor SDK (zpSDK), because your project may expect that for some printers
        if (LANG_ZPL.equals(lang)) {
            try {
                boolean ok = BluetoothSocket.getInstance().ConnectPrinter(macAddress);
                if (ok) {
                    usingSdk = true;
                    currentConnection = ConnectionType.BLUETOOTH;
                    isConnected = true;
                    deviceInfo = device.getName() != null ? device.getName() : macAddress;
                    postConnected("Bluetooth (SDK): " + deviceInfo);
                    Log.i("PRINTER", "Connected via zpSDK to " + macAddress);
                    return; // done
                } else {
                    Log.w("PRINTER", "zpSDK ConnectPrinter returned false, will try native RFCOMM fallback");
                }
            } catch (Throwable t) {
                Log.w("PRINTER", "zpSDK ConnectPrinter threw: " + t.getMessage(), t);
            }
        }

        // Otherwise (TSPL/ESC/AUTO) or if SDK failed, try native RFCOMM
        try {
            connected = nativeConnect(device);
            if (connected) {
                usingSdk = false;
                currentConnection = ConnectionType.BLUETOOTH;
                isConnected = true;
                deviceInfo = device.getName() != null ? device.getName() : macAddress;
                postConnected("Bluetooth (RFCOMM): " + deviceInfo);
                Log.i("PRINTER", "Connected via native RFCOMM to " + macAddress);
            } else {
                isConnected = false;
                currentConnection = ConnectionType.NONE;
                Log.e("PRINTER", "Native RFCOMM connect returned false");
                postError("Failed to connect to printer (native RFCOMM).");
            }
        } catch (Exception e) {
            isConnected = false;
            currentConnection = ConnectionType.NONE;
            Log.e("PRINTER", "Native RFCOMM connect failed: " + e.getMessage(), e);
            postError("Failed to connect to printer: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    /** Disconnect both SDK socket and native socket if connected */
    public void disconnect() {
        if (usingSdk) {
            try {
                BluetoothSocket.getInstance().disconnect();
            } catch (Exception ignored) {}
            usingSdk = false;
        }
        if (nativeSocket != null) {
            try {
                nativeSocket.close();
            } catch (Exception ignored) {}
            nativeSocket = null;
        }
        currentConnection = ConnectionType.NONE;
        isConnected = false;
        deviceInfo = "";
        postDisconnected();
    }

    /* ---------------- printing API ---------------- */

    /**
     * Generic print method — chooses encoding and transport based on saved language for MAC.
     * If using SDK, writes via SDK. Otherwise writes to native RFCOMM OutputStream.
     */
    public void printText(@NonNull String text) {
        if (!ensureConnected()) return;

        new Thread(() -> {
            String mac = PrefHelper.getPrinterMac(context);
            String lang = null;
            try { lang = PrefHelper.getPrinterLanguageForMac(context, mac); } catch (Throwable ignored) {}
            if (lang == null) {
                try { lang = PrefHelper.getPrinterLanguageGlobal(context); } catch (Throwable ignored) {}
            }
            if (lang == null) lang = LANG_AUTO;

            String charset = charsetForLang(lang);

            try {
                byte[] data;
                try {
                    data = text.getBytes(charset);
                } catch (UnsupportedEncodingException uex) {
                    Log.w("PRINTER", "Unsupported charset " + charset + ", fallback to UTF-8", uex);
                    data = text.getBytes(DEFAULT_CHARSET_ZPL);
                }

                if (usingSdk) {
                    // Use vendor SDK Write
                    try {
                        BluetoothSocket.getInstance().Write(data);
                    } catch (Throwable t) {
                        Log.e("PRINTER", "SDK Write failed", t);
                        postError("SDK Write failed: " + t.getMessage());
                        return;
                    }
                } else {
                    // native RFCOMM
                    if (nativeSocket == null) {
                        postError("Native bluetooth socket is null");
                        return;
                    }
                    OutputStream os = nativeSocket.getOutputStream();
                    os.write(data);
                    os.flush();
                }

                // short pause to allow printer to finish
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                postPrintSuccess();
            } catch (Exception e) {
                Log.e("PRINTER", "Print failed", e);
                postError("Print failed: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Print product label. Chooses generator based on saved language for MAC.
     */
    public void printProductLabel(String qaCode, String productName, String size, String stage, String status, String sku, String referenceCode) {
        if (!ensureConnected()) return;

        String mac = PrefHelper.getPrinterMac(context);
        String lang = null;
        try { lang = PrefHelper.getPrinterLanguageForMac(context, mac); } catch (Throwable ignored) {}
        if (lang == null) {
            try { lang = PrefHelper.getPrinterLanguageGlobal(context); } catch (Throwable ignored) {}
        }
        if (lang == null) lang = LANG_AUTO;

        if (LANG_TSPL.equals(lang)) {
            String tspl = generateProductLabelTSPL(qaCode, productName, size, stage, status, sku, referenceCode);
            printText(tspl);
        } else if (LANG_ESC.equals(lang)) {
            String esc = generateProductLabelEscPos(qaCode, productName, size, stage, status, sku, referenceCode);
            printText(esc);
        } else { // default ZPL
            String zpl = generateProductLabelZPL(qaCode, productName, size, stage, status, sku, referenceCode);
            printText(zpl);
        }
    }

    public void checkPrinterStatus() {
        if (!ensureConnected()) return;

        // If using SDK try existing status command, otherwise attempt a generic status (may not be supported)
        if (usingSdk) {
            byte[] statusCmd = new byte[]{0x1d, (byte) 0x99, 0x00, 0x00};
            printBytes(statusCmd);
        } else {
            // TSPL: send "~HS" for host status (not standardized on all printers) — keep simple: no-op or custom impl
            try {
                String mac = PrefHelper.getPrinterMac(context);
                String lang = null;
                try { lang = PrefHelper.getPrinterLanguageForMac(context, mac); } catch (Throwable ignored) {}
                if (LANG_TSPL.equals(lang)) {
                    printText("~HS\n");
                } else {
                    printText("\u001B" + "v"); // placeholder ESC sequence
                }
            } catch (Exception e) {
                postError("Status check not supported: " + e.getMessage());
            }
        }
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
        if (!ensureConnected()) return;

        if (usingSdk) {
            try {
                BluetoothSocket.getInstance().Write(bytes);
            } catch (Throwable t) {
                postError("SDK Write failed: " + t.getMessage());
            }
        } else {
            try {
                if (nativeSocket == null) {
                    postError("Native socket null");
                    return;
                }
                OutputStream os = nativeSocket.getOutputStream();
                os.write(bytes);
                os.flush();
            } catch (Exception e) {
                postError("Native write failed: " + e.getMessage());
            }
        }
    }

    /* ---------------- Label generators ---------------- */

    /**
     * ZPL generator (keeps your existing layout)
     */
    // ---------------------- MEDIUM BOLD ----------------------------
    private String generateProductLabelZPL(
            String qaCode,
            String productName,
            String size,
            String stage,
            String status,
            String sku,
            String referenceCode
    ) {
        qaCode = qaCode == null ? "" : qaCode;
        productName = productName == null ? "" : productName;
        size = size == null ? "" : size;
        sku = sku == null ? "" : sku;
        referenceCode = referenceCode == null ? "" : referenceCode;

        return "^XA" +

                // 🔥 Ultra dark print (safe range)
                "^MD40" +

                // ================= QA CODE =================
                "^FO50,40^A0N,30,30^FD" + escapeZpl(qaCode) + "^FS" +
                "^FO51,40^A0N,30,30^FD" + escapeZpl(qaCode) + "^FS" +

                // ================= QR CODE =================
                "^FO50,120^BQN,2,3^FDQA," + escapeZpl(qaCode) + "^FS" +

                // ================= NAME =================
                "^FO160,120^A0N,30,30^FDName: " + escapeZpl(productName) + "^FS" +
                "^FO161,120^A0N,30,30^FDName: " + escapeZpl(productName) + "^FS" +

                // ================= SIZE =================
                "^FO160,170^A0N,30,30^FDSize: " + escapeZpl(size) + "^FS" +
                "^FO161,170^A0N,30,30^FDSize: " + escapeZpl(size) + "^FS" +

                // ================= SKU =================
                "^FO160,220^A0N,30,30^FDSKU: " + escapeZpl(sku) + "^FS" +
                "^FO161,220^A0N,30,30^FDSKU: " + escapeZpl(sku) + "^FS" +

                // ================= REF CODE =================
                "^FO160,270^A0N,30,30^FDRef Code: " + escapeZpl(referenceCode) + "^FS" +
                "^FO161,270^A0N,30,30^FDRef Code: " + escapeZpl(referenceCode) + "^FS" +

                "^XZ";
    }
// ---------------------- UlTRA BOLD ----------------------------
//    private String generateProductLabelZPL(
//            String qaCode,
//            String productName,
//            String size,
//            String stage,
//            String status,
//            String sku,
//            String referenceCode
//    ) {
//        qaCode = qaCode == null ? "" : qaCode;
//        productName = productName == null ? "" : productName;
//        size = size == null ? "" : size;
//        sku = sku == null ? "" : sku;
//        referenceCode = referenceCode == null ? "" : referenceCode;
//
//        return "^XA" +
//
//                // 🔥 Maximum safe darkness (203 DPI printers)
//                "^MD42" +
//
//                // ================= QA CODE =================
//                "^FO50,40^A0N,30,30^FD" + escapeZpl(qaCode) + "^FS" +
//                "^FO51,40^A0N,30,30^FD" + escapeZpl(qaCode) + "^FS" +
//                "^FO52,40^A0N,30,30^FD" + escapeZpl(qaCode) + "^FS" +
//
//                // ================= QR CODE =================
//                "^FO50,120^BQN,2,3^FDQA," + escapeZpl(qaCode) + "^FS" +
//
//                // ================= NAME =================
//                "^FO160,120^A0N,30,30^FDName: " + escapeZpl(productName) + "^FS" +
//                "^FO161,120^A0N,30,30^FDName: " + escapeZpl(productName) + "^FS" +
//                "^FO162,120^A0N,30,30^FDName: " + escapeZpl(productName) + "^FS" +
//
//                // ================= SIZE =================
//                "^FO160,170^A0N,30,30^FDSize: " + escapeZpl(size) + "^FS" +
//                "^FO161,170^A0N,30,30^FDSize: " + escapeZpl(size) + "^FS" +
//                "^FO162,170^A0N,30,30^FDSize: " + escapeZpl(size) + "^FS" +
//
//                // ================= SKU =================
//                "^FO160,220^A0N,30,30^FDSKU: " + escapeZpl(sku) + "^FS" +
//                "^FO161,220^A0N,30,30^FDSKU: " + escapeZpl(sku) + "^FS" +
//                "^FO162,220^A0N,30,30^FDSKU: " + escapeZpl(sku) + "^FS" +
//
//                // ================= REF CODE =================
//                "^FO160,270^A0N,30,30^FDRef Code: " + escapeZpl(referenceCode) + "^FS" +
//                "^FO161,270^A0N,30,30^FDRef Code: " + escapeZpl(referenceCode) + "^FS" +
//                "^FO162,270^A0N,30,30^FDRef Code: " + escapeZpl(referenceCode) + "^FS" +
//
//                "^XZ";
//    }

    private String escapeZpl(String s) {
        if (s == null) return "";
        return s.replace("\r", "").replace("\n", " ");
    }

    /**
     * TSPL (TSC/DC-3M) label generator — mirrors ZPL layout: QR on left, text on right.
     * Adjust coordinates if needed for your label size / printer.
     */
    /**
     * TSPL (TSC/DC-3M) label generator — mirrors ZPL layout:
     *
     * ZPL layout (for reference):
     *  ^FO50,40  -> QA text (top-left)
     *  ^FO50,120 -> QR code (left)
     *  ^FO160,120 -> Name:
     *  ^FO160,170 -> Size:
     *  ^FO160,220 -> SKU:
     *  ^FO160,270 -> Ref Code:
     *
     * Here we assume 203 dpi (8 dots/mm), so TSPL coordinates in dots
     * are basically the same numbers as ZPL FO values.
     */
    private String generateProductLabelTSPL(String qaCode, String productName, String size,
                                            String stage, String status, String sku,
                                            String referenceCode) {

        qaCode = qaCode == null ? "" : qaCode;
        productName = productName == null ? "" : productName;
        size = size == null ? "" : size;
        sku = sku == null ? "" : sku;
        referenceCode = referenceCode == null ? "" : referenceCode;

        StringBuilder sb = new StringBuilder();

        // Header (same as your code)
        sb.append("SIZE 60 mm,40 mm\r\n");
        sb.append("GAP 2 mm,0 mm\r\n");
        sb.append("DIRECTION 0,0\r\n");
        sb.append("REFERENCE 0,0\r\n");
        sb.append("CLS\r\n");

        // QA Code text (bold kept because font “3” naturally prints bold)
        sb.append("TEXT 50,40,\"3\",0,1,1,\"")
                .append(escapeTspl(qaCode))
                .append("\"\r\n");

        // ⭐ FIXED QR CODE — correct size & scannable
        sb.append("QRCODE 50,120,L,4,0,0,\"QA,")
                .append(escapeTspl(qaCode))
                .append("\"\r\n");

        // Details text (boldness kept)
        sb.append("TEXT 160,120,\"3\",0,1,1,\"Name: ")
                .append(escapeTspl(productName))
                .append("\"\r\n");

        sb.append("TEXT 160,170,\"3\",0,1,1,\"Size: ")
                .append(escapeTspl(size))
                .append("\"\r\n");

        sb.append("TEXT 160,220,\"3\",0,1,1,\"SKU: ")
                .append(escapeTspl(sku))
                .append("\"\r\n");

        sb.append("TEXT 160,270,\"3\",0,1,1,\"Ref Code: ")
                .append(escapeTspl(referenceCode))
                .append("\"\r\n");

        sb.append("PRINT 1\r\n");

        return sb.toString();
    }

    private String escapeTspl(String s) {
        if (s == null) return "";
        return s.replace("\r", "").replace("\n", " ");
    }


    /**
     * ESC/POS simple generator (receipt-style). If you need advanced image or barcode printing,
     * implement accordingly for your ESC/POS printer model.
     */
    private String generateProductLabelEscPos(String qaCode, String productName, String size, String stage, String status, String sku, String referenceCode) {
        StringBuilder sb = new StringBuilder();
        sb.append(productName == null ? "" : productName).append("\n");
        sb.append("QA: ").append(qaCode == null ? "" : qaCode).append("\n");
        sb.append("Size: ").append(size == null ? "" : size).append("\n");
        sb.append("SKU: ").append(sku == null ? "" : sku).append("\n");
        sb.append("Ref: ").append(referenceCode == null ? "" : referenceCode).append("\n\n");
        sb.append("\n\n");
        return sb.toString();
    }

    /* ---------------- native RFCOMM connect/disconnect helpers ---------------- */

    /**
     * Try native RFCOMM connect to device. Returns true if connected (nativeSocket set).
     */
    private boolean nativeConnect(BluetoothDevice device) throws IOException {
        if (device == null) return false;
        // close any existing native socket
        if (nativeSocket != null) {
            try { nativeSocket.close(); } catch (Exception ignored) {}
            nativeSocket = null;
        }

        // create socket and connect
        android.bluetooth.BluetoothSocket sock = null;
        try {
            sock = device.createRfcommSocketToServiceRecord(SPP_UUID);
            // cancel discovery for faster connection
            if (btAdapter != null) {
                try { btAdapter.cancelDiscovery(); } catch (Exception ignored) {}
            }
            sock.connect(); // blocking
            nativeSocket = sock;
            return true;
        } catch (IOException ioe) {
            // some devices require reflection fallback to createInsecureRfcommSocket or createRfcommSocket via reflection
            Log.w("PRINTER", "nativeConnect initial connect failed, trying reflection fallback: " + ioe.getMessage());
            try {
                // fallback: try createRfcommSocket via reflection (older workaround)
                java.lang.reflect.Method m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                Object fallbackSock = m.invoke(device, 1);
                if (fallbackSock instanceof android.bluetooth.BluetoothSocket) {
                    sock = (android.bluetooth.BluetoothSocket) fallbackSock;
                    sock.connect();
                    nativeSocket = sock;
                    return true;
                }
            } catch (Exception ex) {
                Log.w("PRINTER", "reflection fallback failed: " + ex.getMessage());
            }
            // ensure closed
            if (sock != null) try { sock.close(); } catch (Exception ignored) {}
            throw ioe; // rethrow original
        }
    }

    /* ---------------- utility helpers ---------------- */

    private String charsetForLang(String lang) {
        if (LANG_TSPL.equals(lang)) return DEFAULT_CHARSET_TSPL;
        if (LANG_ESC.equals(lang)) return DEFAULT_CHARSET_ESC;
        // default to UTF-8 for ZPL and others
        return DEFAULT_CHARSET_ZPL;
    }

    /**
     * Check if mac exists in allowed list (case-insensitive).
     */
    private boolean isMacAllowed(String mac) {
        if (mac == null) return false;
        String m = mac.trim().toUpperCase();
        for (String allowed : ALLOWED_MACS) {
            if (allowed != null && m.equals(allowed.trim().toUpperCase())) return true;
        }
        return false;
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
