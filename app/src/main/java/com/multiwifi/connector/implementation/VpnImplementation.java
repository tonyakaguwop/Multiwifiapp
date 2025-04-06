package com.multiwifi.connector.implementation;

import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;

import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.service.MultiWifiVpnService;

import java.util.List;

/**
 * Implementation of multi-WiFi connectivity using VPN-based approach.
 * This approach works on non-rooted devices by creating a local VPN service
 * that captures all traffic and routes it through available networks.
 */
public class VpnImplementation extends MultiWifiImplementation {
    private static final String TAG = "VpnImplementation";
    
    private Intent vpnServiceIntent;
    private boolean vpnPermissionGranted = false;
    
    public VpnImplementation(Context context) {
        super(context);
    }
    
    @Override
    public ConnectionMethod getConnectionMethod() {
        return ConnectionMethod.VPN;
    }
    
    @Override
    public boolean isAvailable() {
        // VPN implementation is available on all devices
        return true;
    }
    
    @Override
    public boolean connect(List<NetworkConnection> networks) {
        if (networks == null || networks.isEmpty()) {
            Log.e(TAG, "Cannot connect: No networks available");
            return false;
        }
        
        if (!vpnPermissionGranted) {
            Log.e(TAG, "Cannot connect: VPN permission not granted");
            return false;
        }
        
        // Start the VPN service
        vpnServiceIntent = new Intent(context, MultiWifiVpnService.class);
        context.startService(vpnServiceIntent);
        
        // Update connected networks
        connectedNetworks.clear();
        connectedNetworks.addAll(networks);
        
        Log.i(TAG, "VPN-based multi-WiFi connection started with " + networks.size() + " networks");
        
        notifyConnectionEstablished(networks);
        return true;
    }
    
    @Override
    public void disconnect() {
        if (vpnServiceIntent != null) {
            context.stopService(vpnServiceIntent);
            vpnServiceIntent = null;
        }
        
        connectedNetworks.clear();
        
        Log.i(TAG, "VPN-based multi-WiFi connection stopped");
        notifyConnectionClosed();
    }
    
    /**
     * Request VPN permission from the user.
     * This method should be called from an Activity before attempting to connect.
     *
     * @param activityResultLauncher The launcher to use for the permission request
     * @return true if the request was launched, false otherwise
     */
    public boolean requestVpnPermission(ActivityResultLauncher<Intent> activityResultLauncher) {
        Intent vpnPrepareIntent = VpnService.prepare(context);
        
        if (vpnPrepareIntent != null) {
            // VPN permission not yet granted, launch the system dialog
            activityResultLauncher.launch(vpnPrepareIntent);
            return true;
        } else {
            // VPN permission already granted
            vpnPermissionGranted = true;
            return false;
        }
    }
    
    /**
     * Callback method to be called when the VPN permission dialog result is available.
     *
     * @param isGranted true if permission was granted, false otherwise
     */
    public void onVpnPermissionResult(boolean isGranted) {
        vpnPermissionGranted = isGranted;
        
        if (isGranted) {
            Log.i(TAG, "VPN permission granted");
        } else {
            Log.w(TAG, "VPN permission denied");
        }
    }
    
    @Override
    public boolean updateNetworks(List<NetworkConnection> networks) {
        if (!isConnected() || networks == null) {
            return false;
        }
        
        connectedNetworks.clear();
        connectedNetworks.addAll(networks);
        
        notifyNetworksUpdated(networks);
        return true;
    }
    
    @Override
    public void onNetworkStatusChanged(NetworkConnection network) {
        // Update network status in the VPN service
        // This would be implemented in a real app to update the service
        // with the latest performance metrics
    }
}
