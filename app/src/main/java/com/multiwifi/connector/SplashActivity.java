package com.multiwifi.connector;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.multiwifi.connector.model.DeviceCapabilities;
import com.multiwifi.connector.util.DeviceCapabilityDetector;
import com.multiwifi.connector.util.NavigationUtils;
import com.multiwifi.connector.util.PermissionHandler;

/**
 * Splash screen activity that detects device capabilities and handles initial setup
 */
public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 2000; // 2 seconds
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private TextView statusText;
    private DeviceCapabilities capabilities;
    
    // Required permissions
    private static final String[] REQUIRED_PERMISSIONS = {
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.CHANGE_WIFI_STATE,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.INTERNET
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        statusText = findViewById(R.id.status_text);
        
        // Check if we have all required permissions
        if (PermissionHandler.hasPermissions(this, REQUIRED_PERMISSIONS)) {
            // Permissions already granted, proceed with initialization
            initialize();
        } else {
            // Request permissions
            statusText.setText(R.string.permissions_required_message);
            PermissionHandler.requestPermissions(this, PERMISSION_REQUEST_CODE, REQUIRED_PERMISSIONS);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (PermissionHandler.hasPermissions(this, REQUIRED_PERMISSIONS)) {
                // All permissions granted, proceed with initialization
                initialize();
            } else {
                // Some permissions denied, show message and exit
                statusText.setText(R.string.permissions_required_settings_message);
                
                // Delay before opening settings
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    PermissionHandler.openAppSettings(this);
                    finish();
                }, 2000);
            }
        }
    }
    
    /**
     * Initializes the app by detecting capabilities and proceeding to the next screen
     */
    private void initialize() {
        statusText.setText(R.string.detecting_capabilities);
        
        // Detect device capabilities in a background thread
        new Thread(() -> {
            capabilities = DeviceCapabilityDetector.detectCapabilities(this);
            
            // Navigate to the next screen after a delay
            new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen, SPLASH_DURATION);
        }).start();
    }
    
    /**
     * Navigates to the next appropriate screen
     */
    private void navigateToNextScreen() {
        // Check if onboarding is needed (first run)
        boolean isFirstRun = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("first_run", true);
        
        if (isFirstRun) {
            // First run, go to onboarding
            Bundle extras = new Bundle();
            extras.putSerializable("capabilities", capabilities);
            NavigationUtils.navigateToAndClearStack(this, OnboardingActivity.class, extras);
            
            // Update first run preference
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("first_run", false)
                    .apply();
        } else {
            // Not first run, go directly to dashboard
            Bundle extras = new Bundle();
            extras.putSerializable("capabilities", capabilities);
            NavigationUtils.navigateToAndClearStack(this, DashboardActivity.class, extras);
        }
    }
}
