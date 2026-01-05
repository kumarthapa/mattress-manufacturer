package com.galla.rfidapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.seuic.uhf.UHFService;
import com.galla.rfidapp.adapter.BluetoothDeviceAdapter;
import com.galla.rfidapp.util.PrefHelper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SettingsActivity - updated to show paired devices and scan on button press,
 * plus printer language dropdown and save-per-MAC behavior.
 */
public class SettingsActivity extends AppCompatActivity implements BluetoothDeviceAdapter.Callback {
    private static final String TAG = "SettingsActivity";

    private static final int REQ_BLUETOOTH_PERMISSIONS = 3001;
    private static final int REQ_ENABLE_BT = 3002;

    private MaterialToolbar toolbar;
    private AutoCompleteTextView acRfidPower, acPrinter; // changed to platform AutoCompleteTextView for layout compatibility
    private AutoCompleteTextView acPrinterLang;
    private MaterialButton btnScan;
    private RecyclerView rvDevices;
    private BluetoothDeviceAdapter adapter;
    private android.widget.TextView tvScanInfo;
    private MaterialButton btnApplySavedPower;
    private MaterialButton btnTestPower;

    private UHFService mDevice;
    private SharedPreferences prefs;
    private static final String PREFS = "app_prefs";

    private final String[] powerLabels = new String[]{"5 dBm", "10 dBm", "15 dBm", "20 dBm", "25 dBm", "30 dBm"};
    private final int[] powerValues = new int[]{5, 10, 15, 20, 25, 30};

    // Printer language choices (UI labels)
    private final String[] printerLangLabels = new String[]{"Auto", "ZPL", "TSPL", "ESC_POS"};
    // Normalized language keys used in prefs
    private String selectedPrinterLang = "AUTO"; // AUTO / ZPL / TSPL / ESC_POS

    private BluetoothAdapter btAdapter;
    private final ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private final Set<String> deviceAddressesSet = new HashSet<>();

    private BroadcastReceiver btReceiver;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = findViewById(R.id.toolbar_settings);
        acRfidPower = findViewById(R.id.acRfidPower);
        acPrinter = findViewById(R.id.acPrinter);
        acPrinterLang = findViewById(R.id.acPrinterLang); // from updated layout
        btnScan = findViewById(R.id.btnScan);
        rvDevices = findViewById(R.id.rvBluetoothDevices);
        tvScanInfo = findViewById(R.id.tvScanInfo);

        // Make sure these IDs exist in the layout you include for RFID card
        btnApplySavedPower = findViewById(R.id.btnApplySavedPower);
        btnTestPower = findViewById(R.id.btnTestPower);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        try {
            mDevice = UHFService.getInstance(this);   // MUST use context
            Log.d(TAG, "UHFService instance created using context");
        } catch (Exception e1) {
            Log.w(TAG, "getInstance(context) failed, trying default");
            try {
                mDevice = UHFService.getInstance();
            } catch (Exception e2) {
                mDevice = null;
                Log.w(TAG, "getInstance() also failed: " + e2.getMessage());
            }
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        setupRfidDropdown();
        setupPrinterLanguageDropdown();
        setupRecyclerView();
        setupButtonHandlers();
        loadSavedPrefs();

        // Load paired devices (will request runtime permission if needed)
        ensurePermissionsForPairedLoad();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (btAdapter != null && btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
        } catch (Exception ignored) {}
        if (btReceiver != null) {
            try { unregisterReceiver(btReceiver); } catch (Exception ignored) {}
            btReceiver = null;
        }
        executor.shutdownNow();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* ---------- RFID dropdown ---------- */
    private void setupRfidDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                powerLabels
        );
        acRfidPower.setAdapter(adapter);
        acRfidPower.setThreshold(0);

        // When user selects a power level from dropdown
        acRfidPower.setOnItemClickListener((parent, view, position, id) -> {
            int power = powerValues[position];
            PrefHelper.saveRfidPower(this, power);
            applyRfidPower(power);
            Toast.makeText(this, "RFID Power set to " + power + " dBm", Toast.LENGTH_SHORT).show();
        });

        // Load saved RFID power or fallback to default (5 dBm)
        int savedPower = PrefHelper.getRfidPower(this);
        if (savedPower <= 0) {
            savedPower = 5;
            PrefHelper.saveRfidPower(this, savedPower);
        }

