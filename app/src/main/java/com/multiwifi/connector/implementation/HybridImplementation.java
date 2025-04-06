package com.multiwifi.connector.implementation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.telephony.TelephonyManager;
import androidx.annotation.NonNull;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.util.LoadBalancer;
import com.multiwifi.connector.util.NetworkUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation using WiFi + Cellular data hybridization
 */
public class HybridImplementation implements MultiWifiImplementation {
    
    private final Context context;
    private final NetworkUtils networkUtils;
    private final LoadBalancer loadBalancer;
    private final ConnectivityManager connectivityManager;
    private final TelephonyManager telephonyManager;
    private boolean isInitialized;
    private boolean isConnected;
    private NetworkConnection wifiNetwork;
    private NetworkConnection cellularNetwork;
    private ConnectivityManager.NetworkCallback wifiCallback;
    private ConnectivityManager.NetworkCallback cellularCallback;
    
    public HybridImplementation(Context context) {
        this.context = context;
        this.networkUtils = new NetworkUtils(context);
        this.loadBalancer = new LoadBalancer();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        this.isInitialized = false;
        this.isConnected = false;
    }
    
    @Override
    public boolean initialize() {
        // Check if connectivity manager is available
        if (connectivityManager == null || telephonyManager == null) {
            return false;
        }
        
        // Check if device has cellular data capability
        if (!hasCellularCapability()) {
            return false;
        }
        
        isInitialized = true;
        return true;
    }
    
    @Override
    public boolean connect() {
        if (!isInitialized) {
            return false;
        }
        
        // Connect to WiFi if not already connected
        wifiNetwork = networkUtils.getCurrentWifiConnection();
        if (wifiNetwork == null) {
            return false;
        }
        
        // Ensure WiFi network remains available
        requestWifiNetwork();
        
        // Connect to cellular data
        connectToCellularData();
        
        isConnected = (wifiNetwork != null && cellularNetwork != null);
        return isConnected;
    }
    
    @Override
    public boolean disconnect() {
        if (!isInitialized) {
            return false;
        }
        
        // Unregister network callbacks
        if (wifiCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(wifiCallback);
                wifiCallback = null;
            } catch (Exception e) {
                // Ignore exceptions when unregistering
            }
        }
        
