package com.multiwifi.connector.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.multiwifi.connector.R;
import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.DeviceCapabilities;
import java.util.ArrayList;
import java.util.List;

public class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder> {
    
    private final Context context;
    private final List<OnboardingPage> pages;
    
    public OnboardingPagerAdapter(Context context, DeviceCapabilities capabilities) {
        this.context = context;
        this.pages = createPages(capabilities);
    }
    
    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.onboarding_page, parent, false);
        return new PageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        OnboardingPage page = pages.get(position);
        holder.bind(page);
    }
    
    @Override
    public int getItemCount() {
        return pages.size();
    }
    
    private List<OnboardingPage> createPages(DeviceCapabilities capabilities) {
        List<OnboardingPage> onboardingPages = new ArrayList<>();
        
        // Welcome page
        onboardingPages.add(new OnboardingPage(
                R.drawable.combined_wifi,
                context.getString(R.string.welcome_title),
                context.getString(R.string.welcome_description)
        ));
        
        // Device capabilities page
        ConnectionMethod method = capabilities.getRecommendedMethod();
        int capabilityIcon;
        String capabilityDescription;
        
        switch (method) {
            case NATIVE:
                capabilityIcon = R.drawable.combined_wifi;
                capabilityDescription = "Your device supports native multi-WiFi connections. " +
                        "We'll use this capability for the best performance.";
                break;
                
            case USB_ADAPTER:
                capabilityIcon = R.drawable.usb_adapter;
                capabilityDescription = "Your device supports USB OTG for external WiFi adapters. " +
                        "Connect a USB WiFi adapter for enhanced speed.";
                break;
                
            case HYBRID:
                capabilityIcon = R.drawable.cellular_wifi;
                capabilityDescription = "Your device can combine WiFi and cellular data. " +
                        "We'll use both for improved connectivity.";
                break;
                
            case PROXY:
            default:
                capabilityIcon = R.drawable.proxy_icon;
                capabilityDescription = "We'll use our proxy service to optimize your connection " +
                        "and enhance your internet speed.";
                break;
        }
        
        onboardingPages.add(new OnboardingPage(
                capabilityIcon,
                context.getString(R.string.capabilities_title),
                capabilityDescription
        ));
        
        // Permissions page
        onboardingPages.add(new OnboardingPage(
                android.R.drawable.ic_menu_manage,
                context.getString(R.string.permissions_title),
                context.getString(R.string.permissions_description)
        ));
        
        // Ready page
        onboardingPages.add(new OnboardingPage(
                android.R.drawable.ic_dialog_info,
                context.getString(R.string.ready_title),
                context.getString(R.string.ready_description)
        ));
        
        return onboardingPages;
    }
    
    static class PageViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView titleView;
        private final TextView descriptionView;
        
        public PageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.onboarding_image);
            titleView = itemView.findViewById(R.id.onboarding_title);
            descriptionView = itemView.findViewById(R.id.onboarding_description);
        }
        
        public void bind(OnboardingPage page) {
            imageView.setImageResource(page.getImageResId());
            titleView.setText(page.getTitle());
            descriptionView.setText(page.getDescription());
        }
    }
    
    private static class OnboardingPage {
        private final int imageResId;
        private final String title;
        private final String description;
        
        public OnboardingPage(int imageResId, String title, String description) {
            this.imageResId = imageResId;
            this.title = title;
            this.description = description;
        }
        
        public int getImageResId() {
            return imageResId;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
