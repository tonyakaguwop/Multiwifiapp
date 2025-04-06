package com.multiwifi.connector;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.multiwifi.connector.adapter.NetworksAdapter;
import com.multiwifi.connector.implementation.VpnImplementation;
import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.DeviceCapabilities;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.service.MultiWifiService;
import com.multiwifi.connector.util.LoadBalancer;
import com.multiwifi.connector.viewmodel.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard activity showing network connections and status
 */
public class DashboardActivity extends AppCompatActivity implements 
        NetworksAdapter.OnNetworkClickListener, MultiWifiService.ConnectionListener {
    
    private DeviceCapabilities capabilities;
    private DashboardViewModel viewModel;
    private MultiWifiService wifiService;
    private boolean isBound = false;
    
    // UI elements
    private TextView statusText;
    private TextView combinedSpeedValue;
    private Button connectionButton;
    private RecyclerView networksRecyclerView;
    private FloatingActionButton fabAddNetwork;
    private NetworksAdapter networksAdapter;
    
    // Activity result launcher for VPN permission
    private final ActivityResultLauncher<Intent> vpnPermissionLauncher = 
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), 
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // VPN permission granted
                    if (wifiService != null && wifiService.getCurrentMethod() == ConnectionMethod.VPN) {
                        // Get the implementation and set permission granted
                        Object impl = wifiService.getImplementation();
                        if (impl instanceof VpnImplementation) {
                            VpnImplementation vpnImpl = (VpnImplementation) impl;
                            vpnImpl.onVpnPermissionResult(true);
                            
                            // Connect to saved networks if any
                            connectToSavedNetworks();
                        }
                    }
                } else {
                    // VPN permission denied
                    Toast.makeText(this, "VPN permission denied. Multi-WiFi VPN mode cannot function without this permission.", 
                            Toast.LENGTH_LONG).show();
                    
                    if (wifiService != null && wifiService.getCurrentMethod() == ConnectionMethod.VPN) {
                        // Get the implementation and set permission denied
                        Object impl = wifiService.getImplementation();
                        if (impl instanceof VpnImplementation) {
                            VpnImplementation vpnImpl = (VpnImplementation) impl;
                            vpnImpl.onVpnPermissionResult(false);
                        }
                    }
                }
            });
    
    /**
     * Service connection for binding to MultiWifiService
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MultiWifiService.LocalBinder localBinder = (MultiWifiService.LocalBinder) binder;
            wifiService = localBinder.getService();
            isBound = true;
            
            // Register as listener
            wifiService.registerListener(DashboardActivity.this);
            
            // Check if we need to request VPN permission
            checkVpnPermission();
            
            // Update UI
            updateConnectionStatus(wifiService.isConnected());
            updateNetworks(wifiService.getConnectedNetworks());
            updateCombinedSpeed(wifiService.getCombinedSpeed());
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            wifiService = null;
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        // Get device capabilities from intent
        if (getIntent().hasExtra("capabilities")) {
            capabilities = (DeviceCapabilities) getIntent().getSerializableExtra("capabilities");
        } else {
            // Default capabilities if not provided
            capabilities = new DeviceCapabilities();
        }
        
        // Initialize view model
        viewModel = new DashboardViewModel();
        
        // Setup UI
        setupUI();
        
        // Start and bind to the service
        startAndBindService();
    }
    
    @Override
    protected void onDestroy() {
        // Unbind from service
        if (isBound && wifiService != null) {
            wifiService.unregisterListener(this);
            unbindService(serviceConnection);
            isBound = false;
        }
        
        super.onDestroy();
    }
    
    /**
     * Sets up the user interface
     */
    private void setupUI() {
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Initialize views
        statusText = findViewById(R.id.status_text);
        combinedSpeedValue = findViewById(R.id.combined_speed_value);
        connectionButton = findViewById(R.id.connection_button);
        networksRecyclerView = findViewById(R.id.networks_recyclerview);
        fabAddNetwork = findViewById(R.id.fab_add_network);
        
        // Setup RecyclerView
        networksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        networksAdapter = new NetworksAdapter(this, this);
        networksRecyclerView.setAdapter(networksAdapter);
        
        // Setup connection button
        connectionButton.setOnClickListener(v -> toggleConnection());
        
        // Setup add network button
        fabAddNetwork.setOnClickListener(v -> scanAndAddNetworks());
    }
    
    /**
     * Starts the service and binds to it
     */
    private void startAndBindService() {
        // Start service
        Intent serviceIntent = new Intent(this, MultiWifiService.class);
        startService(serviceIntent);
        
        // Bind to service
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    /**
     * Checks if VPN permission is needed and requests it if necessary
     */
    private void checkVpnPermission() {
        if (wifiService == null || !isBound) {
            return;
        }
        
        // If we're using VPN implementation, request permission
        if (wifiService.getCurrentMethod() == ConnectionMethod.VPN) {
            Object impl = wifiService.getImplementation();
            if (impl instanceof VpnImplementation) {
                VpnImplementation vpnImpl = (VpnImplementation) impl;
                
                // Request VPN permission if needed
                if (vpnImpl.requestVpnPermission(vpnPermissionLauncher)) {
                    // Permission request launched, wait for result
                    Toast.makeText(this, "Please grant VPN permission to enable multi-WiFi functionality", 
                            Toast.LENGTH_LONG).show();
                } else {
                    // Permission already granted
                    vpnImpl.onVpnPermissionResult(true);
                }
            }
        }
    }
    
    /**
     * Connects to saved networks
     */
    private void connectToSavedNetworks() {
        List<NetworkConnection> savedNetworks = viewModel.getSavedNetworks();
        if (savedNetworks != null && !savedNetworks.isEmpty() && 
                wifiService != null && !wifiService.isConnected()) {
            wifiService.connectToNetworks(savedNetworks);
        }
    }
    
    /**
     * Toggles the connection state
     */
    private void toggleConnection() {
        if (wifiService == null || !isBound) {
            return;
        }
        
        if (wifiService.isConnected()) {
            // Disconnect
            wifiService.disconnectAll();
        } else {
            // Connect to saved networks or scan for new ones
            List<NetworkConnection> savedNetworks = viewModel.getSavedNetworks();
            if (savedNetworks != null && !savedNetworks.isEmpty()) {
                // Check VPN permission first if needed
                if (wifiService.getCurrentMethod() == ConnectionMethod.VPN) {
                    checkVpnPermission();
                } else {
                    wifiService.connectToNetworks(savedNetworks);
                }
            } else {
                // Ask user if they want to use the recommendation wizard
                new AlertDialog.Builder(this)
                        .setTitle("Network Selection")
                        .setMessage("Would you like to use the Smart Recommendation Wizard to find the best network configuration?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            // Launch recommendation wizard
                            startActivity(new Intent(DashboardActivity.this, NetworkRecommendationActivity.class));
                        })
                        .setNegativeButton("No, Manual Selection", (dialog, which) -> {
                            // Use manual selection
                            scanAndAddNetworks();
                        })
                        .show();
            }
        }
    }
    
    /**
     * Scans for networks and shows a dialog to add them
     */
    private void scanAndAddNetworks() {
        if (wifiService == null || !isBound) {
            return;
        }
        
        Toast.makeText(this, R.string.scanning, Toast.LENGTH_SHORT).show();
        
        // Scan for networks
        List<NetworkConnection> availableNetworks = wifiService.scanNetworks();
        
        if (availableNetworks.isEmpty()) {
            Toast.makeText(this, "No networks found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create items for dialog
        CharSequence[] items = new CharSequence[availableNetworks.size()];
        boolean[] checkedItems = new boolean[availableNetworks.size()];
        
        for (int i = 0; i < availableNetworks.size(); i++) {
            items[i] = availableNetworks.get(i).getSsid();
            checkedItems[i] = false;
        }
        
        // Show dialog to select networks
        new AlertDialog.Builder(this)
                .setTitle("Select Networks")
                .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Connect", (dialog, which) -> {
                    List<NetworkConnection> selectedNetworks = new ArrayList<>();
                    
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedNetworks.add(availableNetworks.get(i));
                        }
                    }
                    
                    if (!selectedNetworks.isEmpty()) {
                        // Save selected networks
                        viewModel.setSavedNetworks(selectedNetworks);
                        
                        // Check VPN permission first if needed
                        if (wifiService.getCurrentMethod() == ConnectionMethod.VPN) {
                            checkVpnPermission();
                        } else {
                            // Connect to selected networks
                            Toast.makeText(this, R.string.connecting, Toast.LENGTH_SHORT).show();
                            wifiService.connectToNetworks(selectedNetworks);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Shows load balancing settings dialog
     */
    private void showLoadBalancingSettings() {
        if (wifiService == null || !isBound) {
            return;
        }
        
        CharSequence[] items = {
                "Adaptive (Smart)",
                "Speed-Based",
                "Latency-Based",
                "Round Robin (Equal)"
        };
        
        // Get current strategy
        LoadBalancer.Strategy currentStrategy = wifiService.getLoadBalancingStrategy();
        int checkedItem = 0;
        
        switch (currentStrategy) {
            case SPEED_BASED:
                checkedItem = 1;
                break;
            case LATENCY_BASED:
                checkedItem = 2;
                break;
            case ROUND_ROBIN:
                checkedItem = 3;
                break;
            case ADAPTIVE:
            default:
                checkedItem = 0;
                break;
        }
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.load_balancing)
                .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                    LoadBalancer.Strategy strategy;
                    
                    switch (which) {
                        case 1:
                            strategy = LoadBalancer.Strategy.SPEED_BASED;
                            break;
                        case 2:
                            strategy = LoadBalancer.Strategy.LATENCY_BASED;
                            break;
                        case 3:
                            strategy = LoadBalancer.Strategy.ROUND_ROBIN;
                            break;
                        case 0:
                        default:
                            strategy = LoadBalancer.Strategy.ADAPTIVE;
                            break;
                    }
                    
                    wifiService.setLoadBalancingStrategy(strategy);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_recommend) {
            // Launch recommendation wizard
            startActivity(new Intent(this, NetworkRecommendationActivity.class));
            return true;
        } else if (id == R.id.action_visualize) {
            // Launch network visualizer
            startActivity(new Intent(this, NetworkVisualizerActivity.class));
            return true;
        } else if (id == R.id.action_switch) {
            // Launch network switch wizard
            startActivity(new Intent(this, NetworkSwitchWizardActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            showLoadBalancingSettings();
            return true;
        } else if (id == R.id.action_help) {
            // Show help dialog
            new AlertDialog.Builder(this)
                    .setTitle("Help")
                    .setMessage("This app allows you to combine multiple WiFi networks to optimize connection speed and reliability.\n\n" +
                            "• Tap the + button to scan for and add networks\n" +
                            "• Use Smart Recommendation to find optimal configuration\n" +
                            "• View Connection Visualizer to see performance metrics\n" +
                            "• Use Network Switching to change connection methods\n" +
                            "• The app intelligently balances traffic across all connected networks")
                    .setPositiveButton("OK", null)
                    .show();
            return true;
        } else if (id == R.id.action_about) {
            // Show about dialog with connection method info
            String methodInfo = "";
            if (wifiService != null) {
                ConnectionMethod method = wifiService.getCurrentMethod();
                String methodDesc = "";
                
                switch (method) {
                    case NATIVE:
                        methodDesc = "Native multi-WiFi (Android 12+)";
                        break;
                    case USB_ADAPTER:
                        methodDesc = "USB WiFi adapter";
                        break;
                    case HYBRID:
                        methodDesc = "WiFi+Cellular hybrid";
                        break;
                    case VPN:
                        methodDesc = "VPN-based (non-root)";
                        break;
                    case PROXY:
                        methodDesc = "Proxy-based";
                        break;
                }
                
                methodInfo = "\n\nConnection Method: " + methodDesc;
            }
            
            new AlertDialog.Builder(this)
                    .setTitle("About")
                    .setMessage("Multi-WiFi Connector\nVersion 1.0\n\n" +
                            "An advanced utility that intelligently combines multiple WiFi networks " +
                            "to optimize connection speed and reliability." + methodInfo)
                    .setPositiveButton("OK", null)
                    .show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onNetworkClick(NetworkConnection network) {
        // Show network details
        new AlertDialog.Builder(this)
                .setTitle(network.getSsid())
                .setMessage("BSSID: " + network.getBssid() + "\n" +
                        "Speed: " + String.format("%.1f Mbps", network.getSpeedMbps()) + "\n" +
                        "Latency: " + network.getLatencyMs() + " ms\n" +
                        "Traffic Allocation: " + String.format("%.0f%%", network.getAllocationPercentage()) + "\n" +
                        "Connection Method: " + network.getConnectionMethod())
                .setPositiveButton("OK", null)
                .show();
    }
    
    // MultiWifiService.ConnectionListener methods
    
    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        updateConnectionStatus(isConnected);
    }
    
    @Override
    public void onNetworksUpdated(List<NetworkConnection> networks) {
        updateNetworks(networks);
    }
    
    @Override
    public void onCombinedSpeedChanged(double speedMbps) {
        updateCombinedSpeed(speedMbps);
    }
    
    /**
     * Updates the connection status in the UI
     * 
     * @param isConnected Connection status
     */
    private void updateConnectionStatus(boolean isConnected) {
        if (isConnected) {
            statusText.setText(R.string.status_connected);
            statusText.setTextColor(getResources().getColor(R.color.colorConnected));
            connectionButton.setText(R.string.disconnect);
        } else {
            statusText.setText(R.string.status_disconnected);
            statusText.setTextColor(getResources().getColor(R.color.colorDisconnected));
            connectionButton.setText(R.string.connect);
        }
    }
    
    /**
     * Updates the networks list in the UI
     * 
     * @param networks List of networks
     */
    private void updateNetworks(List<NetworkConnection> networks) {
        networksAdapter.updateNetworks(networks);
    }
    
    /**
     * Updates the combined speed in the UI
     * 
     * @param speedMbps Combined speed in Mbps
     */
    private void updateCombinedSpeed(double speedMbps) {
        combinedSpeedValue.setText(String.format("%.1f Mbps", speedMbps));
    }
}
