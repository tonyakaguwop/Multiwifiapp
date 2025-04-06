package com.multiwifi.connector;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.multiwifi.connector.util.DeviceCapabilityDetector;
import com.multiwifi.connector.util.PermissionHandler;

public class SplashActivity extends AppCompatActivity {
    
    private static final long SPLASH_DELAY = 2000; // 2 seconds
    private DeviceCapabilityDetector capabilityDetector;
    private PermissionHandler permissionHandler;
    private TextView statusTextView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        statusTextView = findViewById(R.id.status_text);
        
        // Initialize capability detector
        capabilityDetector = new DeviceCapabilityDetector(this);
        permissionHandler = new PermissionHandler(this);
        
        // Detect device capabilities
        detectCapabilities();
    }
    
    private void detectCapabilities() {
        statusTextView.setText(R.string.detecting_capabilities);
        
        // Simulate detection process
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Store detected capabilities
            capabilityDetector.detectCapabilities();
            
            // Check if first launch
            if (isFirstLaunch()) {
                navigateToOnboarding();
            } else {
                navigateToDashboard();
            }
        }, SPLASH_DELAY);
    }
    
    private boolean isFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences("MultiWifiPrefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("IsFirstLaunch", true);
        
        // If it's the first launch, update the flag
        if (isFirstLaunch) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("IsFirstLaunch", false);
            editor.apply();
        }
        
        return isFirstLaunch;
    }
    
    private void navigateToOnboarding() {
        Intent intent = new Intent(this, OnboardingActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void navigateToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}
