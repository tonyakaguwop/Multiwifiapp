package com.multiwifi.connector.implementation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.util.NetworkUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation for devices with native multi-WiFi support (Android 12+)
 */
@RequiresApi(api = Build.VERSION_CODES.S)
public class NativeMultiWifiImplementation implements MultiWifiImplementation {
    private static final String TAG = "NativeMultiWifiImpl";
    
    private Context context;
    private boolean isInitialized = false;
    private boolean isConnected = false;
    private final List<NetworkConnection> connectedNetworks = new ArrayList<>();
    private final Map<String, Network> networkMap = new HashMap<>();
    private final Map<String, ConnectivityManager.NetworkCallback> callbackMap = new HashMap<>();
    private ConnectivityManager connectivityManager;
    
    @Override
    public boolean initialize(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager is null");
            return false;
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Log.e(TAG, "Device does not support native multi-WiFi (requires Android 12+)");
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
        
        disconnectAll(); // Disconnect existing connections first
        
        // Request each network
        for (NetworkConnection network : networks) {
            requestNetwork(network);
            network.setConnectionMethod(ConnectionMethod.NATIVE);
        }
        
        return true;
    }
    
    private void requestNetwork(NetworkConnection network) {
        // Implementation for Android 12+ to request multiple networks
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network newNetwork) {
                super.onAvailable(newNetwork);
                Log.d(TAG, "Network " + network.getSsid() + " connected");
                
                // Store the network and callback
                networkMap.put(network.getSsid(), newNetwork);
                
                // Update connection status
                network.setConnected(true);
                if (!connectedNetworks.contains(network)) {
                    connectedNetworks.add(network);
                }
                
                isConnected = true;
                
                // Test the speed of this connection
                testNetworkSpeed(network);
            }
            
            @Override
            public void onLost(@NonNull Network lostNetwork) {
                super.onLost(lostNetwork);
                Log.d(TAG, "Network " + network.getSsid() + " disconnected");
                
                // Remove from maps
                networkMap.remove(network.getSsid());
                
                // Update connection status
                network.setConnected(false);
                connectedNetworks.remove(network);
                
                isConnected = !connectedNetworks.isEmpty();
            }
            
            @Override
            public void onCapabilitiesChanged(@NonNull Network changedNetwork, 
                                             @NonNull NetworkCapabilities capabilities) {
                super.onCapabilitiesChanged(changedNetwork, capabilities);
                
                // Update network metrics
                int downstreamBandwidth = capabilities.getLinkDownstreamBandwidthKbps();
                double speedMbps = downstreamBandwidth / 1000.0;
                network.setSpeedMbps(speedMbps);
                
                Log.d(TAG, "Network " + network.getSsid() + " speed: " + speedMbps + " Mbps");
            }
        };
        
        connectivityManager.requestNetwork(request, callback);
        callbackMap.put(network.getSsid(), callback);
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
        
        // Unregister all callbacks
        for (Map.Entry<String, ConnectivityManager.NetworkCallback> entry : callbackMap.entrySet()) {
            try {
                connectivityManager.unregisterNetworkCallback(entry.getValue());
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering callback for " + entry.getKey(), e);
            }
        }
        
        // Clear collections
        callbackMap.clear();
        networkMap.clear();
        
        // Update status
        for (NetworkConnection network : connectedNetworks) {
            network.setConnected(false);
        }
        connectedNetworks.clear();
        isConnected = false;
        
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
        // In native implementation, the Android system handles traffic allocation
        Log.d(TAG, "Traffic allocation is handled by the system");
    }
    
    @Override
    public void cleanup() {
        disconnectAll();
        isInitialized = false;
    }
}
