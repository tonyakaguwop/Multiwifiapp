package com.multiwifi.connector.implementation;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.util.NetworkUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Implementation using USB WiFi adapter
 */
public class UsbWifiImplementation implements MultiWifiImplementation {
    private static final String TAG = "UsbWifiImplementation";
    
    private Context context;
    private boolean isInitialized = false;
    private boolean isConnected = false;
    private final List<NetworkConnection> connectedNetworks = new ArrayList<>();
    private UsbManager usbManager;
    private UsbDevice usbWifiAdapter;
    private UsbDeviceConnection usbConnection;
    
    @Override
    public boolean initialize(Context context) {
        this.context = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        
        if (usbManager == null) {
            Log.e(TAG, "UsbManager is null");
            return false;
        }
        
        // Find USB WiFi adapter
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.isEmpty()) {
            Log.e(TAG, "No USB devices found");
            return false;
        }
        
        // Note: In a real implementation, we would need to identify the USB WiFi adapter
        // based on vendor/product IDs or other characteristics. This is a simplified version.
        
        // For demonstration, just take the first device
        for (UsbDevice device : deviceList.values()) {
            Log.d(TAG, "Found USB device: " + device.getDeviceName());
            usbWifiAdapter = device;
            break;
        }
        
        if (usbWifiAdapter == null) {
            Log.e(TAG, "USB WiFi adapter not found");
            return false;
        }
        
        // Request permission and open connection
        if (usbManager.hasPermission(usbWifiAdapter)) {
            openConnection();
        } else {
            Log.e(TAG, "No permission for USB device");
            return false;
        }
        
        isInitialized = usbConnection != null;
        return isInitialized;
    }
    
    private void openConnection() {
        usbConnection = usbManager.openDevice(usbWifiAdapter);
        if (usbConnection == null) {
            Log.e(TAG, "Failed to open USB connection");
        } else {
            Log.d(TAG, "USB connection opened successfully");
        }
    }
    
    @Override
    public List<NetworkConnection> scanNetworks() {
        if (!isInitialized) {
            Log.e(TAG, "Implementation not initialized");
            return new ArrayList<>();
        }
        
        // In a real implementation, we would use the USB WiFi adapter to scan for networks.
        // For this demo, we'll use the built-in WiFi to simulate it.
        return NetworkUtils.scanNetworks(context);
    }
    
    @Override
    public boolean connectToNetworks(List<NetworkConnection> networks) {
        if (!isInitialized) {
            Log.e(TAG, "Implementation not initialized");
            return false;
        }
        
        // In a real implementation, we would use the USB WiFi adapter to connect to networks.
        // This is simplified for the demo.
        
        Log.d(TAG, "Connecting to " + networks.size() + " networks via USB adapter");
        
        for (NetworkConnection network : networks) {
            // Simulate connection
            network.setConnected(true);
            network.setConnectionMethod(ConnectionMethod.USB_ADAPTER);
            network.setSpeedMbps(25.0); // Simulated speed
            network.setLatencyMs(20);   // Simulated latency
            
            if (!connectedNetworks.contains(network)) {
                connectedNetworks.add(network);
            }
            
            Log.d(TAG, "Connected to " + network.getSsid() + " via USB adapter");
        }
        
        isConnected = !connectedNetworks.isEmpty();
        
        return true;
    }
    
    @Override
    public boolean disconnectAll() {
        if (!isInitialized) {
            Log.e(TAG, "Implementation not initialized");
            return false;
        }
        
        // Disconnect all networks
        for (NetworkConnection network : connectedNetworks) {
            network.setConnected(false);
            Log.d(TAG, "Disconnected from " + network.getSsid());
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
        // Apply allocation percentages to the USB adapter's routing table
        // This would be implemented with actual USB commands to the adapter
        Log.d(TAG, "Updated traffic allocation on USB adapter");
        
        for (NetworkConnection network : networks) {
            Log.d(TAG, network.getSsid() + " allocation: " + network.getAllocationPercentage() + "%");
        }
    }
    
    @Override
    public void cleanup() {
        disconnectAll();
        
        if (usbConnection != null) {
            usbConnection.close();
            usbConnection = null;
        }
        
        isInitialized = false;
    }
}
