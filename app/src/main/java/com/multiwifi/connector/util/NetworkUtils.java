package com.multiwifi.connector.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.PatternMatcher;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.NetworkConnection;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Utility class for network operations
 */
public class NetworkUtils {
    private static final String TAG = "NetworkUtils";
    private static final String TEST_URL = "https://www.google.com";
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    
    /**
     * Scans for available WiFi networks
     * 
     * @param context The application context
     * @return List of available networks
     */
    public static List<NetworkConnection> scanNetworks(Context context) {
        List<NetworkConnection> networks = new ArrayList<>();
        
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Log.e(TAG, "WifiManager is null");
            return networks;
        }
        
        // Ensure WiFi is enabled
        if (!wifiManager.isWifiEnabled()) {
            Log.w(TAG, "WiFi is disabled");
            wifiManager.setWifiEnabled(true);
        }
        
        // Start scan
        boolean scanSuccess = wifiManager.startScan();
        if (!scanSuccess) {
            Log.w(TAG, "WiFi scan failed. Using cached results if available.");
        }
        
        // Get scan results
        List<ScanResult> scanResults = wifiManager.getScanResults();
        for (ScanResult result : scanResults) {
            if (result.SSID != null && !result.SSID.isEmpty()) {
                NetworkConnection network = new NetworkConnection(
                        result.SSID,
                        result.BSSID,
                        result.level
                );
                networks.add(network);
                Log.d(TAG, "Found network: " + result.SSID);
            }
        }
        
        return networks;
    }
    
    /**
     * Connects to a WiFi network
     * 
     * @param context The application context
     * @param ssid The SSID of the network to connect to
     * @param password The password for the network (null for open networks)
     * @param method The connection method to use
     * @return true if connection was initiated successfully, false otherwise
     */
    public static boolean connectToNetwork(Context context, String ssid, String password, ConnectionMethod method) {
        switch (method) {
            case NATIVE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    return connectNetworkModern(context, ssid, password);
                } else {
                    return connectNetworkLegacy(context, ssid, password);
                }
            case USB_ADAPTER:
                // Implementation would depend on the specific USB adapter
                Log.w(TAG, "USB adapter connection not implemented");
                return false;
            case HYBRID:
                // For hybrid, we connect to WiFi and ensure cellular is also available
                return connectNetworkLegacy(context, ssid, password);
            case PROXY:
            default:
                // For proxy, we just use whatever connection is available
                return true;
        }
    }
    
    /**
     * Legacy method to connect to a WiFi network (pre-Android 10)
     * 
     * @param context The application context
     * @param ssid The SSID of the network to connect to
     * @param password The password for the network (null for open networks)
     * @return true if connection was initiated successfully, false otherwise
     */
    private static boolean connectNetworkLegacy(Context context, String ssid, String password) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Log.e(TAG, "WifiManager is null");
            return false;
        }
        
        // Setup WiFi configuration
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";
        
        if (password == null) {
            // Open network
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else {
            // Secured network (WPA/WPA2)
            config.preSharedKey = "\"" + password + "\"";
        }
        
        // Add network
        int networkId = wifiManager.addNetwork(config);
        if (networkId == -1) {
            Log.e(TAG, "Failed to add network configuration");
            return false;
        }
        
        // Connect to network
        boolean enableSuccess = wifiManager.enableNetwork(networkId, true);
        boolean reconnectSuccess = wifiManager.reconnect();
        
        return enableSuccess && reconnectSuccess;
    }
    
    /**
     * Modern method to connect to a WiFi network (Android 10+)
     * 
     * @param context The application context
     * @param ssid The SSID of the network to connect to
     * @param password The password for the network (null for open networks)
     * @return true if connection was initiated successfully, false otherwise
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static boolean connectNetworkModern(Context context, String ssid, String password) {
        ConnectivityManager connectivityManager = (ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager is null");
            return false;
        }
        
        // Create network specifier
        WifiNetworkSpecifier.Builder specifierBuilder = new WifiNetworkSpecifier.Builder();
        specifierBuilder.setSsidPattern(new PatternMatcher(ssid, PatternMatcher.PATTERN_LITERAL));
        
        if (password != null) {
            specifierBuilder.setWpa2Passphrase(password);
        }
        
        WifiNetworkSpecifier wifiNetworkSpecifier = specifierBuilder.build();
        
        // Create network request
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build();
        
        // Request network connection
        connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.d(TAG, "Network connection is available");
                
                // Bind process to the network
                connectivityManager.bindProcessToNetwork(network);
            }
            
            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.e(TAG, "Network connection is unavailable");
            }
        });
        
        return true;
    }
    
    /**
     * Tests the speed of the current connection
     * 
     * @param callback Callback to receive the speed test result
     */
    public static void testConnectionSpeed(final SpeedTestCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            long startTime = System.currentTimeMillis();
            long fileSize = 0;
            
            try {
                URL url = new URL(TEST_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(CONNECTION_TIMEOUT);
                connection.connect();
                
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    
                    while ((bytesRead = connection.getInputStream().read(buffer)) != -1) {
                        fileSize += bytesRead;
                    }
                    
                    long endTime = System.currentTimeMillis();
                    double duration = (endTime - startTime) / 1000.0; // seconds
                    double speed = (fileSize * 8.0) / (duration * 1000000.0); // Mbps
                    
                    // Callback with result
                    if (callback != null) {
                        callback.onSpeedTested(speed);
                    }
                }
                
                connection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "Error testing connection speed", e);
                if (callback != null) {
                    callback.onSpeedTestFailed(e.getMessage());
                }
            }
        });
    }
    
    /**
     * Interface for speed test callbacks
     */
    public interface SpeedTestCallback {
        void onSpeedTested(double speedMbps);
        void onSpeedTestFailed(String errorMessage);
    }
}
