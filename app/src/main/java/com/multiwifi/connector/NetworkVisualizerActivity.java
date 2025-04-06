package com.multiwifi.connector;

import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.service.MultiWifiService;
import com.multiwifi.connector.view.NetworkSpeedGraphView;
import com.multiwifi.connector.view.PieChartView;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for visualizing network connections and performance
 */
public class NetworkVisualizerActivity extends AppCompatActivity implements MultiWifiService.ConnectionListener {

    private MultiWifiService wifiService;
    private boolean isBound = false;
    private List<NetworkConnection> currentNetworks = new ArrayList<>();
    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private static final long REFRESH_INTERVAL = 2000; // 2 seconds

    // UI Elements
    private TextView statusText;
    private TextView combinedSpeedText;
    private PieChartView pieChartView;
    private NetworkSpeedGraphView speedGraphView;
    private Button refreshButton;
    private CardView noConnectionCard;

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
            wifiService.registerListener(NetworkVisualizerActivity.this);
            
            // Update UI with initial data
            updateVisualization();
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
        setContentView(R.layout.activity_network_visualizer);
        
        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.network_visualizer_title);
        }
        
        // Initialize UI elements
        setupUI();
        
        // Create refresh runnable
        refreshRunnable = this::updateVisualization;
        
        // Bind to service
        Intent serviceIntent = new Intent(this, MultiWifiService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onDestroy() {
        // Unregister listener and unbind service
        if (isBound && wifiService != null) {
            wifiService.unregisterListener(this);
            unbindService(serviceConnection);
            isBound = false;
        }
        
        // Stop refresh handler
        refreshHandler.removeCallbacks(refreshRunnable);
        
        super.onDestroy();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Start periodic updates
        startRefreshTimer();
    }
    
    @Override
    protected void onPause() {
        // Stop periodic updates
        refreshHandler.removeCallbacks(refreshRunnable);
        super.onPause();
    }
    
    /**
     * Sets up the UI elements
     */
    private void setupUI() {
        // Initialize views
        statusText = findViewById(R.id.status_text);
        combinedSpeedText = findViewById(R.id.combined_speed_text);
        pieChartView = findViewById(R.id.pie_chart_view);
        speedGraphView = findViewById(R.id.speed_graph_view);
        refreshButton = findViewById(R.id.refresh_button);
        noConnectionCard = findViewById(R.id.no_connection_card);
        
        // Setup click listeners
        refreshButton.setOnClickListener(v -> updateVisualization());
    }
    
    /**
     * Starts the refresh timer
     */
    private void startRefreshTimer() {
        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }
    
    /**
     * Updates the visualizations with current network data
     */
    private void updateVisualization() {
        if (!isBound || wifiService == null) {
            return;
        }
        
        boolean isConnected = wifiService.isConnected();
        List<NetworkConnection> networks = wifiService.getConnectedNetworks();
        double combinedSpeed = wifiService.getCombinedSpeed();
        
        // Update UI
        if (isConnected) {
            statusText.setText(R.string.status_connected);
            combinedSpeedText.setText(String.format("%.1f Mbps", combinedSpeed));
            noConnectionCard.setVisibility(View.GONE);
            
            // Update pie chart with current allocation
            pieChartView.setNetworks(networks);
            pieChartView.startAnimation();
            
            // Update speed graph
            speedGraphView.addDataPoint(combinedSpeed);
            
            // Save current networks
            currentNetworks = new ArrayList<>(networks);
        } else {
            statusText.setText(R.string.status_disconnected);
            combinedSpeedText.setText("0.0 Mbps");
            noConnectionCard.setVisibility(View.VISIBLE);
            
            // Clear visualizations
            pieChartView.setNetworks(new ArrayList<>());
            speedGraphView.reset();
            
            // Clear current networks
            currentNetworks.clear();
        }
        
        // Schedule next refresh
        startRefreshTimer();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // MultiWifiService.ConnectionListener methods
    
    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        updateVisualization();
    }
    
    @Override
    public void onNetworksUpdated(List<NetworkConnection> networks) {
        updateVisualization();
    }
    
    @Override
    public void onCombinedSpeedChanged(double speedMbps) {
        combinedSpeedText.setText(String.format("%.1f Mbps", speedMbps));
        speedGraphView.addDataPoint(speedMbps);
    }
}
