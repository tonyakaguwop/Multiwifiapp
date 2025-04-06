package com.multiwifi.connector;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.multiwifi.connector.model.DeviceCapabilities;
import com.multiwifi.connector.util.DeviceCapabilityDetector;
import com.multiwifi.connector.util.NavigationUtils;

/**
 * Main activity as entry point which redirects to appropriate screens
 */
public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Short delay before navigating to allow UI to render
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen, 500);
    }
    
    /**
     * Navigates to the appropriate next screen based on app state
     */
    private void navigateToNextScreen() {
        // For simplicity, we'll just go to the splash screen
        // In a real app, we might check if onboarding is completed, etc.
        NavigationUtils.navigateToAndClearStack(this, SplashActivity.class);
    }
}
