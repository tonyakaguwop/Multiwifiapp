package com.multiwifi.connector.implementation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ProxyInfo;
import android.os.Build;
import android.util.Log;

import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.util.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation using proxy servers to route traffic
 */
public class ProxyImplementation implements MultiWifiImplementation {
    private static final String TAG = "ProxyImplementation";
    private static final String PROXY_HOST = "proxy.multiwifi.example.com";
    private static final int PROXY_PORT = 8080;
    
    private Context context;
    private boolean isInitialized = false;
    private boolean isConnected = false;
    private final List<NetworkConnection> connectedNetworks = new ArrayList<>();
    private ConnectivityManager connectivityManager;
    
    @Override
    public boolean initialize(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager is null");
            return false;
        }
        
        isInitialized = true;
        return true;
    }
    
    @Override
    public List<NetworkConnection> scanNetworks() {
        if (!isInitialized) {
            Log.e(TAG, "Implementation not initialized");
            return new ArrayList<>();
        }
        
        return NetworkUtils.scanNetworks(context);
    }
    
    @Override
    public boolean connectToNetworks(List<NetworkConnection> networks) {
        if (!isInitialized) {
            Log.e(TAG, "Implementation not initialized");
            return false;
        }
        
        // Connect to a single WiFi network and use proxy for traffic splitting
        if (networks.isEmpty()) {
            Log.e(TAG, "No networks to connect to");
            return false;
        }
        
        // Connect to the first network
        NetworkConnection primaryNetwork = networks.get(0);
        boolean connected = NetworkUtils.connectToNetwork(
                context,
                primaryNetwork.getSsid(),
                null, // Password would be provided in a real app
                ConnectionMethod.PROXY
        );
        
        if (connected) {
            primaryNetwork.setConnected(true);
            primaryNetwork.setConnectionMethod(ConnectionMethod.PROXY);
            
            if (!connectedNetworks.contains(primaryNetwork)) {
                connectedNetworks.add(primaryNetwork);
            }
            
            Log.d(TAG, "Connected to " + primaryNetwork.getSsid() + " via proxy");
            
            // Set up proxy
            setupProxy();
            
            // Test network speed
            testNetworkSpeed(primaryNetwork);
            
            isConnected = true;
        } else {
            Log.e(TAG, "Failed to connect to " + primaryNetwork.getSsid());
            isConnected = false;
        }
        
        return isConnected;
    }
    
    private void setupProxy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Set global proxy
            // Note: This requires system/root privileges on most devices
            // In a real app, we would use VPN service to route traffic through our proxy
            ProxyInfo proxyInfo = ProxyInfo.buildDirectProxy(PROXY_HOST, PROXY_PORT);
            
            Log.d(TAG, "Proxy set up at " + PROXY_HOST + ":" + PROXY_PORT);
        } else {
            Log.w(TAG, "Setting proxy requires Android 6.0+");
        }
    }
    
    private void testNetworkSpeed(NetworkConnection network) {
        NetworkUtils.testConnectionSpeed(new NetworkUtils.SpeedTestCallback() {
            @Override
            public void onSpeedTested(double speedMbps) {
                network.setSpeedMbps(speedMbps);
                Log.d(TAG, "Speed test for " + network.getSsid() + ": " + speedMbps + " Mbps");
            }
            
            @Override
            public void onSpeedTestFailed(String errorMessage) {
                Log.e(TAG, "Speed test failed for " + network.getSsid() + ": " + errorMessage);
            }
        });
    }
    
    @Override
    public boolean disconnectAll() {
        if (!isInitialized) {
            Log.e(TAG, "Implementation not initialized");
            return false;
        }
        
        // Remove proxy settings
        removeProxy();
        
        // Disconnect all networks
        for (NetworkConnection network : connectedNetworks) {
            network.setConnected(false);
            Log.d(TAG, "Disconnected from " + network.getSsid());
        }
        
        connectedNetworks.clear();
        isConnected = false;
        
        return true;
    }
    
    private void removeProxy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Clear proxy settings
            // Again, this requires system privileges
            Log.d(TAG, "Proxy settings removed");
        }
    }
    
    @Override
    public double getCombinedSpeed() {
        if (!isConnected) {
            return 0.0;
        }
        
        // In proxy implementation, the combined speed is determined by the proxy server
        // For demo purposes, we'll just use the connected network's speed
        if (!connectedNetworks.isEmpty()) {
            return connectedNetworks.get(0).getSpeedMbps();
        }
        
        return 0.0;
    }
    
    @Override
    public boolean isConnected() {
        return isConnected;
    }
    
    @Override
    public List<NetworkConnection> getConnectedNetworks() {
        return new ArrayList<>(connectedNetworks);
    }
    
    @Override
    public void updateAllocation(List<NetworkConnection> networks) {
        // Send allocation updates to the proxy server
        Log.d(TAG, "Sending allocation updates to proxy server");
        
        for (NetworkConnection network : networks) {
            Log.d(TAG, network.getSsid() + " allocation: " + network.getAllocationPercentage() + "%");
        }
    }
    
    @Override
    public void cleanup() {
        disconnectAll();
        isInitialized = false;
    }
}
