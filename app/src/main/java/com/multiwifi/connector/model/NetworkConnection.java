package com.multiwifi.connector.model;

/**
 * Class representing a network connection (WiFi, Cellular, USB adapter)
 */
public class NetworkConnection {
    
    public enum Type {
        WIFI,
        CELLULAR,
        USB_ADAPTER,
        PROXY
    }
    
    private String ssid;
    private Type type;
    private float speed; // In Mbps
    private int latency; // In milliseconds
    private int signalStrength; // RSSI value for WiFi
    private float allocationPercentage; // Load allocation percentage
    private boolean isActive;
    
    public NetworkConnection() {
        // Default constructor
        this.isActive = false;
        this.allocationPercentage = 0;
    }
    
    public NetworkConnection(String ssid, Type type) {
        this.ssid = ssid;
        this.type = type;
        this.isActive = false;
        this.allocationPercentage = 0;
    }
    
    public String getSsid() {
        return ssid;
    }
    
    public void setSsid(String ssid) {
        this.ssid = ssid;
    }
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public float getSpeed() {
        return speed;
    }
    
    public void setSpeed(float speed) {
        this.speed = speed;
    }
    
    public int getLatency() {
        return latency;
    }
    
    public void setLatency(int latency) {
        this.latency = latency;
    }
    
    public int getSignalStrength() {
        return signalStrength;
    }
    
    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }
    
    public float getAllocationPercentage() {
        return allocationPercentage;
    }
    
    public void setAllocationPercentage(float allocationPercentage) {
        this.allocationPercentage = allocationPercentage;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    /**
     * Get a human-readable name for this connection
     */
    public String getDisplayName() {
        switch (type) {
            case WIFI:
                return ssid;
            case CELLULAR:
                return "Cellular Data";
            case USB_ADAPTER:
                return "USB WiFi: " + ssid;
            case PROXY:
                return "Proxy Connection";
            default:
                return "Unknown Connection";
        }
    }
}
