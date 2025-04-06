package com.multiwifi.connector.model;

/**
 * Class representing the capabilities of the device for multi-WiFi connectivity
 */
public class DeviceCapabilities {
    private boolean supportsNativeMultiWifi;
    private boolean supportsUsbOtg;
    private boolean supportsCellularData;
    private boolean hasProxySupport;
    private boolean hasVpnSupport;
    private ConnectionMethod recommendedMethod;

    public DeviceCapabilities() {
        // Default all capabilities to false initially
        this.supportsNativeMultiWifi = false;
        this.supportsUsbOtg = false;
        this.supportsCellularData = false;
        
        // Proxy and VPN are always supported as fallback
        this.hasProxySupport = true;
        this.hasVpnSupport = true;
        
        // Default recommended method is VPN (non-root solution)
        this.recommendedMethod = ConnectionMethod.VPN;
    }

    public boolean isSupportsNativeMultiWifi() {
        return supportsNativeMultiWifi;
    }

    public void setSupportsNativeMultiWifi(boolean supportsNativeMultiWifi) {
        this.supportsNativeMultiWifi = supportsNativeMultiWifi;
        
        // If native is supported, it becomes the recommended method
        if (supportsNativeMultiWifi) {
            this.recommendedMethod = ConnectionMethod.NATIVE;
        }
    }

    public boolean isSupportsUsbOtg() {
        return supportsUsbOtg;
    }

    public void setSupportsUsbOtg(boolean supportsUsbOtg) {
        this.supportsUsbOtg = supportsUsbOtg;
        
        // If USB OTG is supported and native is not, USB becomes recommended
        if (supportsUsbOtg && !supportsNativeMultiWifi) {
            this.recommendedMethod = ConnectionMethod.USB_ADAPTER;
        }
    }

    public boolean isSupportsCellularData() {
        return supportsCellularData;
    }

    public void setSupportsCellularData(boolean supportsCellularData) {
        this.supportsCellularData = supportsCellularData;
        
        // If cellular is supported but native and USB are not, hybrid becomes recommended
        if (supportsCellularData && !supportsNativeMultiWifi && !supportsUsbOtg) {
            this.recommendedMethod = ConnectionMethod.HYBRID;
        }
    }

    public boolean isHasProxySupport() {
        return hasProxySupport;
    }

    public void setHasProxySupport(boolean hasProxySupport) {
        this.hasProxySupport = hasProxySupport;
    }
    
    public boolean isHasVpnSupport() {
        return hasVpnSupport;
    }

    public void setHasVpnSupport(boolean hasVpnSupport) {
        this.hasVpnSupport = hasVpnSupport;
        
        // If VPN is supported but native, USB, and cellular are not, 
        // VPN becomes recommended over proxy
        if (hasVpnSupport && !supportsNativeMultiWifi && !supportsUsbOtg && !supportsCellularData) {
            this.recommendedMethod = ConnectionMethod.VPN;
        }
    }

    public ConnectionMethod getRecommendedMethod() {
        return recommendedMethod;
    }

    public void setRecommendedMethod(ConnectionMethod recommendedMethod) {
        this.recommendedMethod = recommendedMethod;
    }

    @Override
    public String toString() {
        return "DeviceCapabilities{" +
                "supportsNativeMultiWifi=" + supportsNativeMultiWifi +
                ", supportsUsbOtg=" + supportsUsbOtg +
                ", supportsCellularData=" + supportsCellularData +
                ", hasProxySupport=" + hasProxySupport +
                ", hasVpnSupport=" + hasVpnSupport +
                ", recommendedMethod=" + recommendedMethod +
                '}';
    }
}
