package com.multiwifi.connector.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.multiwifi.connector.R;
import com.multiwifi.connector.model.NetworkConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying network connections in a RecyclerView
 */
public class NetworksAdapter extends RecyclerView.Adapter<NetworksAdapter.NetworkViewHolder> {
    
    private List<NetworkConnection> networks;
    
    public NetworksAdapter() {
        this.networks = new ArrayList<>();
    }
    
    @NonNull
    @Override
    public NetworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_network, parent, false);
        return new NetworkViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull NetworkViewHolder holder, int position) {
        NetworkConnection network = networks.get(position);
        holder.bind(network);
    }
    
    @Override
    public int getItemCount() {
        return networks.size();
    }
    
    /**
     * Update the list of networks
     */
    public void updateNetworks(List<NetworkConnection> newNetworks) {
        this.networks = newNetworks;
        notifyDataSetChanged();
    }
    
    /**
     * ViewHolder for network connection items
     */
    static class NetworkViewHolder extends RecyclerView.ViewHolder {
        
        private final ImageView networkIcon;
        private final TextView networkName;
        private final TextView speedValue;
        private final TextView latencyValue;
        private final TextView allocationValue;
        
        public NetworkViewHolder(@NonNull View itemView) {
            super(itemView);
            
            networkIcon = itemView.findViewById(R.id.network_icon);
            networkName = itemView.findViewById(R.id.network_name);
            speedValue = itemView.findViewById(R.id.speed_value);
            latencyValue = itemView.findViewById(R.id.latency_value);
            allocationValue = itemView.findViewById(R.id.allocation_value);
        }
        
        /**
         * Bind network data to the view
         */
        public void bind(NetworkConnection network) {
            // Set network name
            networkName.setText(network.getDisplayName());
            
            // Set network icon based on type
            int iconResId;
            switch (network.getType()) {
                case WIFI:
                    iconResId = R.drawable.wifi_icon;
                    break;
                case CELLULAR:
                    iconResId = R.drawable.cellular_wifi;
                    break;
                case USB_ADAPTER:
                    iconResId = R.drawable.usb_adapter;
                    break;
                case PROXY:
                    iconResId = R.drawable.proxy_icon;
                    break;
                default:
                    iconResId = R.drawable.wifi_icon;
                    break;
            }
            networkIcon.setImageResource(iconResId);
            
            // Set network metrics
            speedValue.setText(String.format("%.1f Mbps", network.getSpeed()));
            latencyValue.setText(String.format("%d ms", network.getLatency()));
            allocationValue.setText(String.format("%.0f%%", network.getAllocationPercentage()));
        }
    }
}
