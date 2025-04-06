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
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.multiwifi.connector.DashboardActivity;
import com.multiwifi.connector.R;
import com.multiwifi.connector.implementation.HybridImplementation;
import com.multiwifi.connector.implementation.MultiWifiImplementation;
import com.multiwifi.connector.implementation.NativeMultiWifiImplementation;
import com.multiwifi.connector.implementation.ProxyImplementation;
import com.multiwifi.connector.implementation.UsbWifiImplementation;
import com.multiwifi.connector.implementation.VpnImplementation;
import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.DeviceCapabilities;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.util.DeviceCapabilityDetector;
import com.multiwifi.connector.util.LoadBalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Service to manage multi-WiFi connections in the background
 */
public class MultiWifiService extends Service {
    private static final String TAG = "MultiWifiService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "MultiWifiServiceChannel";
    private static final long UPDATE_INTERVAL = 5000; // 5 seconds
    
    private final IBinder binder = new LocalBinder();
    private MultiWifiImplementation implementation;
    private DeviceCapabilities capabilities;
    private ConnectionMethod currentMethod;
    private boolean isConnected = false;
    private List<NetworkConnection> connectedNetworks = new ArrayList<>();
    private final List<ConnectionListener> listeners = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Timer updateTimer;
    private LoadBalancer loadBalancer;
    
    /**
     * Interface for connection status listeners
     */
    public interface ConnectionListener {
        void onConnectionStatusChanged(boolean isConnected);
        void onNetworksUpdated(List<NetworkConnection> networks);
        void onCombinedSpeedChanged(double speedMbps);
    }
    
    /**
     * Binder for clients to access the service
     */
    public class LocalBinder extends Binder {
        public MultiWifiService getService() {
            return MultiWifiService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize load balancer
        loadBalancer = new LoadBalancer();
        
        // Detect device capabilities
        capabilities = DeviceCapabilityDetector.detectCapabilities(this);
        
        // Initialize the appropriate implementation based on capabilities
        initializeImplementation(capabilities.getRecommendedMethod());
        
        // Start update timer
        startUpdateTimer();
        
        Log.d(TAG, "MultiWifi service created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MultiWifi service started");
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "MultiWifi service destroyed");
        stopUpdateTimer();
        
        if (implementation != null) {
            implementation.cleanup();
            implementation = null;
        }
        
        super.onDestroy();
    }
    
    /**
     * Initializes the appropriate implementation based on the connection method
     * 
     * @param method The connection method to use
     */
    public void initializeImplementation(ConnectionMethod method) {
        // Clean up existing implementation if any
        if (implementation != null) {
            implementation.cleanup();
        }
        
        // Create new implementation based on method
        currentMethod = method;
        switch (method) {
            case NATIVE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    implementation = new NativeMultiWifiImplementation();
                } else {
                    // Fallback to VPN if native is not supported
                    implementation = new VpnImplementation(this);
                    currentMethod = ConnectionMethod.VPN;
                }
                break;
                
            case USB_ADAPTER:
                implementation = new UsbWifiImplementation();
                break;
                
            case HYBRID:
                implementation = new HybridImplementation();
                break;
                
            case VPN:
                implementation = new VpnImplementation(this);
                break;
                
            case PROXY:
            default:
                // Try VPN first, then fall back to PROXY if needed
                implementation = new VpnImplementation(this);
                currentMethod = ConnectionMethod.VPN;
                // If VPN initialization fails, we'll fall back to PROXY in the code below
                break;
        }
        
        // Initialize the implementation
        boolean initSuccess = implementation.initialize(this);
        
