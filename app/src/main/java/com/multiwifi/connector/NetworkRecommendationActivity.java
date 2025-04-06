package com.multiwifi.connector;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.multiwifi.connector.adapter.RecommendedNetworksAdapter;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.model.NetworkRecommendation;
import com.multiwifi.connector.service.MultiWifiService;
import com.multiwifi.connector.util.NetworkAnalyzer;
import com.multiwifi.connector.viewmodel.NetworkRecommendationViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for providing smart network connection recommendations
 */
public class NetworkRecommendationActivity extends AppCompatActivity implements 
        RecommendedNetworksAdapter.OnRecommendationClickListener {
    
    private static final String TAG = "NetworkRecommendActivity";
    
    private MultiWifiService wifiService;
    private boolean isBound = false;
    private NetworkRecommendationViewModel viewModel;
    private NetworkAnalyzer networkAnalyzer;
    
    // UI Elements
    private ProgressBar scanProgressBar;
    private TextView statusText;
    private RecyclerView recommendationsRecyclerView;
    private Button applyButton;
    private Button rescanButton;
    private RecommendedNetworksAdapter adapter;
    
    // Service connection for binding to MultiWifiService
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MultiWifiService.LocalBinder localBinder = (MultiWifiService.LocalBinder) binder;
            wifiService = localBinder.getService();
            isBound = true;
            
            // Start scanning when service is connected
            startScan();
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
        setContentView(R.layout.activity_network_recommendation);
        
        // Initialize ViewModel
        viewModel = new NetworkRecommendationViewModel();
        
        // Initialize NetworkAnalyzer
        networkAnalyzer = new NetworkAnalyzer();
        
        // Initialize UI
        setupUI();
        
        // Bind to service
        Intent serviceIntent = new Intent(this, MultiWifiService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onDestroy() {
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
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.network_recommendation_title);
        }
        
        // Initialize UI elements
        scanProgressBar = findViewById(R.id.scan_progress);
        statusText = findViewById(R.id.status_text);
        recommendationsRecyclerView = findViewById(R.id.recommendations_recyclerview);
        applyButton = findViewById(R.id.apply_button);
        rescanButton = findViewById(R.id.rescan_button);
        
        // Set up RecyclerView
        recommendationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecommendedNetworksAdapter(this);
        recommendationsRecyclerView.setAdapter(adapter);
        
        // Set up click listeners
        applyButton.setOnClickListener(v -> applyRecommendations());
        rescanButton.setOnClickListener(v -> startScan());
        
        // Initialize UI state
        updateUIForScanning(true);
    }
    
    /**
     * Starts scanning for networks and generating recommendations
     */
    private void startScan() {
        if (!isBound || wifiService == null) {
            Toast.makeText(this, "Service not connected, please try again", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Update UI to show scanning
        updateUIForScanning(true);
        
        // Clear previous recommendations
        adapter.clearRecommendations();
        
        // Start network scan in a background thread
        new Thread(() -> {
            // Scan available networks
            List<NetworkConnection> availableNetworks = wifiService.scanNetworks();
            
            // Analyze networks and generate recommendations
            final List<NetworkRecommendation> recommendations = 
                    networkAnalyzer.generateRecommendations(availableNetworks);
            
            // Update UI on main thread
            runOnUiThread(() -> {
                updateUIForScanning(false);
                
                if (recommendations.isEmpty()) {
                    statusText.setText(R.string.no_recommendations);
                } else {
                    statusText.setText(R.string.recommendations_ready);
                    adapter.updateRecommendations(recommendations);
                }
            });
        }).start();
    }
    
    /**
     * Updates UI state based on whether scanning is in progress
     * 
     * @param isScanning True if scanning is in progress
     */
    private void updateUIForScanning(boolean isScanning) {
        if (isScanning) {
            scanProgressBar.setVisibility(View.VISIBLE);
            statusText.setText(R.string.scanning_networks);
            applyButton.setEnabled(false);
        } else {
            scanProgressBar.setVisibility(View.GONE);
            applyButton.setEnabled(true);
        }
    }
    
    /**
     * Applies the selected recommendations
     */
    private void applyRecommendations() {
        if (!isBound || wifiService == null) {
            Toast.makeText(this, "Service not connected, please try again", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<NetworkRecommendation> selectedRecommendations = adapter.getSelectedRecommendations();
        
        if (selectedRecommendations.isEmpty()) {
            Toast.makeText(this, "Please select at least one recommended configuration", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Extract networks from the selected recommendation
        NetworkRecommendation selectedRecommendation = selectedRecommendations.get(0);
        List<NetworkConnection> networksToConnect = selectedRecommendation.getNetworks();
        
        // Save the selected networks
        viewModel.saveSelectedNetworks(networksToConnect);
        
        // Connect to the selected networks
        boolean success = wifiService.connectToNetworks(networksToConnect);
        
        if (success) {
            Toast.makeText(this, "Applying recommended configuration...", Toast.LENGTH_SHORT).show();
            
            // Return to dashboard
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Failed to apply recommendation, please try again", 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRecommendationClick(NetworkRecommendation recommendation) {
        // Show details dialog
        new AlertDialog.Builder(this)
                .setTitle(recommendation.getTitle())
                .setMessage(recommendation.getDetailedDescription())
                .setPositiveButton("OK", null)
                .show();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