        if (cellularCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(cellularCallback);
                cellularCallback = null;
            } catch (Exception e) {
                // Ignore exceptions when unregistering
            }
        }
        
        wifiNetwork = null;
        cellularNetwork = null;
        isConnected = false;
        
        return true;
    }
    
    @Override
    public boolean isConnected() {
        return isConnected;
    }
    
    @Override
    public List<NetworkConnection> scanNetworks() {
        List<NetworkConnection> networks = new ArrayList<>();
        
        if (!isInitialized) {
            return networks;
        }
        
        // Add WiFi networks
        networkUtils.scanForNetworks().forEach(scanResult -> {
            NetworkConnection network = new NetworkConnection();
            network.setSsid(scanResult.SSID);
            network.setType(NetworkConnection.Type.WIFI);
            network.setSignalStrength(scanResult.level);
            
            // Check if this is the current WiFi network
            if (wifiNetwork != null && wifiNetwork.getSsid().equals(scanResult.SSID)) {
                network.setActive(true);
                network.setSpeed(wifiNetwork.getSpeed());
                network.setLatency(wifiNetwork.getLatency());
            } else {
                network.setActive(false);
                
                // Set estimated values
                network.setSpeed(estimateSpeedFromSignal(scanResult.level));
                network.setLatency(estimateLatencyFromSignal(scanResult.level));
            }
            
            networks.add(network);
        });
        
        // Add cellular network if available
        if (hasCellularCapability()) {
            NetworkConnection cellular = new NetworkConnection();
            cellular.setSsid("Cellular Data");
            cellular.setType(NetworkConnection.Type.CELLULAR);
            
            // Get cellular signal strength (simplified)
            int signalStrength = getCellularSignalStrength();
            cellular.setSignalStrength(signalStrength);
            
            // Set cellular network speed and latency (estimated)
            float cellularSpeed = estimateCellularSpeed();
            int cellularLatency = estimateCellularLatency();
            cellular.setSpeed(cellularSpeed);
            cellular.setLatency(cellularLatency);
            
            // Check if cellular is active
            cellular.setActive(cellularNetwork != null);
            
            networks.add(cellular);
        }
        
        return networks;
    }
    
    @Override
    public List<NetworkConnection> getConnectedNetworks() {
        List<NetworkConnection> networks = new ArrayList<>();
        
        if (!isInitialized || !isConnected) {
            return networks;
        }
        
        // Add WiFi network if connected
        if (wifiNetwork != null) {
            networks.add(wifiNetwork);
        }
        
        // Add cellular network if connected
        if (cellularNetwork != null) {
            networks.add(cellularNetwork);
        }
        
        return networks;
    }
    
    @Override
    public boolean addNetwork(String ssid, String password) {
        if (!isInitialized) {
            return false;
        }
        
        // If it's "Cellular Data", try to connect to cellular
        if (ssid.equals("Cellular Data")) {
            if (cellularNetwork == null) {
                connectToCellularData();
                return cellularNetwork != null;
            }
            return true;
        }
        
        // Otherwise, try to connect to WiFi
        boolean wifiConnected = networkUtils.connectToNetwork(ssid, password);
        if (wifiConnected) {
            wifiNetwork = networkUtils.getCurrentWifiConnection();
            
            // Request WiFi network
            requestWifiNetwork();
            
            isConnected = (wifiNetwork != null && cellularNetwork != null);
            return wifiNetwork != null;
        }
        
        return false;
    }
    
    @Override
    public boolean removeNetwork(String ssid) {
        if (!isInitialized) {
            return false;
        }
        
        // If it's cellular, disconnect cellular
        if (ssid.equals("Cellular Data")) {
            if (cellularCallback != null) {
                try {
                    connectivityManager.unregisterNetworkCallback(cellularCallback);
                    cellularCallback = null;
                    cellularNetwork = null;
                    isConnected = wifiNetwork != null;
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        }
        
        // If it's WiFi, disconnect WiFi
        if (wifiNetwork != null && wifiNetwork.getSsid().equals(ssid)) {
            if (wifiCallback != null) {
                try {
                    connectivityManager.unregisterNetworkCallback(wifiCallback);
                    wifiCallback = null;
                    wifiNetwork = null;
                    isConnected = cellularNetwork != null;
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public float getCombinedSpeed() {
        if (!isInitialized || !isConnected) {
            return 0;
        }
        
        float totalSpeed = 0;
        
        // Add WiFi speed
        if (wifiNetwork != null) {
            totalSpeed += wifiNetwork.getSpeed();
        }
        
        // Add cellular speed
        if (cellularNetwork != null) {
            totalSpeed += cellularNetwork.getSpeed();
        }
        
        return totalSpeed;
    }
    
    @Override
    public void updateMetrics() {
        if (!isInitialized || !isConnected) {
            return;
        }
        
        // Update WiFi metrics
        if (wifiNetwork != null) {
            float wifiSpeed = networkUtils.measureConnectionSpeed();
            int wifiLatency = networkUtils.measureLatency("google.com");
            wifiNetwork.setSpeed(wifiSpeed);
            wifiNetwork.setLatency(wifiLatency);
        }
        
        // Update cellular metrics
        if (cellularNetwork != null) {
            float cellularSpeed = estimateCellularSpeed();
            int cellularLatency = estimateCellularLatency();
            cellularNetwork.setSpeed(cellularSpeed);
            cellularNetwork.setLatency(cellularLatency);
        }
        
        // Update load allocation
        List<NetworkConnection> networks = getConnectedNetworks();
        loadBalancer.distributeLoad(networks, 1000000); // 1MB test load
    }
    
    @Override
    public void cleanup() {
        disconnect();
        isInitialized = false;
    }
    
    /**
     * Check if device has cellular data capability
     */
    private boolean hasCellularCapability() {
        if (telephonyManager == null) {
            return false;
        }
        
        // Check if device has a SIM card
        return telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
    }
    
    /**
     * Request WiFi network to bind to
     */
    private void requestWifiNetwork() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            
            wifiCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    // WiFi is available
                    wifiNetwork = networkUtils.getCurrentWifiConnection();
                    isConnected = (wifiNetwork != null && cellularNetwork != null);
                }
                
                @Override
                public void onLost(@NonNull Network network) {
                    // WiFi is lost
                    wifiNetwork = null;
                    isConnected = cellularNetwork != null;
                }
            };
            
            connectivityManager.requestNetwork(builder.build(), wifiCallback);
        }
    }
    
    /**
     * Connect to cellular data
     */
    private void connectToCellularData() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
            
            cellularCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    // Cellular data is available
                    cellularNetwork = new NetworkConnection();
                    cellularNetwork.setSsid("Cellular Data");
                    cellularNetwork.setType(NetworkConnection.Type.CELLULAR);
                    cellularNetwork.setActive(true);
                    
                    // Set cellular network metrics
                    int signalStrength = getCellularSignalStrength();
                    cellularNetwork.setSignalStrength(signalStrength);
                    
                    float cellularSpeed = estimateCellularSpeed();
                    int cellularLatency = estimateCellularLatency();
                    cellularNetwork.setSpeed(cellularSpeed);
                    cellularNetwork.setLatency(cellularLatency);
                    
                    isConnected = (wifiNetwork != null && cellularNetwork != null);
                }
                
                @Override
                public void onLost(@NonNull Network network) {
                    // Cellular data is lost
                    cellularNetwork = null;
                    isConnected = wifiNetwork != null;
                }
            };
            
            connectivityManager.requestNetwork(builder.build(), cellularCallback);
        }
    }
    
    /**
     * Get cellular signal strength
     */
    private int getCellularSignalStrength() {
        // In a real app, would use PhoneStateListener to get actual signal strength
        // For this demo, return a simulated value
        return -80; // Moderate signal strength
    }
    
    /**
     * Estimate cellular data speed based on network type and signal strength
     */
    private float estimateCellularSpeed() {
        if (telephonyManager == null) {
            return 0;
        }
        
        // Simplified estimation based on network type
        int networkType = telephonyManager.getNetworkType();
        
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_LTE:
                return 20.0f; // 4G LTE
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return 10.0f; // 3G+
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return 5.0f; // 3G
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return 0.5f; // 2G
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return 0.2f; // 2G
            default:
                // Check for 5G on Android 10+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (networkType == TelephonyManager.NETWORK_TYPE_NR) {
                        return 50.0f; // 5G
                    }
                }
                return 2.0f; // Unknown
        }
    }
    
    /**
     * Estimate cellular data latency based on network type
     */
    private int estimateCellularLatency() {
        if (telephonyManager == null) {
            return 100;
        }
        
        // Simplified estimation based on network type
        int networkType = telephonyManager.getNetworkType();
        
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_LTE:
                return 50; // 4G LTE
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return 80; // 3G+
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return 100; // 3G
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return 200; // 2G
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return 300; // 2G
            default:
                // Check for 5G on Android 10+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (networkType == TelephonyManager.NETWORK_TYPE_NR) {
                        return 30; // 5G
                    }
                }
                return 150; // Unknown
        }
    }
    
    /**
     * Estimate WiFi speed based on signal strength
     */
    private float estimateSpeedFromSignal(int signalLevel) {
        if (signalLevel >= -50) {
            return 50f; // Excellent signal
        } else if (signalLevel >= -60) {
            return 40f; // Good signal
        } else if (signalLevel >= -70) {
            return 25f; // Fair signal
        } else if (signalLevel >= -80) {
            return 10f; // Poor signal
        } else {
            return 5f; // Very poor signal
        }
    }
    
    /**
     * Estimate latency based on signal strength
     */
    private int estimateLatencyFromSignal(int signalLevel) {
        if (signalLevel >= -50) {
            return 15; // Excellent signal
        } else if (signalLevel >= -60) {
            return 25; // Good signal
        } else if (signalLevel >= -70) {
            return 40; // Fair signal
        } else if (signalLevel >= -80) {
            return 60; // Poor signal
        } else {
            return 100; // Very poor signal
        }
    }
}
