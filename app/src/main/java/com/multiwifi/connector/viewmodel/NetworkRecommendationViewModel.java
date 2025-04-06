package com.multiwifi.connector.viewmodel;

import com.multiwifi.connector.model.NetworkConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for NetworkRecommendationActivity
 */
public class NetworkRecommendationViewModel {
    
    private List<NetworkConnection> selectedNetworks;
    
    /**
     * Default constructor
     */
    public NetworkRecommendationViewModel() {
        selectedNetworks = new ArrayList<>();
    }
    
    /**
     * Saves the selected networks
     * 
     * @param networks List of networks to save
     */
    public void saveSelectedNetworks(List<NetworkConnection> networks) {
        selectedNetworks = new ArrayList<>(networks);
    }
    
    /**
     * Gets the selected networks
     * 
     * @return List of selected networks
     */
    public List<NetworkConnection> getSelectedNetworks() {
        return selectedNetworks;
    }
}
