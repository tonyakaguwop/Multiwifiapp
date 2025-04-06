package com.multiwifi.connector.model;

/**
 * Enum representing the different connection methods supported by the app
 */
public enum ConnectionMethod {
    NATIVE,        // Native multi-WiFi using Android 12+ APIs
    USB_ADAPTER,   // External USB WiFi adapter
    HYBRID,        // WiFi + Cellular data hybrid
    PROXY          // Proxy-based connection combining
}
