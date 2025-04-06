package com.multiwifi.connector;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.multiwifi.connector.adapter.NetworksAdapter;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.service.MultiWifiService;
import com.multiwifi.connector.util.NavigationUtils;
import com.multiwifi.connector.viewmodel.DashboardViewModel;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {
    
    private Button connectionButton;
    private TextView statusText;
    private TextView combinedSpeedText;
    private RecyclerView networksRecyclerView;
    private NetworksAdapter networksAdapter;
    private DashboardViewModel viewModel;
    private MultiWifiService multiWifiService;
    private boolean isBound = false;
    
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MultiWifiService.LocalBinder binder = (MultiWifiService.LocalBinder) service;
            multiWifiService = binder.getService();
            isBound = true;
            
            // Initialize service with the view model
            viewModel.setMultiWifiService(multiWifiService);
            
            // Update UI with current service status
            updateConnectionStatus(multiWifiService.isConnected());
            updateNetworksList(multiWifiService.getConnectedNetworks());
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        
        // Initialize views
        initViews();
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup observers
        setupObservers();
        
        // Setup click listeners
        setupClickListeners();
        
        // Bind to the service
        bindService();
    }
    
    private void initViews() {
        connectionButton = findViewById(R.id.connection_button);
        statusText = findViewById(R.id.status_text);
        combinedSpeedText = findViewById(R.id.combined_speed_value);
        networksRecyclerView = findViewById(R.id.networks_recyclerview);
    }
    
    private void setupRecyclerView() {
        networksAdapter = new NetworksAdapter();
        networksRecyclerView.setAdapter(networksAdapter);
        networksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void setupObservers() {
        // Observe connection status
        viewModel.getConnectionStatus().observe(this, this::updateConnectionStatus);
        
        // Observe combined speed
        viewModel.getCombinedSpeed().observe(this, this::updateCombinedSpeed);
        
        // Observe network connections
        viewModel.getNetworkConnections().observe(this, this::updateNetworksList);
    }
    
    private void setupClickListeners() {
        connectionButton.setOnClickListener(v -> {
            if (isBound) {
                if (multiWifiService.isConnected()) {
                    viewModel.disconnect();
                } else {
                    viewModel.connect();
                }
            }
        });
    }
    
    private void bindService() {
        Intent intent = new Intent(this, MultiWifiService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    private void updateConnectionStatus(boolean isConnected) {
        if (isConnected) {
            statusText.setText(R.string.status_connected);
            statusText.setTextColor(getResources().getColor(R.color.colorConnected, getTheme()));
            connectionButton.setText(R.string.disconnect);
        } else {
            statusText.setText(R.string.status_disconnected);
            statusText.setTextColor(getResources().getColor(R.color.colorDisconnected, getTheme()));
            connectionButton.setText(R.string.connect);
        }
    }
    
    private void updateCombinedSpeed(double speed) {
        combinedSpeedText.setText(String.format(getString(R.string.combined_speed_format), speed));
    }
    
    private void updateNetworksList(List<NetworkConnection> networks) {
        networksAdapter.updateNetworks(networks);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            // Open settings
            NavigationUtils.navigateToSettings(this);
            return true;
        } else if (item.getItemId() == R.id.action_help) {
            // Open help
            NavigationUtils.navigateToHelp(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
