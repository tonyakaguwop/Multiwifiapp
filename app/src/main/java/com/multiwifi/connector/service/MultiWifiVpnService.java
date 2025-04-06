package com.multiwifi.connector.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.multiwifi.connector.DashboardActivity;
import com.multiwifi.connector.R;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.util.LoadBalancer;
import com.multiwifi.connector.util.NetworkUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * VPN Service implementation for routing traffic through multiple network interfaces.
 * This service uses Android's VPN API to capture all network traffic and then route it
 * intelligently through available network connections based on their performance characteristics.
 */
public class MultiWifiVpnService extends VpnService implements Handler.Callback {
    private static final String TAG = "MultiWifiVpnService";
    private static final String CHANNEL_ID = "multi_wifi_vpn_channel";
    private static final int NOTIFICATION_ID = 1338;
    private static final int MTU = 1500;
    private static final int MAX_PACKET_SIZE = 4096;
    
    private Handler handler;
    private ParcelFileDescriptor vpnInterface;
    private ExecutorService executorService;
    private LoadBalancer loadBalancer;
    private ConcurrentHashMap<String, ConnectionTunnel> tunnels;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    
    private List<NetworkConnection> availableNetworks = Collections.synchronizedList(new ArrayList<>());
    
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(this);
        tunnels = new ConcurrentHashMap<>();
        loadBalancer = new LoadBalancer();
        executorService = Executors.newFixedThreadPool(5); // Main thread + up to 4 network threads
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isRunning.get()) {
            Log.d(TAG, "Service already running");
            return START_STICKY;
        }
        
        // Start as a foreground service to maintain connectivity
        startForeground(NOTIFICATION_ID, buildNotification("Initializing Multi-WiFi VPN..."));
        
        // Start the VPN service
        startVpnService();
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        isRunning.set(false);
        closeVpnInterface();
        closeAllTunnels();
        executorService.shutdownNow();
        super.onDestroy();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public boolean handleMessage(Message message) {
        // Handle messages from worker threads
        return true;
    }
    
    /**
     * Creates the VPN interface and starts routing traffic
     */
    private void startVpnService() {
        // Set up the VPN interface
        try {
            // Configure VPN interface
            Builder builder = new Builder()
                    .setSession("MultiWifiVPN")
                    .addAddress("10.0.0.2", 24)
                    .addRoute("0.0.0.0", 0)  // Capture all traffic
                    .addDnsServer("8.8.8.8") // Use Google DNS
                    .setMtu(MTU);
            
            // Exclude the app itself from the VPN
            try {
                builder.addDisallowedApplication(getPackageName());
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Failed to exclude app from VPN", e);
            }
            
            // Create the VPN interface
            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface");
                stopSelf();
                return;
            }
            
            Log.d(TAG, "VPN interface established");
            isRunning.set(true);
            
            // Start the packet handling thread
            executorService.submit(new VpnRunnable(vpnInterface.getFileDescriptor()));
            
            // Update the notification
            updateNotification("Multi-WiFi VPN is active");
            
            // Initialize network connections
            initializeNetworkConnections();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting VPN service", e);
            isRunning.set(false);
            stopSelf();
        }
    }
    
    /**
     * Discover and set up available network connections
     */
    private void initializeNetworkConnections() {
        // Scan for available networks
        List<NetworkConnection> networks = NetworkUtils.getAvailableNetworks(this);
        
        if (networks.isEmpty()) {
            Log.w(TAG, "No networks available for VPN routing");
            updateNotification("No networks available");
            return;
        }
        
        availableNetworks.clear();
        availableNetworks.addAll(networks);
        
        // Initialize tunnels for each network
        for (NetworkConnection network : networks) {
            ConnectionTunnel tunnel = new ConnectionTunnel(network);
            tunnels.put(network.getSsid(), tunnel);
            executorService.submit(tunnel);
        }
        
        // Set up load balancer with the available networks
        loadBalancer.updateNetworks(networks);
        
        Log.d(TAG, "Initialized " + networks.size() + " network connections");
        updateNotification("Connected to " + networks.size() + " networks");
    }
    
    /**
     * Updates the active networks list and reconfigures tunnels
     * 
     * @param networks Updated list of network connections
     */
    public void updateNetworks(List<NetworkConnection> networks) {
        if (!isRunning.get()) {
            return;
        }
        
        Log.d(TAG, "Updating networks: " + networks.size() + " connections");
        
        // Close tunnels for networks that are no longer available
        for (String ssid : tunnels.keySet()) {
            boolean found = false;
            for (NetworkConnection network : networks) {
                if (network.getSsid().equals(ssid)) {
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                ConnectionTunnel tunnel = tunnels.remove(ssid);
                if (tunnel != null) {
                    tunnel.stop();
                }
            }
        }
        
        // Add new networks
        for (NetworkConnection network : networks) {
            if (!tunnels.containsKey(network.getSsid())) {
                ConnectionTunnel tunnel = new ConnectionTunnel(network);
                tunnels.put(network.getSsid(), tunnel);
                executorService.submit(tunnel);
            }
        }
        
        // Update available networks
        availableNetworks.clear();
        availableNetworks.addAll(networks);
        
        // Update load balancer
        loadBalancer.updateNetworks(networks);
        
        // Update notification
        updateNotification("Connected to " + networks.size() + " networks");
    }
    
    /**
     * Tunnel class for handling traffic through a specific network connection
     */
    private class ConnectionTunnel implements Runnable {
        private final NetworkConnection network;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private DatagramChannel channel;
        
        public ConnectionTunnel(NetworkConnection network) {
            this.network = network;
        }
        
        @Override
        public void run() {
            running.set(true);
            
            try {
                // Create a datagram channel
                channel = DatagramChannel.open();
                
                // Protect this socket from VPN to prevent loops
                protect(channel.socket());
                
                // Configure the channel
                channel.configureBlocking(true);
                
                // Buffer for receiving data
                ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKET_SIZE);
                
                while (running.get() && isRunning.get()) {
                    // Wait for data to be routed to this network
                    // The actual routing logic will be implemented in the VpnRunnable
                    Thread.sleep(100); // Prevent CPU spin, replace with actual logic
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in connection tunnel for " + network.getSsid(), e);
            } finally {
                close();
            }
        }
        
        public void stop() {
            running.set(false);
            close();
        }
        
        private void close() {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing channel", e);
                }
                channel = null;
            }
        }
    }
    
    /**
     * Runnable to handle traffic from the VPN interface
     */
    private class VpnRunnable implements Runnable {
        private final int fd;
        
        public VpnRunnable(int fd) {
            this.fd = fd;
        }
        
        @Override
        public void run() {
            try {
                FileInputStream in = new FileInputStream(fd);
                FileOutputStream out = new FileOutputStream(fd);
                
                ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_SIZE);
                
                while (isRunning.get()) {
                    // Clear the packet buffer
                    packet.clear();
                    
                    // Read from VPN interface
                    int length = in.read(packet.array());
                    if (length <= 0) {
                        Thread.sleep(100);
                        continue;
                    }
                    
                    // Set the buffer position and limit
                    packet.limit(length);
                    packet.position(0);
                    
                    // Analyze the packet (IP header, etc.) and determine routing
                    String destination = analyzePacket(packet);
                    
                    // Use load balancer to select the best network for this packet
                    NetworkConnection selectedNetwork = loadBalancer.selectNetworkForTraffic(destination);
                    
                    if (selectedNetwork != null) {
                        // Get the tunnel for this network
                        ConnectionTunnel tunnel = tunnels.get(selectedNetwork.getSsid());
                        
                        if (tunnel != null && tunnel.channel != null && tunnel.channel.isOpen()) {
                            // Forward packet to the selected network
                            // This is simplified - in a real implementation we would:
                            // 1. Route the packet to the correct network interface
                            // 2. Handle responses and route them back through the VPN
                            tunnel.channel.write(packet);
                        }
                    }
                    
                    // This is a simplified implementation
                    // In reality, we would need to:
                    // 1. Maintain connection state
                    // 2. Handle IP fragmentation
                    // 3. Process responses and write them back to the VPN interface
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in VPN runnable", e);
            } finally {
                isRunning.set(false);
            }
        }
        
        /**
         * Analyze a packet to determine routing information
         * 
         * @param packet The packet to analyze
         * @return A string representation of the destination (e.g., IP or hostname)
         */
        private String analyzePacket(ByteBuffer packet) {
            // This is a simplified implementation
            // In a real app, we would:
            // 1. Parse the IP header
            // 2. Determine protocol (TCP/UDP)
            // 3. Extract source/destination addresses and ports
            // 4. Handle DNS resolution if needed
            
            // For simplicity, we're just returning a placeholder
            return "destination";
        }
    }
    
    private void closeVpnInterface() {
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing VPN interface", e);
            }
            vpnInterface = null;
        }
    }
    
    private void closeAllTunnels() {
        for (ConnectionTunnel tunnel : tunnels.values()) {
            tunnel.stop();
        }
        tunnels.clear();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Multi-WiFi VPN Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Notifications for Multi-WiFi VPN service status");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification buildNotification(String content) {
        Intent notificationIntent = new Intent(this, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
                
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Multi-WiFi VPN")
                .setContentText(content)
                .setSmallIcon(R.drawable.combined_wifi)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    
    private void updateNotification(String content) {
        Notification notification = buildNotification(content);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }
}
