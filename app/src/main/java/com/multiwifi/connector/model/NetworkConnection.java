package com.multiwifi.connector.model;

/**
 * Class representing a network connection with performance metrics
 */
public class NetworkConnection {
    private String ssid;
    private String bssid;
    private int signalStrength;
    private double speedMbps;
    private int latencyMs;
    private double allocationPercentage;
    private boolean isConnected;
    private ConnectionMethod connectionMethod;

    public NetworkConnection(String ssid, String bssid, int signalStrength) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.signalStrength = signalStrength;
        this.speedMbps = 0.0;
        this.latencyMs = 0;
        this.allocationPercentage = 0.0;
        this.isConnected = false;
        this.connectionMethod = ConnectionMethod.PROXY;  // Default
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public double getSpeedMbps() {
        return speedMbps;
    }

    public void setSpeedMbps(double speedMbps) {
        this.speedMbps = speedMbps;
    }

    public int getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(int latencyMs) {
        this.latencyMs = latencyMs;
    }

    public double getAllocationPercentage() {
        return allocationPercentage;
    }

    public void setAllocationPercentage(double allocationPercentage) {
        this.allocationPercentage = allocationPercentage;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public ConnectionMethod getConnectionMethod() {
        return connectionMethod;
    }

    public void setConnectionMethod(ConnectionMethod connectionMethod) {
        this.connectionMethod = connectionMethod;
    }

    @Override
    public String toString() {
        return "NetworkConnection{" +
                "ssid='" + ssid + '\'' +
                ", bssid='" + bssid + '\'' +
                ", signalStrength=" + signalStrength +
                ", speedMbps=" + speedMbps +
                ", latencyMs=" + latencyMs +
                ", allocationPercentage=" + allocationPercentage +
                ", isConnected=" + isConnected +
                ", connectionMethod=" + connectionMethod +
                '}';
    }
}
