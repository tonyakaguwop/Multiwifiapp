package com.multiwifi.connector.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.DeviceCapabilities;

public class DeviceCapabilityDetector {
    
    private final Context context;
    private DeviceCapabilities deviceCapabilities;
    
    public DeviceCapabilityDetector(Context context) {
        this.context = context;
        this.deviceCapabilities = new DeviceCapabilities();
    }
    
    public void detectCapabilities() {
        // Check for native multi-WiFi support (Android 12+)
        checkNativeMultiWifiSupport();
        
        // Check for USB OTG support
        checkUsbOtgSupport();
        
        // Check for cellular data availability
        checkCellularDataAvailability();
        
        // Determine the best connection method
        determineBestConnectionMethod();
    }
    
    private void checkNativeMultiWifiSupport() {
        // Native multi-WiFi is available on Android 12 (API 31) and above
        boolean hasNativeSupport = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
        deviceCapabilities.setSupportsNativeMultiWifi(hasNativeSupport);
    }
    
    private void checkUsbOtgSupport() {
        // Check if device has USB host support (OTG)
        boolean hasUsbOtg = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST);
        deviceCapabilities.setSupportsUsbAdapter(hasUsbOtg);
    }
    
    private void checkCellularDataAvailability() {
        // Check if device has cellular connectivity
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean hasCellular = false;
        
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(
                        connectivityManager.getActiveNetwork());
                
                if (capabilities != null) {
                    hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
                }
            } else {
                // Legacy method for older Android versions
                hasCellular = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) != null;
            }
        }
        
        deviceCapabilities.setSupportsCellularData(hasCellular);
    }
    
    private void determineBestConnectionMethod() {
        // Determine the best connection method based on device capabilities
        ConnectionMethod bestMethod;
        
        if (deviceCapabilities.isSupportsNativeMultiWifi()) {
            // If device supports native multi-WiFi, use it
            bestMethod = ConnectionMethod.NATIVE;
        } else if (deviceCapabilities.isSupportsUsbAdapter()) {
            // If device supports USB OTG, use USB adapter method
            bestMethod = ConnectionMethod.USB_ADAPTER;
        } else if (deviceCapabilities.isSupportsCellularData()) {
            // If device has cellular data, use WiFi + Cellular hybrid
            bestMethod = ConnectionMethod.HYBRID;
        } else {
            // Fallback to proxy method
            bestMethod = ConnectionMethod.PROXY;
        }
        
        deviceCapabilities.setRecommendedMethod(bestMethod);
    }
    
    public DeviceCapabilities getDeviceCapabilities() {
        return deviceCapabilities;
    }
}
