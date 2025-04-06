package com.multiwifi.connector;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.multiwifi.connector.adapter.NetworkSwitchPagerAdapter;
import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.DeviceCapabilities;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.model.SwitchOption;
import com.multiwifi.connector.service.MultiWifiService;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for guiding users through network connection method switching
 */
public class NetworkSwitchWizardActivity extends AppCompatActivity {

    private MultiWifiService wifiService;
    private boolean isBound = false;
    private DeviceCapabilities capabilities;
    
    // UI elements
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private Button nextButton;
    private Button previousButton;
    private TextView titleText;
    private ImageView methodImage;
    
    private NetworkSwitchPagerAdapter pagerAdapter;
    private List<SwitchOption> switchOptions;
    private int currentStep = 0;
    private ConnectionMethod selectedMethod;
    
    /**
     * Service connection for binding to MultiWifiService
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MultiWifiService.LocalBinder localBinder = (MultiWifiService.LocalBinder) binder;
            wifiService = localBinder.getService();
            isBound = true;
            
            // Initialize UI with current connection method
            if (wifiService != null) {
                selectedMethod = wifiService.getCurrentMethod();
                capabilities = wifiService.getCapabilities();
                
                // Generate available switch options based on device capabilities
                generateSwitchOptions();
                
                // Set up ViewPager adapter
                pagerAdapter = new NetworkSwitchPagerAdapter(
                        NetworkSwitchWizardActivity.this, switchOptions);
                viewPager.setAdapter(pagerAdapter);
                
                // Setup tab layout with ViewPager
                new TabLayoutMediator(tabLayout, viewPager,
                        (tab, position) -> {
                            // Tab titles based on position
                            switch (position) {
                                case 0:
                                    tab.setText("Method");
                                    break;
                                case 1:
                                    tab.setText("Networks");
                                    break;
                                case 2:
                                    tab.setText("Configuration");
                                    break;
                                case 3:
                                    tab.setText("Apply");
                                    break;
                            }
                        }).attach();
                
                // Update UI for initial state
                updateUI();
            }
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
        setContentView(R.layout.activity_network_switch_wizard);
        
        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.network_switch_title);
        }
        
        // Initialize UI elements
        setupUI();
        
        // Bind to the service
        Intent serviceIntent = new Intent(this, MultiWifiService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onDestroy() {
        // Unbind from service
        if (isBound && wifiService != null) {
            unbindService(serviceConnection);
            isBound = false;
        }
        
        super.onDestroy();
    }
    
    /**
     * Sets up the UI elements
     */
    private void setupUI() {
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);
        nextButton = findViewById(R.id.next_button);
        previousButton = findViewById(R.id.previous_button);
        titleText = findViewById(R.id.title_text);
        methodImage = findViewById(R.id.method_image);
        
        // Set up button click listeners
        nextButton.setOnClickListener(v -> moveToNextStep());
        previousButton.setOnClickListener(v -> moveToPreviousStep());
        
        // Disable swiping between pages (to enforce wizard flow)
        viewPager.setUserInputEnabled(false);
        