        if (!initSuccess) {
            Log.e(TAG, "Failed to initialize " + method + " implementation");
            
            // Fallback to proxy if initialization fails and we're not already using proxy
            if (currentMethod != ConnectionMethod.PROXY) {
                // If we tried VPN and it failed, fall back to PROXY
                if (currentMethod == ConnectionMethod.VPN) {
                    Log.d(TAG, "VPN initialization failed, falling back to PROXY");
                    implementation = new ProxyImplementation();
                    currentMethod = ConnectionMethod.PROXY;
                    initSuccess = implementation.initialize(this);
                    
                    if (!initSuccess) {
                        Log.e(TAG, "Failed to initialize PROXY implementation as fallback");
                    }
                } else {
                    // For other methods, try VPN first as fallback
                    Log.d(TAG, "Initialization failed, trying VPN as fallback");
                    implementation = new VpnImplementation(this);
                    currentMethod = ConnectionMethod.VPN;
                    initSuccess = implementation.initialize(this);
                    
                    if (!initSuccess) {
                        // If VPN also fails, fall back to PROXY as last resort
                        Log.d(TAG, "VPN initialization failed, falling back to PROXY");
                        implementation = new ProxyImplementation();
                        currentMethod = ConnectionMethod.PROXY;
                        initSuccess = implementation.initialize(this);
                        
                        if (!initSuccess) {
                            Log.e(TAG, "Failed to initialize PROXY implementation as last resort");
                        }
                    }
                }
            }
        } else {
            Log.d(TAG, "Initialized " + method + " implementation");
        }
    }
    
    /**
     * Scans for available networks
     * 
     * @return List of available networks
     */
    public List<NetworkConnection> scanNetworks() {
        if (implementation == null) {
            return new ArrayList<>();
        }
        
        return implementation.scanNetworks();
    }
    
    /**
     * Connects to the given networks
     * 
     * @param networks List of networks to connect to
     * @return true if connection process was initiated successfully
     */
    public boolean connectToNetworks(List<NetworkConnection> networks) {
        if (implementation == null) {
            return false;
        }
        
        boolean success = implementation.connectToNetworks(networks);
        if (success) {
            updateConnectionStatus();
        }
        
        return success;
    }
    
    /**
     * Disconnects from all networks
     * 
     * @return true if disconnection was successful
     */
    public boolean disconnectAll() {
        if (implementation == null) {
            return false;
        }
        
        boolean success = implementation.disconnectAll();
        if (success) {
            updateConnectionStatus();
        }
        
        return success;
    }
    
    /**
     * Gets the combined speed of all connected networks
     * 
     * @return Combined speed in Mbps
     */
    public double getCombinedSpeed() {
        if (implementation == null || !isConnected) {
            return 0.0;
        }
        
        return implementation.getCombinedSpeed();
    }
    
    /**
     * Checks if the service is connected to any networks
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Gets the current connection method
     * 
     * @return Current connection method
     */
    /**
    * Gets the current implementation
    * 
    * @return Current implementation
    */
    public MultiWifiImplementation getImplementation() {
        return implementation;
    }

    public ConnectionMethod getCurrentMethod() {
        return currentMethod;
    }
    
    /**
     * Gets the device capabilities
     * 
     * @return Device capabilities
     */
    public DeviceCapabilities getCapabilities() {
        return capabilities;
    }
    
    /**
     * Gets currently connected networks
     * 
     * @return List of connected networks
     */
    public List<NetworkConnection> getConnectedNetworks() {
        if (implementation == null) {
            return new ArrayList<>();
        }
        
        return implementation.getConnectedNetworks();
    }
    
    /**
     * Updates the allocation percentages for the networks
     * 
     * @param networks List of networks with updated allocation percentages
     */
    public void updateAllocation(List<NetworkConnection> networks) {
        if (implementation == null) {
            return;
        }
        
        implementation.updateAllocation(networks);
    }
    
    /**
     * Sets the load balancing strategy
     * 
     * @param strategy The strategy to use
     */
    public void setLoadBalancingStrategy(LoadBalancer.Strategy strategy) {
        if (loadBalancer != null) {
            loadBalancer.setStrategy(strategy);
            
            // Recalculate allocation with new strategy
            if (isConnected && implementation != null) {
                List<NetworkConnection> networks = implementation.getConnectedNetworks();
                loadBalancer.computeAllocation(networks);
                implementation.updateAllocation(networks);
                
                // Notify listeners
                for (ConnectionListener listener : listeners) {
                    listener.onNetworksUpdated(networks);
                }
            }
        }
    }
    
    /**
     * Gets the current load balancing strategy
     * 
     * @return Current strategy
     */
    public LoadBalancer.Strategy getLoadBalancingStrategy() {
        if (loadBalancer != null) {
            return loadBalancer.getStrategy();
        }
        
        return LoadBalancer.Strategy.ADAPTIVE;
    }
    
    /**
     * Registers a connection listener
     * 
     * @param listener The listener to register
     */
    public void registerListener(ConnectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Unregisters a connection listener
     * 
     * @param listener The listener to unregister
     */
    public void unregisterListener(ConnectionListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Creates the notification for foreground service
     * 
     * @return The notification
     */
    private Notification createNotification() {
        createNotificationChannel();
        
        // Create an intent for the dashboard activity
        Intent intent = new Intent(this, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(isConnected ? 
                        getString(R.string.status_connected) : 
                        getString(R.string.status_disconnected))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        
        return builder.build();
    }
    
    /**
     * Creates the notification channel for foreground service
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Multi-WiFi Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Updates the notification with current connection status
     */
    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification());
        }
    }
    
    /**
     * Starts the update timer to periodically update connection status
     */
    private void startUpdateTimer() {
        stopUpdateTimer(); // Ensure no existing timer
        
        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateConnectionStatus();
            }
        }, 0, UPDATE_INTERVAL);
    }
    
    /**
     * Stops the update timer
     */
    private void stopUpdateTimer() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }
    
    /**
     * Updates the connection status and notifies listeners
     */
    private void updateConnectionStatus() {
        if (implementation == null) {
            return;
        }
        
        // Update connection status
        final boolean newConnected = implementation.isConnected();
        final List<NetworkConnection> newNetworks = implementation.getConnectedNetworks();
        final double newSpeed = implementation.getCombinedSpeed();
        
        // Calculate allocation if connected
        if (newConnected && loadBalancer != null) {
            loadBalancer.computeAllocation(newNetworks);
            implementation.updateAllocation(newNetworks);
        }
        
        // Update notification on main thread
        mainHandler.post(() -> {
            boolean statusChanged = (isConnected != newConnected);
            isConnected = newConnected;
            connectedNetworks = new ArrayList<>(newNetworks);
            
            // Update notification
            updateNotification();
            
            // Notify listeners
            for (ConnectionListener listener : listeners) {
                if (statusChanged) {
                    listener.onConnectionStatusChanged(isConnected);
                }
                
                listener.onNetworksUpdated(connectedNetworks);
                listener.onCombinedSpeedChanged(newSpeed);
            }
        });
    }
}
