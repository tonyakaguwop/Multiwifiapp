package com.multiwifi.connector.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import androidx.annotation.NonNull;
import com.multiwifi.connector.model.NetworkConnection;
import java.util.ArrayList;
import java.util.List;

public class NetworkUtils {
    
    private final Context context;
    private final WifiManager wifiManager;
    
    public NetworkUtils(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }
    
    /**
     * Scan for available WiFi networks
     */
    public List<ScanResult> scanForNetworks() {
        if (wifiManager != null) {
            wifiManager.startScan();
            return wifiManager.getScanResults();
        }
        return new ArrayList<>();
    }
    
    /**
     * Connect to a specified WiFi network
     */
    public boolean connectToNetwork(String ssid, String password) {
        if (wifiManager == null) return false;
        
        // For Android 10 and above, use NetworkRequest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return connectToNetworkQ(ssid, password);
        } else {
            // For older Android versions
            return connectToNetworkLegacy(ssid, password);
        }
    }
    
    /**
     * Connect to a WiFi network on Android 10+ using NetworkRequest
     */
    private boolean connectToNetworkQ(String ssid, String password) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                ConnectivityManager connectivityManager = (ConnectivityManager) 
                        context.getSystemService(Context.CONNECTIVITY_SERVICE);
                
                WifiNetworkSpecifier.Builder specifierBuilder = new WifiNetworkSpecifier.Builder();
                specifierBuilder.setSsid(ssid);
                
                if (password != null && !password.isEmpty()) {
                    specifierBuilder.setWpa2Passphrase(password);
                }
                
                NetworkRequest.Builder requestBuilder = new NetworkRequest.Builder();
                requestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                requestBuilder.setNetworkSpecifier(specifierBuilder.build());
                
                ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        // Bind to this network
                        connectivityManager.bindProcessToNetwork(network);
                    }
                };
                
                connectivityManager.requestNetwork(requestBuilder.build(), networkCallback);
                return true;
                
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
    
    /**
     * Connect to a WiFi network on Android 9 and below
     */
    private boolean connectToNetworkLegacy(String ssid, String password) {
        try {
            WifiConfiguration configuration = new WifiConfiguration();
            configuration.SSID = "\"" + ssid + "\"";
            
            if (password == null || password.isEmpty()) {
                // Open network
                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            } else {
                // WPA/WPA2 network
                configuration.preSharedKey = "\"" + password + "\"";
            }
            
            int networkId = wifiManager.addNetwork(configuration);
            
            if (networkId != -1) {
                // Disconnect from current network
                wifiManager.disconnect();
                
                // Connect to new network
                boolean enableResult = wifiManager.enableNetwork(networkId, true);
                boolean reconnectResult = wifiManager.reconnect();
                
                return enableResult && reconnectResult;
            }
            
            return false;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get information about the currently connected WiFi network
     */
    public NetworkConnection getCurrentWifiConnection() {
        if (wifiManager == null) return null;
        
        try {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            
            if (wifiInfo != null && wifiInfo.getNetworkId() != -1) {
                NetworkConnection connection = new NetworkConnection();
                
                // Set SSID (remove quotes if present)
                String ssid = wifiInfo.getSSID();
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                connection.setSsid(ssid);
                
                // Set signal strength (RSSI)
                connection.setSignalStrength(wifiInfo.getRssi());
                
                // Set link speed
                connection.setSpeed(wifiInfo.getLinkSpeed());
                
                // Set connection type to WiFi
                connection.setType(NetworkConnection.Type.WIFI);
                
                return connection;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Measure the latency to a remote server
     */
    public int measureLatency(String host) {
        try {
            Process process = Runtime.getRuntime().exec("/system/bin/ping -c 1 " + host);
            process.waitFor();
            
            // Parse ping output to extract latency
            // For a real implementation, use a non-blocking approach
            
            return 30; // Placeholder - return a default latency of 30ms
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Measure connection speed (in Mbps)
     */
    public float measureConnectionSpeed() {
        // In a real app, this would use a speed test against a remote server
        // For this demo, we'll return a simulated value based on current link speed
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                // Get link speed in Mbps
                int linkSpeed = wifiInfo.getLinkSpeed();
                
                // Actual speeds are typically lower than link speeds
                // Apply a realistic factor based on signal strength
                float signalQualityFactor = calculateSignalQualityFactor(wifiInfo.getRssi());
                return linkSpeed * signalQualityFactor;
            }
        }
        
        return 0;
    }
    
    /**
     * Calculate a factor (0.0-1.0) representing signal quality based on RSSI
     */
    private float calculateSignalQualityFactor(int rssi) {
        // RSSI usually ranges from -100 dBm (poor) to -30 dBm (excellent)
        if (rssi >= -50) return 0.9f;
        if (rssi >= -60) return 0.8f;
        if (rssi >= -70) return 0.6f;
        if (rssi >= -80) return 0.4f;
        return 0.2f;
    }
}
