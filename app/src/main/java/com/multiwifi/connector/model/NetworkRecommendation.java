package com.multiwifi.connector.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class for network connection recommendations
 */
public class NetworkRecommendation {
    
    /**
     * Recommendation type enum
     */
    public enum RecommendationType {
        SPEED_OPTIMIZED("Speed Optimized"),
        RELIABILITY_OPTIMIZED("Reliability Optimized"),
        BALANCED("Balanced Performance"),
        POWER_SAVING("Power Efficient"),
        CUSTOM("Custom Configuration");
        
        private final String label;
        
        RecommendationType(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
    }
    
    private String title;
    private String description;
    private String detailedDescription;
    private RecommendationType type;
    private List<NetworkConnection> networks;
    private double estimatedSpeed;
    private double estimatedReliability;
    private double estimatedPowerUsage;
    private boolean isSelected;
    
    /**
     * Default constructor
     */
    public NetworkRecommendation() {
        networks = new ArrayList<>();
        isSelected = false;
    }
    
    /**
     * Constructor with network list
     * 
     * @param type Recommendation type
     * @param networks Networks for this recommendation
     */
    public NetworkRecommendation(RecommendationType type, List<NetworkConnection> networks) {
        this.type = type;
        this.networks = new ArrayList<>(networks);
        this.title = type.getLabel() + " Configuration";
        generateDescription();
        isSelected = false;
    }
    
    /**
     * Generates summary and detailed descriptions based on networks and type
     */
    private void generateDescription() {
        // Basic description
        StringBuilder summary = new StringBuilder();
        switch (type) {
            case SPEED_OPTIMIZED:
                summary.append("Combines ").append(networks.size()).append(" networks for maximum speed");
                break;
            case RELIABILITY_OPTIMIZED:
                summary.append("Uses ").append(networks.size()).append(" stable networks for reliable connection");
                break;
            case BALANCED:
                summary.append("Balances ").append(networks.size()).append(" networks for good speed and reliability");
                break;
            case POWER_SAVING:
                summary.append("Efficient setup using ").append(networks.size()).append(" networks to save battery");
                break;
            case CUSTOM:
                summary.append("Custom setup with ").append(networks.size()).append(" networks");
                break;
        }
        this.description = summary.toString();
        
        // Detailed description
        StringBuilder details = new StringBuilder();
        details.append(description).append("\n\n");
        details.append("Estimated Performance:\n");
        details.append("• Speed: ").append(String.format("%.1f Mbps", estimatedSpeed)).append("\n");
        details.append("• Reliability: ").append(String.format("%.0f%%", estimatedReliability * 100)).append("\n");
        details.append("• Power Usage: ").append(getPowerUsageDesc()).append("\n\n");
        
        details.append("Networks:\n");
        for (NetworkConnection network : networks) {
            details.append("• ").append(network.getSsid())
                   .append(" (").append(String.format("%.1f Mbps", network.getSpeedMbps())).append(")\n");
        }
        
        this.detailedDescription = details.toString();
    }
    
    /**
     * Gets a text description of power usage
     * 
     * @return Power usage description
     */
    private String getPowerUsageDesc() {
        if (estimatedPowerUsage < 0.3) {
            return "Low";
        } else if (estimatedPowerUsage < 0.7) {
            return "Medium";
        } else {
            return "High";
        }
    }

    // Getters and setters
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public void setDetailedDescription(String detailedDescription) {
        this.detailedDescription = detailedDescription;
    }

    public RecommendationType getType() {
        return type;
    }

    public void setType(RecommendationType type) {
        this.type = type;
        generateDescription();
    }

    public List<NetworkConnection> getNetworks() {
        return networks;
    }

    public void setNetworks(List<NetworkConnection> networks) {
        this.networks = networks;
        generateDescription();
    }

    public double getEstimatedSpeed() {
        return estimatedSpeed;
    }

    public void setEstimatedSpeed(double estimatedSpeed) {
        this.estimatedSpeed = estimatedSpeed;
        generateDescription();
    }

    public double getEstimatedReliability() {
        return estimatedReliability;
    }

    public void setEstimatedReliability(double estimatedReliability) {
        this.estimatedReliability = estimatedReliability;
        generateDescription();
    }

    public double getEstimatedPowerUsage() {
        return estimatedPowerUsage;
    }

    public void setEstimatedPowerUsage(double estimatedPowerUsage) {
        this.estimatedPowerUsage = estimatedPowerUsage;
        generateDescription();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
