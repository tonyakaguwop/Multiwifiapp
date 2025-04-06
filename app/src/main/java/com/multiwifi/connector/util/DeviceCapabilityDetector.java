package com.multiwifi.connector.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.DeviceCapabilities;

/**
 * Utility class to detect device capabilities for multi-WiFi connections
 */
public class DeviceCapabilityDetector {
    private static final String TAG = "DeviceCapabilityDetector";

    /**
     * Detects capabilities of the device and determines the best connection method
     * 
     * @param context The application context
     * @return DeviceCapabilities object with detected capabilities
     */
    public static DeviceCapabilities detectCapabilities(Context context) {
        DeviceCapabilities capabilities = new DeviceCapabilities();
        
        // Check for native multi-WiFi support (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            capabilities.setSupportsNativeMultiWifi(true);
            Log.d(TAG, "Device supports native multi-WiFi connections");
        }
        
        // Check for USB OTG support
        boolean hasUsbOtg = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST);
        capabilities.setSupportsUsbOtg(hasUsbOtg);
        if (hasUsbOtg) {
            Log.d(TAG, "Device supports USB OTG");
        }
        
        // Check for cellular data capability
        boolean hasCellular = checkCellularSupport(context);
        capabilities.setSupportsCellularData(hasCellular);
        if (hasCellular) {
            Log.d(TAG, "Device supports cellular data");
        }
        
        // Check for proxy and VPN support
        capabilities.setHasProxySupport(hasProxySupport(context));
        capabilities.setHasVpnSupport(hasVpnSupport(context));
        
        Log.d(TAG, "Recommended connection method: " + capabilities.getRecommendedMethod());
        return capabilities;
    }
    
    /**
     * Checks if the device supports VPN implementation
     *
     * @param context The application context
     * @return true if VPN implementation is supported
     */
    public static boolean hasVpnSupport(Context context) {
        // VPN is supported on all Android devices running 4.0+
        // Our app targets Android 8.0+ so VPN is always supported
        return true;
    }
    
    /**
     * Checks if the device supports proxy implementation
     *
     * @param context The application context
     * @return true if proxy implementation is supported
     */
    public static boolean hasProxySupport(Context context) {
        // Proxy implementation is supported on all devices
        return true;
    }
    
    /**
     * Checks if the device has cellular data capabilities
     * 
     * @param context The application context
     * @return true if the device supports cellular data, false otherwise
     */
    private static boolean checkCellularSupport(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Use NetworkCapabilities for API 23+
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            }
        } else {
            // Deprecated approach for older devices
            return cm.getActiveNetworkInfo() != null &&
                   cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_MOBILE;
        }
        
        return false;
    }
}
