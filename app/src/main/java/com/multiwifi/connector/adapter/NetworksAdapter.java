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
import com.multiwifi.connector.model.NetworkConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying network connections in a RecyclerView
 */
public class NetworksAdapter extends RecyclerView.Adapter<NetworksAdapter.NetworkViewHolder> {
    
    private final Context context;
    private final List<NetworkConnection> networks;
    private final OnNetworkClickListener listener;
    
    /**
     * Interface for network item click events
     */
    public interface OnNetworkClickListener {
        void onNetworkClick(NetworkConnection network);
    }
    
    public NetworksAdapter(Context context, OnNetworkClickListener listener) {
        this.context = context;
        this.networks = new ArrayList<>();
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public NetworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_network, parent, false);
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
     * Updates the adapter with a new list of networks
     * 
     * @param networks The new networks list
     */
    public void updateNetworks(List<NetworkConnection> networks) {
        this.networks.clear();
        if (networks != null) {
            this.networks.addAll(networks);
        }
        notifyDataSetChanged();
    }
    
    class NetworkViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ImageView networkIcon;
        private final TextView networkName;
        private final TextView speedValue;
        private final TextView latencyValue;
        private final TextView allocationValue;
        private NetworkConnection network;
        
        public NetworkViewHolder(@NonNull View itemView) {
            super(itemView);
            networkIcon = itemView.findViewById(R.id.network_icon);
            networkName = itemView.findViewById(R.id.network_name);
            speedValue = itemView.findViewById(R.id.speed_value);
            latencyValue = itemView.findViewById(R.id.latency_value);
            allocationValue = itemView.findViewById(R.id.allocation_value);
            itemView.setOnClickListener(this);
        }
        
        public void bind(NetworkConnection network) {
            this.network = network;
            
            networkName.setText(network.getSsid());
            speedValue.setText(String.format("%.1f Mbps", network.getSpeedMbps()));
            latencyValue.setText(String.format("%d ms", network.getLatencyMs()));
            allocationValue.setText(String.format("%.0f%%", network.getAllocationPercentage()));
            
            // Set appropriate icon based on connection method
            if (network.getSsid().equalsIgnoreCase("Cellular Data")) {
                networkIcon.setImageResource(R.drawable.cellular_wifi);
            } else {
                switch (network.getConnectionMethod()) {
                    case NATIVE:
                        networkIcon.setImageResource(R.drawable.combined_wifi);
                        break;
                    case USB_ADAPTER:
                        networkIcon.setImageResource(R.drawable.usb_adapter);
                        break;
                    case HYBRID:
                        networkIcon.setImageResource(R.drawable.cellular_wifi);
                        break;
                    case PROXY:
                        case VPN:
                            networkIcon.setImageResource(R.drawable.vpn_icon);
                            break;
                        networkIcon.setImageResource(R.drawable.proxy_icon);
                        break;
                    default:
                        networkIcon.setImageResource(R.drawable.wifi_icon);
                        break;
                }
            }
        }
        
        @Override
        public void onClick(View v) {
            if (listener != null && network != null) {
                listener.onNetworkClick(network);
            }
        }
    }
}
