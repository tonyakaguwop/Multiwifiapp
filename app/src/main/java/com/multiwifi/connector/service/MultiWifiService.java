package com.multiwifi.connector.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import com.multiwifi.connector.DashboardActivity;
import com.multiwifi.connector.R;
import com.multiwifi.connector.implementation.HybridImplementation;
import com.multiwifi.connector.implementation.MultiWifiImplementation;
import com.multiwifi.connector.implementation.NativeMultiWifiImplementation;
import com.multiwifi.connector.implementation.ProxyImplementation;
import com.multiwifi.connector.implementation.UsbWifiImplementation;
import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.DeviceCapabilities;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.util.DeviceCapabilityDetector;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing multi-WiFi connections
 */
public class MultiWifiService extends Service {
    
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "multi_wifi_channel";
    private static final long UPDATE_INTERVAL = 5000; // 5 seconds
    
    private final IBinder binder = new LocalBinder();
    private DeviceCapabilityDetector capabilityDetector;
    private MultiWifiImplementation implementation;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private boolean isConnected = false;
    private List<NetworkConnection> connectedNetworks = new ArrayList<>();
    private float combinedSpeed = 0;
    
    public class LocalBinder extends Binder {
        public MultiWifiService getService() {
            return MultiWifiService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize capability detector
        capabilityDetector = new DeviceCapabilityDetector(this);
        
        // Initialize update handler
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = this::updateNetworkMetrics;
        
        // Create notification channel
        createNotificationChannel();
        
        // Initialize the appropriate implementation
        initializeImplementation();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as a foreground service
        startForeground(NOTIFICATION_ID, createNotification("Multi-WiFi service running"));
        
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        // Cleanup implementation
        if (implementation != null) {
            implementation.cleanup();
        }
        
        // Remove update callbacks
        updateHandler.removeCallbacks(updateRunnable);
        
        super.onDestroy();
    }
    
    /**
     * Initialize the appropriate implementation based on device capabilities
     */
    private void initializeImplementation() {
        // Detect device capabilities
        capabilityDetector.detectCapabilities();
        DeviceCapabilities capabilities = capabilityDetector.getDeviceCapabilities();
        
        // Create the appropriate implementation
        ConnectionMethod method = capabilities.getRecommendedMethod();
        
        switch (method) {
            case NATIVE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    implementation = new NativeMultiWifiImplementation(this);
                } else {
                    // Fallback to proxy if native is not available
                    implementation = new ProxyImplementation(this);
                }
                break;
                
            case USB_ADAPTER:
                implementation = new UsbWifiImplementation(this);
                break;
                
            case HYBRID:
                implementation = new HybridImplementation(this);
                break;
                
            case PROXY:
            default:
                implementation = new ProxyImplementation(this);
                break;
        }
        
        // Initialize the implementation
        if (implementation != null) {
            implementation.initialize();
        }
    }
    
    /**
     * Connect to multiple networks
     */
    public boolean connect() {
        if (implementation == null) {
            return false;
        }
        
        boolean connected = implementation.connect();
        
        if (connected) {
            isConnected = true;
            connectedNetworks = implementation.getConnectedNetworks();
            combinedSpeed = implementation.getCombinedSpeed();
            
            // Start periodic updates
            startPeriodicUpdates();
            
            // Update notification
            updateNotification();
        }
        
        return connected;
    }
    
    /**
     * Disconnect from all networks
     */
    public boolean disconnect() {
        if (implementation == null) {
            return false;
        }
        
        boolean disconnected = implementation.disconnect();
        
        if (disconnected) {
            isConnected = false;
            connectedNetworks.clear();
            combinedSpeed = 0;
            
            // Stop periodic updates
            stopPeriodicUpdates();
            
            // Update notification
            updateNotification();
        }
        
        return disconnected;
    }
    
    /**
     * Check if connected
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Get list of connected networks
     */
    public List<NetworkConnection> getConnectedNetworks() {
        return connectedNetworks;
    }
    
    /**
     * Get combined connection speed
     */
    public float getCombinedSpeed() {
        return combinedSpeed;
    }
    
    /**
     * Scan for available networks
     */
    public List<NetworkConnection> scanNetworks() {
        if (implementation == null) {
            return new ArrayList<>();
        }
        
        return implementation.scanNetworks();
    }
    
    /**
     * Add a specific network
     */
    public boolean addNetwork(String ssid, String password) {
        if (implementation == null) {
            return false;
        }
        
        boolean added = implementation.addNetwork(ssid, password);
        
        if (added) {
            isConnected = implementation.isConnected();
            connectedNetworks = implementation.getConnectedNetworks();
            combinedSpeed = implementation.getCombinedSpeed();
            
            // Start periodic updates if newly connected
            if (isConnected && connectedNetworks.size() == 1) {
                startPeriodicUpdates();
            }
            
            // Update notification
            updateNotification();
        }
        
        return added;
    }
    
    /**
     * Remove a specific network
     */
    public boolean removeNetwork(String ssid) {
        if (implementation == null) {
            return false;
        }
        
        boolean removed = implementation.removeNetwork(ssid);
        
        if (removed) {
            isConnected = implementation.isConnected();
            connectedNetworks = implementation.getConnectedNetworks();
            combinedSpeed = implementation.getCombinedSpeed();
            
            // Stop periodic updates if disconnected
            if (!isConnected) {
                stopPeriodicUpdates();
            }
            
            // Update notification
            updateNotification();
        }
        
        return removed;
    }
    
    /**
     * Start periodic network metric updates
     */
    private void startPeriodicUpdates() {
        updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL);
    }
    
    /**
     * Stop periodic network metric updates
     */
    private void stopPeriodicUpdates() {
        updateHandler.removeCallbacks(updateRunnable);
    }
    
    /**
     * Update network metrics
     */
    private void updateNetworkMetrics() {
        if (implementation != null && isConnected) {
            // Update metrics
            implementation.updateMetrics();
            
            // Update local variables
            connectedNetworks = implementation.getConnectedNetworks();
            combinedSpeed = implementation.getCombinedSpeed();
            
            // Update notification
            updateNotification();
            
            // Schedule next update
            updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL);
        }
    }
    
    /**
     * Create notification channel (required for Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Multi-WiFi Service";
            String description = "Notifications for the Multi-WiFi Connector service";
            int importance = NotificationManager.IMPORTANCE_LOW;
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Create and return a notification
     */
    private Notification createNotification(String contentText) {
        // Create an intent to open the app when notification is tapped
        Intent notificationIntent = new Intent(this, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Multi-WiFi Connector")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent);
        
        return builder.build();
    }
    
    /**
     * Update the service notification
     */
    private void updateNotification() {
        String content;
        
        if (isConnected) {
            int networkCount = connectedNetworks.size();
            content = "Connected to " + networkCount + " network" +
                    (networkCount > 1 ? "s" : "") +
                    " (" + String.format("%.1f", combinedSpeed) + " Mbps)";
        } else {
            content = "Not connected";
        }
        
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(content));
        }
    }
}
