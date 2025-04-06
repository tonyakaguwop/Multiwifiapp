package com.multiwifi.connector.model;

/**
 * Class representing the device's network capabilities
 */
public class DeviceCapabilities {
    
    private boolean supportsNativeMultiWifi;
    private boolean supportsUsbAdapter;
    private boolean supportsCellularData;
    private ConnectionMethod recommendedMethod;
    
    public DeviceCapabilities() {
        // Default values
        this.supportsNativeMultiWifi = false;
        this.supportsUsbAdapter = false;
        this.supportsCellularData = false;
        this.recommendedMethod = ConnectionMethod.PROXY; // Default to proxy method
    }
    
    public boolean isSupportsNativeMultiWifi() {
        return supportsNativeMultiWifi;
    }
    
    public void setSupportsNativeMultiWifi(boolean supportsNativeMultiWifi) {
        this.supportsNativeMultiWifi = supportsNativeMultiWifi;
    }
    
    public boolean isSupportsUsbAdapter() {
        return supportsUsbAdapter;
    }
    
    public void setSupportsUsbAdapter(boolean supportsUsbAdapter) {
        this.supportsUsbAdapter = supportsUsbAdapter;
    }
    
    public boolean isSupportsCellularData() {
        return supportsCellularData;
    }
    
    public void setSupportsCellularData(boolean supportsCellularData) {
        this.supportsCellularData = supportsCellularData;
    }
    
    public ConnectionMethod getRecommendedMethod() {
        return recommendedMethod;
    }
    
    public void setRecommendedMethod(ConnectionMethod recommendedMethod) {
        this.recommendedMethod = recommendedMethod;
    }
}
