package com.multiwifi.connector.model;

/**
 * Enum representing the different connection methods available for multi-WiFi implementation
 */
public enum ConnectionMethod {
    /**
     * Native multi-WiFi implementation available on newer Android devices (Android 12+)
     */
    NATIVE,
    
    /**
     * Implementation using USB WiFi adapter connected via OTG
     */
    USB_ADAPTER,
    
    /**
     * Hybrid implementation combining WiFi and cellular data
     */
    HYBRID,
    
    /**
     * Proxy-based implementation as fallback option
     */
    PROXY,
    
    /**
     * VPN-based implementation for non-root devices
     */
    VPN
}
