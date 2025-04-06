package com.multiwifi.connector.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.multiwifi.connector.R;
import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.DeviceCapabilities;

/**
 * Adapter for onboarding ViewPager
 */
public class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder> {
    
    private final Context context;
    private final DeviceCapabilities capabilities;
    
    // Page titles and descriptions
    private final String[] titles = new String[4];
    private final String[] descriptions = new String[4];
    private final int[] illustrations = new int[4];
    
    public OnboardingPagerAdapter(Context context, DeviceCapabilities capabilities) {
        this.context = context;
        this.capabilities = capabilities;
        
        // Initialize page data
        setupPageContent();
    }
    
    /**
     * Sets up the content for each onboarding page
     */
    private void setupPageContent() {
        // Page 1: Welcome
        titles[0] = context.getString(R.string.onboarding_title_welcome);
        descriptions[0] = context.getString(R.string.onboarding_desc_welcome);
        illustrations[0] = R.drawable.combined_wifi;
        
        // Page 2: Method
        titles[1] = context.getString(R.string.onboarding_title_method);
        
        // Get connection method description
        String methodName;
        switch (capabilities.getRecommendedMethod()) {
            case NATIVE:
                methodName = "Native Multi-WiFi";
                illustrations[1] = R.drawable.combined_wifi;
                break;
            case USB_ADAPTER:
                methodName = "USB Adapter";
                illustrations[1] = R.drawable.usb_adapter;
                break;
            case HYBRID:
                methodName = "WiFi + Cellular Hybrid";
                illustrations[1] = R.drawable.cellular_wifi;
                break;
            case PROXY:
            default:
                methodName = "Proxy-based Networking";
                illustrations[1] = R.drawable.proxy_icon;
                break;
        }
        
        descriptions[1] = String.format(context.getString(R.string.onboarding_desc_method), methodName);
        
        // Page 3: Dashboard
        titles[2] = context.getString(R.string.onboarding_title_dashboard);
        descriptions[2] = context.getString(R.string.onboarding_desc_dashboard);
        illustrations[2] = R.drawable.wifi_icon;
        
        // Page 4: Start
        titles[3] = context.getString(R.string.onboarding_title_start);
        descriptions[3] = context.getString(R.string.onboarding_desc_start);
        illustrations[3] = R.drawable.wifi_icon;
    }
    
    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.onboarding_page, parent, false);
        return new OnboardingViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.title.setText(titles[position]);
        holder.description.setText(descriptions[position]);
        holder.illustration.setImageResource(illustrations[position]);
    }
    
    @Override
    public int getItemCount() {
        return titles.length;
    }
    
    static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        ImageView illustration;
        TextView title;
        TextView description;
        
        public OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            illustration = itemView.findViewById(R.id.illustration);
            title = itemView.findViewById(R.id.title);
            description = itemView.findViewById(R.id.description);
        }
    }
}