        // Match saved power to label and set text
        String selectedLabel = "5 dBm";
        for (int i = 0; i < powerValues.length; i++) {
            if (powerValues[i] == savedPower) {
                selectedLabel = powerLabels[i];
                break;
            }
        }
        acRfidPower.setText(selectedLabel, false);

        // Show dropdown when tapped
        acRfidPower.setOnClickListener(v -> acRfidPower.showDropDown());
    }

    /* ---------- Printer language dropdown ---------- */
    private void setupPrinterLanguageDropdown() {
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                printerLangLabels
        );
        acPrinterLang.setAdapter(langAdapter);
        acPrinterLang.setThreshold(0);
        acPrinterLang.setOnClickListener(v -> acPrinterLang.showDropDown());

        acPrinterLang.setOnItemClickListener((parent, view, position, id) -> {
            String label = printerLangLabels[position];
            switch (label) {
                case "ZPL": selectedPrinterLang = "ZPL"; break;
                case "TSPL": selectedPrinterLang = "TSPL"; break;
                case "ESC_POS": selectedPrinterLang = "ESC_POS"; break;
                default: selectedPrinterLang = "AUTO"; break;
            }
            // Save as global default
            PrefHelper.savePrinterLanguageGlobal(this, selectedPrinterLang);
            Toast.makeText(this, "Printer language set: " + label, Toast.LENGTH_SHORT).show();
        });

        // Load saved language (per-MAC if present, otherwise global)
        String savedMac = PrefHelper.getPrinterMac(this);
        String langToShow = savedMac != null ? PrefHelper.getPrinterLanguageForMac(this, savedMac)
                : PrefHelper.getPrinterLanguageGlobal(this);
        if (langToShow == null) langToShow = "AUTO";

        if ("ZPL".equals(langToShow)) acPrinterLang.setText("ZPL", false);
        else if ("TSPL".equals(langToShow)) acPrinterLang.setText("TSPL", false);
        else if ("ESC_POS".equals(langToShow)) acPrinterLang.setText("ESC_POS", false);
        else acPrinterLang.setText("Auto", false);

        selectedPrinterLang = langToShow;
    }

    /**
     * Ensure we have a valid, opened UHFService instance.
     * Tries getInstance(Context) and open() if necessary.
     * Returns true if device ready for calls.
     */
    private boolean ensureUhfOpen() {
        try {
            if (mDevice == null) {
                // try getInstance with context if available
                try {
                    Method gi = Class.forName("com.seuic.uhf.UHFService")
                            .getMethod("getInstance", Context.class);
                    Object inst = gi.invoke(null, this);
                    if (inst instanceof UHFService) mDevice = (UHFService) inst;
                } catch (NoSuchMethodException nm) {
                    // fallback to no-arg getInstance()
                    try {
                        Method gi2 = Class.forName("com.seuic.uhf.UHFService").getMethod("getInstance");
                        Object inst = gi2.invoke(null);
                        if (inst instanceof UHFService) mDevice = (UHFService) inst;
                    } catch (Exception ignored) {}
                } catch (Exception e) {
                    Log.w(TAG, "ensureUhfOpen getInstance failed: " + e.getMessage());
                }
            }

            if (mDevice == null) {
                Log.w(TAG, "ensureUhfOpen: UHFService instance still null");
                return false;
            }

            // Check open state via isOpen() or isopen()
            boolean opened = false;
            try {
                Method isOpen = mDevice.getClass().getMethod("isOpen");
                Object res = isOpen.invoke(mDevice);
                opened = res instanceof Boolean && (Boolean) res;
            } catch (NoSuchMethodException ex) {
                try {
                    Method isopen = mDevice.getClass().getMethod("isopen");
                    Object res = isopen.invoke(mDevice);
                    opened = res instanceof Boolean && (Boolean) res;
                } catch (Exception ignored) {}
            } catch (Exception e) {
                Log.w(TAG, "isOpen check failed: " + e.getMessage());
            }

            if (!opened) {
                // attempt open()
                try {
                    Method open = mDevice.getClass().getMethod("open");
                    Object result = open.invoke(mDevice);
                    if (result instanceof Boolean) opened = (Boolean) result;
                    Log.d(TAG, "UHFService.open() returned: " + result);
                } catch (NoSuchMethodException nm) {
                    Log.w(TAG, "ensureUhfOpen: open() not found on UHFService");
                }
            }

            Log.d(TAG, "ensureUhfOpen: opened=" + opened);
            return opened;
        } catch (Exception e) {
            Log.e(TAG, "ensureUhfOpen exception: " + e.getMessage());
            return false;
        }
    }


    /**
     * Apply power robustly: ensure opened, call setPower(int) or fallback to setParameters.
     */
    private void applyRfidPower(int power) {
        Log.d(TAG, "applyRfidPower request: " + power + " dBm");
        if (!ensureUhfOpen()) {
            Toast.makeText(this, "UHF not available / failed to open", Toast.LENGTH_LONG).show();
            return;
        }

        boolean applied = false;
        String methodUsed = null;

        // 1) Try direct setPower(int) if present (preferred)
        try {
            Method m = mDevice.getClass().getMethod("setPower", int.class);
            m.setAccessible(true);
            Object r = m.invoke(mDevice, power);
            if (r instanceof Boolean) applied = (Boolean) r;
            methodUsed = "setPower";
            Log.d(TAG, "invoke setPower returned: " + r);
        } catch (NoSuchMethodException ns) {
            Log.d(TAG, "setPower method not present: " + ns.getMessage());
        } catch (Exception e) {
            Log.w(TAG, "setPower invocation failed: " + e.getMessage());
        }

        // 2) Try other named methods via reflection (if setPower not present/succeeded)
        if (!applied) {
            String[] names = new String[]{"setOutputPower", "setRfPower", "setTxPower", "setPowerDbm"};
            for (String name : names) {
                try {
                    Method m = mDevice.getClass().getMethod(name, int.class);
                    m.setAccessible(true);
                    Object r = m.invoke(mDevice, power);
                    if (r instanceof Boolean) applied = (Boolean) r;
                    methodUsed = name;
                    Log.d(TAG, "invoke " + name + " returned: " + r);
                    if (applied) break;
                } catch (NoSuchMethodException ns) {
                    // ignore
                } catch (Exception e) {
                    Log.w(TAG, "invoke " + name + " failed: " + e.getMessage());
                }
            }
        }

        // 3) Fallback to setParameters(paramId, value)
        if (!applied) {
            try {
                Method sp = mDevice.getClass().getMethod("setParameters", int.class, int.class);
                sp.setAccessible(true);

                // Try parameter ids likely to be used by this SDK. The decompiled class didn't
                // expose a PARAMETER_POWER constant, so we try a best-effort list.
                int[] paramIdsToTry = new int[]{0, 3, 4, 18, 19}; // heuristic
                for (int pid : paramIdsToTry) {
                    try {
                        Object r = sp.invoke(mDevice, pid, power);
                        boolean ok = false;
                        if (r instanceof Boolean) ok = (Boolean) r;
                        Log.d(TAG, "setParameters(" + pid + "," + power + ") returned: " + r);
                        if (ok) {
                            applied = true;
                            methodUsed = "setParameters(" + pid + ")";
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            } catch (NoSuchMethodException nm) {
                Log.d(TAG, "setParameters not present");
            } catch (Exception e) {
                Log.w(TAG, "setParameters invocation failed: " + e.getMessage());
            }
        }

        // Read-back attempt
        Integer currentPower = null;
        try {
            Method gp = mDevice.getClass().getMethod("getPower");
            gp.setAccessible(true);
            Object res = gp.invoke(mDevice);
            if (res instanceof Integer) currentPower = (Integer) res;
            else if (res != null) currentPower = Integer.parseInt(String.valueOf(res));
            Log.d(TAG, "read-back getPower() => " + res);
        } catch (NoSuchMethodException ns) {
            Log.d(TAG, "getPower not present for read-back");
        } catch (Exception e) {
            Log.w(TAG, "getPower read-back failed: " + e.getMessage());
        }

        // Show result
        if (applied) {
            String msg = "Requested " + power + " dBm via " + (methodUsed == null ? "unknown" : methodUsed);
            if (currentPower != null) msg += " | current " + currentPower + " dBm";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            Log.i(TAG, "applyRfidPower succeeded: " + msg);
        } else {
            String msg = "Failed to set RFID power. methodUsed=" + methodUsed;
            if (currentPower != null) msg += " | current " + currentPower + " dBm";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            Log.e(TAG, msg);
        }
    }

    /* ---------- RecyclerView & adapter ---------- */
    private void setupRecyclerView() {
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BluetoothDeviceAdapter(this);
        rvDevices.setAdapter(adapter);
        adapter.setItems(deviceList); // initially empty

        String savedMac = PrefHelper.getPrinterMac(this);
        if (savedMac != null) adapter.setSelectedMac(savedMac);
    }

    private void setupButtonHandlers() {
        btnScan.setOnClickListener(v -> {
            if (btAdapter == null) {
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
                return;
            }
            ensurePermissionsThenScan();
        });

        btnApplySavedPower.setOnClickListener(v -> {
            int savedPower = PrefHelper.getRfidPower(SettingsActivity.this);
            if (savedPower <= 0) savedPower = 30;
            applyRfidPower(savedPower);
        });

        // <-- call existing method name showCurrentPower()
        btnTestPower.setOnClickListener(v -> showCurrentPower());

        acPrinter.setOnClickListener(v -> {
            String savedMac = PrefHelper.getPrinterMac(this);
            if (savedMac != null) acPrinter.setText(savedMac, false);
            else acPrinter.setText("", false);
        });
    }

    /* ---------- Permissions helpers ---------- */
    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ArrayList<String> needed = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (!needed.isEmpty()) {
                ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), REQ_BLUETOOTH_PERMISSIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_BLUETOOTH_PERMISSIONS);
            }
        }
    }

    private void ensurePermissionsForPairedLoad() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
        } else {
            loadPairedDevices();
        }
    }

    private void ensurePermissionsThenScan() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }
        if (!isBluetoothEnabled()) {
            Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBt, REQ_ENABLE_BT);
            return;
        }
        startScan();
    }

    private boolean isBluetoothEnabled() {
        try {
            return btAdapter != null && btAdapter.isEnabled();
        } catch (SecurityException se) {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_ENABLE_BT) {
            if (isBluetoothEnabled()) {
                startScan();
            } else {
                Toast.makeText(this, "Bluetooth is required for scanning", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // permission callback
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQ_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            if (grantResults.length == 0) allGranted = false;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                loadPairedDevices();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required to list devices", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressLint("MissingPermission")
    private void loadPairedDevices() {
        tvScanInfo.setText("Status: Loading paired devices...");
        deviceList.clear();
        deviceAddressesSet.clear();

        executor.execute(() -> {
            if (!hasBluetoothPermissions()) {
                mainHandler.post(() -> tvScanInfo.setText("Status: Permission required to load paired devices"));
                return;
            }
            try {
                Set<BluetoothDevice> paired = btAdapter != null ? btAdapter.getBondedDevices() : null;
                if (paired != null && !paired.isEmpty()) {
                    for (BluetoothDevice d : paired) {
                        if (d == null) continue;
                        String addr = d.getAddress();
                        if (addr == null) continue;
                        if (deviceAddressesSet.add(addr)) {
                            deviceList.add(d);
                        }
                    }
                }
            } catch (SecurityException se) {
                Log.w(TAG, "loadPairedDevices: permission missing: " + se.getMessage());
            }
            mainHandler.post(() -> {
                adapter.setItems(new ArrayList<>(deviceList));
                tvScanInfo.setText("Status: Found " + deviceList.size() + " paired device(s)");
            });
        });
    }

    @SuppressLint("MissingPermission")
    private void startScan() {
        if (!hasBluetoothPermissions()) {
            Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show();
            requestBluetoothPermissions();
            return;
        }
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        tvScanInfo.setText("Status: Scanning...");
        if (btReceiver == null) registerDiscoveryReceiver();
        try { if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery(); } catch (Exception ignored) {}
        boolean started = false;
        try { started = btAdapter.startDiscovery(); } catch (SecurityException se) { Log.w(TAG, "startDiscovery failed: " + se.getMessage()); }
        if (!started) {
            tvScanInfo.setText("Status: Discovery not started");
            Toast.makeText(this, "Failed to start discovery or discovery already running", Toast.LENGTH_SHORT).show();
        } else {
            tvScanInfo.setText("Status: Scanning nearby devices...");
        }
    }

    private void registerDiscoveryReceiver() {
        if (btReceiver != null) return;
        btReceiver = new BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device == null) return;
                    String addr = device.getAddress();
                    if (addr == null) return;
                    if (deviceAddressesSet.contains(addr)) return;
                    deviceAddressesSet.add(addr);
                    deviceList.add(device);
                    mainHandler.post(() -> {
                        adapter.updateDevice(device);
                        tvScanInfo.setText("Status: Found " + deviceList.size() + " device(s)");
                    });
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    mainHandler.post(() -> {
                        tvScanInfo.setText("Status: Scan finished. " + deviceList.size() + " device(s) listed");
                        Toast.makeText(SettingsActivity.this, "Scan finished", Toast.LENGTH_SHORT).show();
                    });
                } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                    if (dev != null) {
                        mainHandler.post(() -> {
                            adapter.updateDevice(dev);
                            if (state == BluetoothDevice.BOND_BONDED) {
                                Toast.makeText(SettingsActivity.this, "Paired: " + dev.getAddress(), Toast.LENGTH_SHORT).show();
                            } else if (state == BluetoothDevice.BOND_NONE) {
                                Toast.makeText(SettingsActivity.this, "Pairing failed or removed: " + dev.getAddress(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        };

        IntentFilter f = new IntentFilter();
        f.addAction(BluetoothDevice.ACTION_FOUND);
        f.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        f.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        try { registerReceiver(btReceiver, f); } catch (Exception e) { Log.w(TAG, "registerReceiver failed: " + e.getMessage()); }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onPairRequested(BluetoothDevice device) {
        if (device == null) return;
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            Toast.makeText(this, "Bluetooth permission required to pair", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            int state = device.getBondState();
            if (state == BluetoothDevice.BOND_BONDED) {
                Toast.makeText(this, "Already paired", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean started = false;
            try { started = device.createBond(); } catch (Exception e) {
                try { Method m = device.getClass().getMethod("createBond"); started = (Boolean) m.invoke(device); }
                catch (Exception ex) { Log.w(TAG, "createBond reflection failed: " + ex.getMessage()); }
            }
            if (!started) Toast.makeText(this, "Failed to start pairing", Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "Pairing started...", Toast.LENGTH_SHORT).show();
        } catch (SecurityException se) {
            Toast.makeText(this, "Missing permission to pair", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectRequested(BluetoothDevice device) {
        if (device == null) return;
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            Toast.makeText(this, "Device not paired. Please pair first.", Toast.LENGTH_SHORT).show();
            return;
        }
        connectToDevice(device);
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(final BluetoothDevice device) {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            Toast.makeText(this, "Bluetooth permission required to connect", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Connecting to " + (device.getName() == null ? device.getAddress() : device.getName()), Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            BluetoothSocket socket = null;
            try {
                UUID spp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                socket = device.createRfcommSocketToServiceRecord(spp);
                try { if (btAdapter != null && btAdapter.isDiscovering()) btAdapter.cancelDiscovery(); } catch (Exception ignored) {}

                // Connect and immediately close test socket to verify connectivity (we will save MAC & prefs)
                socket.connect();
                try {
                    socket.close();
                } catch (IOException ignoreClose) {}

                mainHandler.post(() -> {
                    // Save MAC
                    PrefHelper.savePrinterMac(SettingsActivity.this, device.getAddress());
                    String label = (device.getName() == null ? "Unknown" : device.getName()) + " (" + device.getAddress() + ")";
                    acPrinter.setText(label, false);
                    Toast.makeText(SettingsActivity.this, "Connected to " + label, Toast.LENGTH_LONG).show();
                    if (adapter != null) adapter.setSelectedMac(device.getAddress());

                    // Decide language to save for this MAC
                    String langToSave = selectedPrinterLang;
                    if ("AUTO".equals(langToSave) || langToSave == null) {
                        langToSave = detectPrinterLanguageFromName(device.getName());
                    }
                    if (langToSave == null) langToSave = PrefHelper.getPrinterLanguageGlobal(SettingsActivity.this);
                    if (langToSave == null) langToSave = "AUTO";

                    PrefHelper.savePrinterLanguageForMac(SettingsActivity.this, device.getAddress(), langToSave);

                    // Update UI language dropdown to reflect saved value
                    if ("ZPL".equals(langToSave)) acPrinterLang.setText("ZPL", false);
                    else if ("TSPL".equals(langToSave)) acPrinterLang.setText("TSPL", false);
                    else if ("ESC_POS".equals(langToSave)) acPrinterLang.setText("ESC_POS", false);
                    else acPrinterLang.setText("Auto", false);

                    selectedPrinterLang = langToSave;
                });
            } catch (IOException ioe) {
                Log.e(TAG, "Connect failed: " + ioe.getMessage(), ioe);
                mainHandler.post(() -> Toast.makeText(SettingsActivity.this, "Connect failed: " + ioe.getMessage(), Toast.LENGTH_LONG).show());
                try { if (socket != null) socket.close(); } catch (IOException ignored) {}
            } catch (SecurityException se) {
                Log.e(TAG, "Missing permission: " + se.getMessage(), se);
                mainHandler.post(() -> Toast.makeText(SettingsActivity.this, "Missing permission to connect", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void loadSavedPrefs() {
        int savedPower = PrefHelper.getRfidPower(this);
        if (savedPower != -1) {
            for (int i = 0; i < powerValues.length; i++) {
                if (powerValues[i] == savedPower) {
                    acRfidPower.setText(powerLabels[i], false);
                    break;
                }
            }
        }
        String savedMac = PrefHelper.getPrinterMac(this);
        if (savedMac != null) {
            // show as name (if discovered) or mac
            String label = savedMac;
            for (BluetoothDevice d : deviceList) {
                if (d != null && savedMac.equalsIgnoreCase(d.getAddress())) {
                    String n = d.getName();
                    if (n != null) label = n + " (" + savedMac + ")";
                    break;
                }
            }
            acPrinter.setText(label, false);
            if (adapter != null) adapter.setSelectedMac(savedMac);

            // load per-mac language or global fallback
            String langForMac = PrefHelper.getPrinterLanguageForMac(this, savedMac);
            if (langForMac == null) langForMac = PrefHelper.getPrinterLanguageGlobal(this);
            if (langForMac == null) langForMac = "AUTO";

            if ("ZPL".equals(langForMac)) acPrinterLang.setText("ZPL", false);
            else if ("TSPL".equals(langForMac)) acPrinterLang.setText("TSPL", false);
            else if ("ESC_POS".equals(langForMac)) acPrinterLang.setText("ESC_POS", false);
            else acPrinterLang.setText("Auto", false);

            selectedPrinterLang = langForMac;
        } else {
            // show global default
            String g = PrefHelper.getPrinterLanguageGlobal(this);
            if (g == null) g = "AUTO";
            if ("ZPL".equals(g)) acPrinterLang.setText("ZPL", false);
            else if ("TSPL".equals(g)) acPrinterLang.setText("TSPL", false);
            else if ("ESC_POS".equals(g)) acPrinterLang.setText("ESC_POS", false);
            else acPrinterLang.setText("Auto", false);
            selectedPrinterLang = g;
        }
    }

    /**
     * Read and show current power with clearer logs.
     */
    private void showCurrentPower() {
        Log.d(TAG, "showCurrentPower called");
        if (!ensureUhfOpen()) {
            Toast.makeText(this, "UHF not available / failed to open", Toast.LENGTH_LONG).show();
            return;
        }
        PrefHelper.PowerResult r = PrefHelper.readCurrentRfidPower(mDevice);
        if (r != null && r.power != null) {
            String msg = "Current RFID power: " + r.power + " dBm (via " + r.method + ")";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            Log.i(TAG, msg);
        } else {
            Toast.makeText(this, "Unable to read current RFID power (SDK may not expose readable API)", Toast.LENGTH_LONG).show();
            Log.w(TAG, "showCurrentPower: readCurrentRfidPower returned null");
        }
    }

    /**
     * Try to detect likely language from bluetooth device name.
     * Returns "ZPL", "TSPL", "ESC_POS" or "AUTO".
     */
    private String detectPrinterLanguageFromName(String name) {
        if (name == null) return "AUTO";
        String n = name.toUpperCase();
        if (n.contains("ZEBRA") || n.contains("ZJ") || n.contains("ZCS") || n.contains("ZD") || n.contains("QL")) return "ZPL";
        if (n.contains("DC") || n.contains("D-CODE") || n.contains("DC3M") || n.contains("D3M") ||
                n.contains("TSC") || n.contains("TSPL") || n.contains("LP") || n.contains("XP") ) return "TSPL";
        if (n.contains("ESC") || n.contains("EPSON") || n.contains("POS") || n.contains("RONGTA") || n.contains("RX")) return "ESC_POS";
        return "AUTO";
    }
}
