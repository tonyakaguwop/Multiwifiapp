package com.multiwifi.connector;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.multiwifi.connector.adapter.OnboardingPagerAdapter;
import com.multiwifi.connector.model.DeviceCapabilities;
import com.multiwifi.connector.util.DeviceCapabilityDetector;
import com.multiwifi.connector.util.PermissionHandler;

public class OnboardingActivity extends AppCompatActivity {
    
    private ViewPager2 viewPager;
    private Button nextButton;
    private Button skipButton;
    private TabLayout pageIndicator;
    private DeviceCapabilityDetector capabilityDetector;
    private PermissionHandler permissionHandler;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private OnboardingPagerAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        
        // Initialize views
        viewPager = findViewById(R.id.viewPager);
        nextButton = findViewById(R.id.nextButton);
        skipButton = findViewById(R.id.skipButton);
        pageIndicator = findViewById(R.id.pageIndicator);
        
        // Initialize capability detector
        capabilityDetector = new DeviceCapabilityDetector(this);
        permissionHandler = new PermissionHandler(this);
        
        // Get device capabilities
        DeviceCapabilities capabilities = capabilityDetector.getDeviceCapabilities();
        
        // Setup adapter based on device capabilities
        setupAdapter(capabilities);
        
        // Setup button listeners
        setupListeners();
    }
    
    private void setupAdapter(DeviceCapabilities capabilities) {
        // Create adapter and set to ViewPager
        adapter = new OnboardingPagerAdapter(this, capabilities);
        viewPager.setAdapter(adapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(pageIndicator, viewPager, (tab, position) -> {
            // No text needed for the tabs as they are just indicators
        }).attach();
        
        // ViewPager callback to update buttons
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateButtonsForPage(position);
            }
        });
    }
    
    private void setupListeners() {
        // Next button listener
        nextButton.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            
            if (currentItem == adapter.getItemCount() - 1) {
                // Last page, request permissions if needed
                if (permissionHandler.areAllPermissionsGranted()) {
                    finishOnboarding();
                } else {
                    permissionHandler.requestRequiredPermissions(PERMISSION_REQUEST_CODE);
                }
            } else {
                // Move to next page
                viewPager.setCurrentItem(currentItem + 1);
            }
        });
        
        // Skip button listener
        skipButton.setOnClickListener(v -> finishOnboarding());
    }
    
    private void updateButtonsForPage(int position) {
        if (position == adapter.getItemCount() - 1) {
            // Last page
            nextButton.setText(R.string.get_started);
            skipButton.setVisibility(View.GONE);
        } else {
            nextButton.setText(R.string.next);
            skipButton.setVisibility(View.VISIBLE);
        }
    }
    
    private void finishOnboarding() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                finishOnboarding();
            } else {
                // Show permission explanation dialog
                permissionHandler.showPermissionExplanationDialog();
            }
        }
    }
}
