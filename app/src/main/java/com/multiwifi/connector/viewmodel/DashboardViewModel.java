package com.multiwifi.connector.viewmodel;

import com.multiwifi.connector.model.NetworkConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the Dashboard screen
 */
public class DashboardViewModel {
    
    private List<NetworkConnection> savedNetworks;
    
    public DashboardViewModel() {
        savedNetworks = new ArrayList<>();
    }
    
    /**
     * Gets the saved networks
     * 
     * @return List of saved networks
     */
    public List<NetworkConnection> getSavedNetworks() {
        return savedNetworks;
    }
    
    /**
     * Sets the saved networks
     * 
     * @param networks List of networks to save
     */
    public void setSavedNetworks(List<NetworkConnection> networks) {
        if (networks != null) {
            this.savedNetworks = new ArrayList<>(networks);
        } else {
            this.savedNetworks.clear();
        }
    }
    
    /**
     * Adds a network to the saved networks
     * 
     * @param network The network to add
     */
    public void addSavedNetwork(NetworkConnection network) {
        if (network != null && !savedNetworks.contains(network)) {
            savedNetworks.add(network);
        }
    }
    
    /**
     * Removes a network from the saved networks
     * 
     * @param network The network to remove
     */
    public void removeSavedNetwork(NetworkConnection network) {
        if (network != null) {
            savedNetworks.remove(network);
        }
    }
    
    /**
     * Clears all saved networks
     */
    public void clearSavedNetworks() {
        savedNetworks.clear();
    }
}
