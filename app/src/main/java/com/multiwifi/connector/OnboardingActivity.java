package com.multiwifi.connector;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.multiwifi.connector.adapter.OnboardingPagerAdapter;
import com.multiwifi.connector.model.DeviceCapabilities;
import com.multiwifi.connector.util.NavigationUtils;

/**
 * Activity for app onboarding flow
 */
public class OnboardingActivity extends AppCompatActivity {
    
    private ViewPager2 viewPager;
    private TabLayout pageIndicator;
    private Button nextButton;
    private Button skipButton;
    private OnboardingPagerAdapter adapter;
    private DeviceCapabilities capabilities;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        
        // Get device capabilities from intent
        if (getIntent().hasExtra("capabilities")) {
            capabilities = (DeviceCapabilities) getIntent().getSerializableExtra("capabilities");
        } else {
            // Default capabilities if not provided
            capabilities = new DeviceCapabilities();
        }
        
        // Initialize views
        viewPager = findViewById(R.id.viewPager);
        pageIndicator = findViewById(R.id.pageIndicator);
        nextButton = findViewById(R.id.nextButton);
        skipButton = findViewById(R.id.skipButton);
        
        // Setup ViewPager
        adapter = new OnboardingPagerAdapter(this, capabilities);
        viewPager.setAdapter(adapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(pageIndicator, viewPager, (tab, position) -> {
            // Tab customization if needed
        }).attach();
        
        // Setup button listeners
        setupButtons();
    }
    
    /**
     * Sets up the next and skip button behavior
     */
    private void setupButtons() {
        nextButton.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem < adapter.getItemCount() - 1) {
                // Go to next page
                viewPager.setCurrentItem(currentItem + 1);
            } else {
                // Last page, complete onboarding
                finishOnboarding();
            }
        });
        
        skipButton.setOnClickListener(v -> finishOnboarding());
        
        // Update button text for the last page
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                
                if (position == adapter.getItemCount() - 1) {
                    // Last page
                    nextButton.setText(R.string.get_started);
                    skipButton.setVisibility(View.GONE);
                } else {
                    nextButton.setText(R.string.next);
                    skipButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    
    /**
     * Completes the onboarding process and navigates to the dashboard
     */
    private void finishOnboarding() {
        Bundle extras = new Bundle();
        extras.putSerializable("capabilities", capabilities);
        NavigationUtils.navigateToAndClearStack(this, DashboardActivity.class, extras);
    }
}
