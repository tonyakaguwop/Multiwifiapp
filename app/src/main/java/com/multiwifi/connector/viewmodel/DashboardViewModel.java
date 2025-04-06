package com.multiwifi.connector.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.service.MultiWifiService;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the Dashboard screen
 */
public class DashboardViewModel extends ViewModel {
    
    private MultiWifiService multiWifiService;
    
    private final MutableLiveData<Boolean> connectionStatus = new MutableLiveData<>(false);
    private final MutableLiveData<Double> combinedSpeed = new MutableLiveData<>(0.0);
    private final MutableLiveData<List<NetworkConnection>> networkConnections = new MutableLiveData<>(new ArrayList<>());
    
    /**
     * Set the MultiWifiService instance
     */
    public void setMultiWifiService(MultiWifiService service) {
        this.multiWifiService = service;
        
        // Update LiveData with current service state
        updateFromService();
    }
    
    /**
     * Connect to networks
     */
    public void connect() {
        if (multiWifiService != null) {
            boolean connected = multiWifiService.connect();
            
            if (connected) {
                updateFromService();
            }
        }
    }
    
    /**
     * Disconnect from networks
     */
    public void disconnect() {
        if (multiWifiService != null) {
            boolean disconnected = multiWifiService.disconnect();
            
            if (disconnected) {
                updateFromService();
            }
        }
    }
    
    /**
     * Add a specific network
     */
    public boolean addNetwork(String ssid, String password) {
        if (multiWifiService != null) {
            boolean added = multiWifiService.addNetwork(ssid, password);
            
            if (added) {
                updateFromService();
            }
            
            return added;
        }
        
        return false;
    }
    
    /**
     * Remove a specific network
     */
    public boolean removeNetwork(String ssid) {
        if (multiWifiService != null) {
            boolean removed = multiWifiService.removeNetwork(ssid);
            
            if (removed) {
                updateFromService();
            }
            
            return removed;
        }
        
        return false;
    }
    
    /**
     * Scan for available networks
     */
    public List<NetworkConnection> scanNetworks() {
        if (multiWifiService != null) {
            return multiWifiService.scanNetworks();
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Update all LiveData from the service
     */
    private void updateFromService() {
        if (multiWifiService != null) {
            // Update connection status
            connectionStatus.setValue(multiWifiService.isConnected());
            
            // Update combined speed
            combinedSpeed.setValue((double) multiWifiService.getCombinedSpeed());
            
            // Update network connections
            networkConnections.setValue(multiWifiService.getConnectedNetworks());
        }
    }
    
    /**
     * Get the connection status LiveData
     */
    public LiveData<Boolean> getConnectionStatus() {
        return connectionStatus;
    }
    
    /**
     * Get the combined speed LiveData
     */
    public LiveData<Double> getCombinedSpeed() {
        return combinedSpeed;
    }
    
    /**
     * Get the network connections LiveData
     */
    public LiveData<List<NetworkConnection>> getNetworkConnections() {
        return networkConnections;
    }
}
