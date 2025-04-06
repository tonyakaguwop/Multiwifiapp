package com.multiwifi.connector.implementation;

import android.content.Context;

import com.multiwifi.connector.model.NetworkConnection;

import java.util.List;

/**
 * Interface for different multi-WiFi connection implementations
 */
public interface MultiWifiImplementation {
    /**
     * Initializes the implementation
     * 
     * @param context The application context
     * @return true if initialization was successful, false otherwise
     */
    boolean initialize(Context context);
    
    /**
     * Scans for available networks
     * 
     * @return List of available networks
     */
    List<NetworkConnection> scanNetworks();
    
    /**
     * Connects to multiple networks
     * 
     * @param networks List of networks to connect to
     * @return true if connection process was initiated successfully, false otherwise
     */
    boolean connectToNetworks(List<NetworkConnection> networks);
    
    /**
     * Disconnects from all networks
     * 
     * @return true if disconnection was successful, false otherwise
     */
    boolean disconnectAll();
    
    /**
     * Gets the combined speed of all connected networks
     * 
     * @return Combined speed in Mbps
     */
    double getCombinedSpeed();
    
    /**
     * Gets the status of the implementation
     * 
     * @return true if connected, false otherwise
     */
    boolean isConnected();
    
    /**
     * Gets currently connected networks
     * 
     * @return List of connected networks
     */
    List<NetworkConnection> getConnectedNetworks();
    
    /**
     * Updates the allocation percentages for the networks
     * 
     * @param networks List of networks with updated allocation percentages
     */
    void updateAllocation(List<NetworkConnection> networks);
    
    /**
     * Cleans up resources used by the implementation
     */
    void cleanup();
}
