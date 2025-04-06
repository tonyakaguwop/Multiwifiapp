package com.multiwifi.connector.implementation;

import com.multiwifi.connector.model.NetworkConnection;
import java.util.List;

/**
 * Interface for different multi-WiFi implementations
 */
public interface MultiWifiImplementation {
    
    /**
     * Initialize the implementation
     */
    boolean initialize();
    
    /**
     * Connect to multiple networks
     */
    boolean connect();
    
    /**
     * Disconnect from all networks
     */
    boolean disconnect();
    
    /**
     * Check if currently connected
     */
    boolean isConnected();
    
    /**
     * Scan for available networks
     */
    List<NetworkConnection> scanNetworks();
    
    /**
     * Get list of currently connected networks
     */
    List<NetworkConnection> getConnectedNetworks();
    
    /**
     * Add a specific network
     */
    boolean addNetwork(String ssid, String password);
    
    /**
     * Remove a specific network
     */
    boolean removeNetwork(String ssid);
    
    /**
     * Get combined connection speed in Mbps
     */
    float getCombinedSpeed();
    
    /**
     * Update connection metrics
     */
    void updateMetrics();
    
    /**
     * Release resources
     */
    void cleanup();
}
