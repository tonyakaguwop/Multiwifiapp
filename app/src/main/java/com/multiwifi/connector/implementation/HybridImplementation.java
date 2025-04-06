package com.multiwifi.connector.implementation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.util.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation combining WiFi and cellular data
 */
public class HybridImplementation implements MultiWifiImplementation {
    private static final String TAG = "HybridImplementation";
    
    private Context context;
    private boolean isInitialized = false;
    private boolean isConnected = false;
    private final List<NetworkConnection> connectedNetworks = new ArrayList<>();
    private ConnectivityManager connectivityManager;
    private NetworkConnection cellularNetwork;
    
    @Override
    public boolean initialize(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager is null");
            return false;
        }
        
        // Check if cellular data is available
        boolean hasCellular = checkCellularAvailability();
        if (!hasCellular) {
            Log.e(TAG, "Cellular data not available");
            return false;
        }
        
        // Create a network object for cellular
        cellularNetwork = new NetworkConnection(
                "Cellular Data",
                "cellular",
                -50 // Simulate signal strength
        );
        cellularNetwork.setConnectionMethod(ConnectionMethod.HYBRID);
        
        isInitialized = true;
        return true;
    }
    
    private boolean checkCellularAvailability() {
        Network[] networks = connectivityManager.getAllNetworks();
        
        for (Network network : networks) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                // Test cellular speed
                testCellularSpeed();
                return true;
            }
        }
        
        return false;
    }
    
    private void testCellularSpeed() {
        NetworkUtils.testConnectionSpeed(new NetworkUtils.SpeedTestCallback() {
            @Override
            public void onSpeedTested(double speedMbps) {
                if (cellularNetwork != null) {
                    cellularNetwork.setSpeedMbps(speedMbps);
                    Log.d(TAG, "Cellular speed: " + speedMbps + " Mbps");
                }
            }
            
            @Override
            public void onSpeedTestFailed(String errorMessage) {
                Log.e(TAG, "Cellular speed test failed: " + errorMessage);
                if (cellularNetwork != null) {
                    cellularNetwork.setSpeedMbps(5.0); // Default fallback speed
                }
            }
        });
    }
    
    @Override
    public List<NetworkConnection> scanNetworks() {
        if (!isInitialized) {
            Log.e(TAG, "Implementation not initialized");
            return new ArrayList<>();
        }
        
        List<NetworkConnection> networks = NetworkUtils.scanNetworks(context);
        
        // Add cellular network to the list
        if (!networks.contains(cellularNetwork)) {
            networks.add(cellularNetwork);
        }
        
        return networks;
    }
    
    @Override
    public boolean connectToNetworks(List<NetworkConnection> networks) {
        if (!isInitialized) {
            Log.e(TAG, "Implementation not initialized");
            return false;
        }
        
        // Disconnect existing connections first
        disconnectAll();
        
        // Connect to WiFi networks
        for (NetworkConnection network : networks) {
            if ("Cellular Data".equals(network.getSsid())) {
                // Connect to cellular
                network.setConnected(true);
                if (!connectedNetworks.contains(network)) {
                    connectedNetworks.add(network);
                }
                Log.d(TAG, "Connected to cellular data");
            } else {
                // Connect to WiFi
                boolean connected = NetworkUtils.connectToNetwork(
                        context, 
                        network.getSsid(), 
                        null, // Password would be provided in a real app
                        ConnectionMethod.HYBRID
                );
                
                if (connected) {
                    network.setConnected(true);
                    network.setConnectionMethod(ConnectionMethod.HYBRID);
                    if (!connectedNetworks.contains(network)) {
                        connectedNetworks.add(network);
                    }
                    Log.d(TAG, "Connected to " + network.getSsid());
                    
                    // Test network speed
                    testNetworkSpeed(network);
                }
            }
        }
        
        isConnected = !connectedNetworks.isEmpty();
        return isConnected;
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
        
        // Disconnect all networks except cellular
        List<NetworkConnection> toRemove = new ArrayList<>();
        
        for (NetworkConnection network : connectedNetworks) {
            if (!"Cellular Data".equals(network.getSsid())) {
                network.setConnected(false);
                toRemove.add(network);
                Log.d(TAG, "Disconnected from " + network.getSsid());
            }
        }
        
        connectedNetworks.removeAll(toRemove);
        isConnected = !connectedNetworks.isEmpty();
        
        return true;
    }
    
    @Override
    public double getCombinedSpeed() {
        if (!isConnected) {
            return 0.0;
        }
        
        double totalSpeed = 0.0;
        for (NetworkConnection network : connectedNetworks) {
            totalSpeed += network.getSpeedMbps();
        }
        
        return totalSpeed;
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
        // In hybrid implementation, we adjust how traffic is split between WiFi and cellular
        Log.d(TAG, "Updated traffic allocation for hybrid connections");
        
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