        // Set up ViewPager page change callback
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentStep = position;
                updateUI();
            }
        });
    }
    
    /**
     * Generates the list of connection method switch options
     * based on device capabilities
     */
    private void generateSwitchOptions() {
        switchOptions = new ArrayList<>();
        
        // Add options based on device capabilities
        if (capabilities.isNativeMultiWifiSupported()) {
            SwitchOption nativeOption = new SwitchOption();
            nativeOption.setMethod(ConnectionMethod.NATIVE);
            nativeOption.setTitle("Native Multi-WiFi");
            nativeOption.setDescription("Built-in Android 12+ multi-network support");
            nativeOption.setSelected(selectedMethod == ConnectionMethod.NATIVE);
            switchOptions.add(nativeOption);
        }
        
        if (capabilities.isUsbAdapterSupported()) {
            SwitchOption usbOption = new SwitchOption();
            usbOption.setMethod(ConnectionMethod.USB_ADAPTER);
            usbOption.setTitle("USB WiFi Adapter");
            usbOption.setDescription("Additional networks via USB adapter");
            usbOption.setSelected(selectedMethod == ConnectionMethod.USB_ADAPTER);
            switchOptions.add(usbOption);
        }
        
        if (capabilities.isHybridSupported()) {
            SwitchOption hybridOption = new SwitchOption();
            hybridOption.setMethod(ConnectionMethod.HYBRID);
            hybridOption.setTitle("WiFi + Cellular");
            hybridOption.setDescription("Combines WiFi and cellular data");
            hybridOption.setSelected(selectedMethod == ConnectionMethod.HYBRID);
            switchOptions.add(hybridOption);
        }
        
        if (capabilities.isVpnSupported()) {
            SwitchOption vpnOption = new SwitchOption();
            vpnOption.setMethod(ConnectionMethod.VPN);
            vpnOption.setTitle("VPN Routing");
            vpnOption.setDescription("Software-based multi-network (non-root)");
            vpnOption.setSelected(selectedMethod == ConnectionMethod.VPN);
            switchOptions.add(vpnOption);
        }
        
        // Always add proxy as fallback
        SwitchOption proxyOption = new SwitchOption();
        proxyOption.setMethod(ConnectionMethod.PROXY);
        proxyOption.setTitle("Proxy Mode");
        proxyOption.setDescription("Basic compatibility mode (all devices)");
        proxyOption.setSelected(selectedMethod == ConnectionMethod.PROXY);
        switchOptions.add(proxyOption);
    }
    
    /**
     * Applies the selected method and network configuration
     */
    private void applyChanges() {
        if (!isBound || wifiService == null) {
            Toast.makeText(this, "Service not connected, please try again later", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get the selected method
        ConnectionMethod method = pagerAdapter.getSelectedMethod();
        
        // Get the selected networks
        List<NetworkConnection> networks = pagerAdapter.getSelectedNetworks();
        
        // Get configuration options
        boolean autoReconnect = pagerAdapter.isAutoReconnectEnabled();
        boolean backgroundScan = pagerAdapter.isBackgroundScanEnabled();
        
        // Apply changes
        boolean success = true;
        
        // First disconnect from current networks if connected
        if (wifiService.isConnected()) {
            success = wifiService.disconnectAll();
        }
        
        // Initialize the new implementation if needed
        if (success && method != wifiService.getCurrentMethod()) {
            wifiService.initializeImplementation(method);
        }
        
        // Connect to the selected networks
        if (success && !networks.isEmpty()) {
            success = wifiService.connectToNetworks(networks);
        }
        
        // Show result toast
        if (success) {
            Toast.makeText(this, "Successfully applied network configuration", 
                    Toast.LENGTH_SHORT).show();
            
            // Return to dashboard
            finish();
        } else {
            Toast.makeText(this, "Failed to apply network configuration, please try again", 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Updates the UI based on current step
     */
    private void updateUI() {
        // Update button visibility and text
        if (currentStep == 0) {
            previousButton.setVisibility(View.INVISIBLE);
        } else {
            previousButton.setVisibility(View.VISIBLE);
        }
        
        if (currentStep == 3) {
            nextButton.setText(R.string.apply);
        } else {
            nextButton.setText(R.string.next);
        }
        
        // Update title and image based on step
        switch (currentStep) {
            case 0:
                titleText.setText(R.string.select_connection_method);
                methodImage.setImageResource(android.R.drawable.ic_menu_manage);
                break;
            case 1:
                titleText.setText(R.string.select_networks);
                methodImage.setImageResource(android.R.drawable.ic_menu_search);
                break;
            case 2:
                titleText.setText(R.string.configure_options);
                methodImage.setImageResource(android.R.drawable.ic_menu_preferences);
                break;
            case 3:
                titleText.setText(R.string.apply_changes);
                methodImage.setImageResource(android.R.drawable.ic_menu_send);
                break;
        }
    }
    
    /**
     * Moves to the next step in the wizard
     */
    private void moveToNextStep() {
        if (currentStep < 3) {
            // Move to next page
            viewPager.setCurrentItem(currentStep + 1);
        } else {
            // On the last page, apply changes
            applyChanges();
        }
    }
    
    /**
     * Moves to the previous step in the wizard
     */
    private void moveToPreviousStep() {
        if (currentStep > 0) {
            viewPager.setCurrentItem(currentStep - 1);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
