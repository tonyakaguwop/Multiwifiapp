package com.multiwifi.connector.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.multiwifi.connector.R;
import com.multiwifi.connector.model.ConnectionMethod;
import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.model.SwitchOption;
import com.multiwifi.connector.service.MultiWifiService;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the network switch wizard ViewPager
 */
public class NetworkSwitchPagerAdapter extends FragmentStateAdapter {
    
    private static final int NUM_PAGES = 4;
    
    private List<SwitchOption> switchOptions;
    private ConnectionMethod selectedMethod;
    private List<NetworkConnection> selectedNetworks = new ArrayList<>();
    private boolean autoReconnect = true;
    private boolean backgroundScan = true;
    
    /**
     * Constructor
     *
     * @param activity The FragmentActivity hosting the adapter
     * @param switchOptions List of available switch options
     */
    public NetworkSwitchPagerAdapter(FragmentActivity activity, List<SwitchOption> switchOptions) {
        super(activity);
        this.switchOptions = switchOptions;
        
        // Find the initially selected method
        for (SwitchOption option : switchOptions) {
            if (option.isSelected()) {
                selectedMethod = option.getMethod();
                break;
            }
        }
        
        // If no method is selected, default to the first one
        if (selectedMethod == null && !switchOptions.isEmpty()) {
            selectedMethod = switchOptions.get(0).getMethod();
            switchOptions.get(0).setSelected(true);
        }
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Create the appropriate fragment based on the position
        WizardPageFragment fragment = new WizardPageFragment();
        Bundle args = new Bundle();
        args.putInt(WizardPageFragment.ARG_PAGE_NUMBER, position);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
    
    /**
     * Gets the selected connection method
     *
     * @return The selected method
     */
    public ConnectionMethod getSelectedMethod() {
        return selectedMethod;
    }
    
    /**
     * Sets the selected connection method
     *
     * @param method The method to select
     */
    public void setSelectedMethod(ConnectionMethod method) {
        this.selectedMethod = method;
        
        // Update selection in switch options
        for (SwitchOption option : switchOptions) {
            option.setSelected(option.getMethod() == method);
        }
    }
    
    /**
     * Gets the list of selected networks
     *
     * @return Selected networks
     */
    public List<NetworkConnection> getSelectedNetworks() {
        return selectedNetworks;
    }
    
    /**
     * Sets the list of selected networks
     *
     * @param networks Networks to select
     */
    public void setSelectedNetworks(List<NetworkConnection> networks) {
        this.selectedNetworks = new ArrayList<>(networks);
    }
    
    /**
     * Gets auto-reconnect setting
     *
     * @return true if auto-reconnect is enabled
     */
    public boolean isAutoReconnectEnabled() {
        return autoReconnect;
    }
    
    /**
     * Sets auto-reconnect setting
     *
     * @param enabled true to enable auto-reconnect
     */
    public void setAutoReconnectEnabled(boolean enabled) {
        this.autoReconnect = enabled;
    }
    
    /**
     * Gets background scan setting
     *
     * @return true if background scan is enabled
     */
    public boolean isBackgroundScanEnabled() {
        return backgroundScan;
    }
    
    /**
     * Sets background scan setting
     *
     * @param enabled true to enable background scan
     */
    public void setBackgroundScanEnabled(boolean enabled) {
        this.backgroundScan = enabled;
    }
    
    /**
     * Fragment for each wizard page
     */
    public static class WizardPageFragment extends Fragment {
        
        public static final String ARG_PAGE_NUMBER = "page_number";
        
        private int pageNumber;
        private NetworkSwitchPagerAdapter parentAdapter;
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                pageNumber = getArguments().getInt(ARG_PAGE_NUMBER);
            }
            parentAdapter = (NetworkSwitchPagerAdapter) getParentFragment();
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Create view based on page number
            View rootView;
            switch (pageNumber) {
                case 0:
                    rootView = createMethodSelectionPage(inflater, container);
                    break;
                case 1:
                    rootView = createNetworkSelectionPage(inflater, container);
                    break;
                case 2:
                    rootView = createConfigurationPage(inflater, container);
                    break;
                case 3:
                    rootView = createSummaryPage(inflater, container);
                    break;
                default:
                    rootView = inflater.inflate(R.layout.fragment_wizard_page_default, container, false);
                    break;
            }
            
            return rootView;
        }
        
        /**
         * Creates the method selection page (step 1)
         */
        private View createMethodSelectionPage(LayoutInflater inflater, ViewGroup container) {
            View view = inflater.inflate(R.layout.fragment_wizard_page_methods, container, false);
            
            RecyclerView methodsRecyclerView = view.findViewById(R.id.methods_recyclerview);
            methodsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            
            MethodsAdapter adapter = new MethodsAdapter();
            methodsRecyclerView.setAdapter(adapter);
            
            return view;
        }
        
        /**
         * Creates the network selection page (step 2)
         */
        private View createNetworkSelectionPage(LayoutInflater inflater, ViewGroup container) {
            View view = inflater.inflate(R.layout.fragment_wizard_page_networks, container, false);
            
            RecyclerView networksRecyclerView = view.findViewById(R.id.networks_recyclerview);
            networksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            
            // Simulate scanning for networks
            List<NetworkConnection> availableNetworks = new ArrayList<>();
            NetworkConnection network1 = new NetworkConnection();
            network1.setSsid("WiFi Network 1");
            network1.setBssid("00:11:22:33:44:55");
            network1.setSpeedMbps(25.5);
            network1.setLatencyMs(15);
            availableNetworks.add(network1);
            
            NetworkConnection network2 = new NetworkConnection();
            network2.setSsid("WiFi Network 2");
            network2.setBssid("AA:BB:CC:DD:EE:FF");
            network2.setSpeedMbps(18.2);
            network2.setLatencyMs(22);
            availableNetworks.add(network2);
            
            NetworkConnection network3 = new NetworkConnection();
            network3.setSsid("WiFi Network 3");
            network3.setBssid("11:22:33:44:55:66");
            network3.setSpeedMbps(40.0);
            network3.setLatencyMs(10);
            availableNetworks.add(network3);
            
            NetworksAdapter adapter = new NetworksAdapter(availableNetworks);
            networksRecyclerView.setAdapter(adapter);
            
            return view;
        }
        
        /**
         * Creates the configuration page (step 3)
         */
        private View createConfigurationPage(LayoutInflater inflater, ViewGroup container) {
            View view = inflater.inflate(R.layout.fragment_wizard_page_config, container, false);
            
            // Set up checkboxes
            CheckBox autoReconnectCheckbox = view.findViewById(R.id.auto_reconnect_checkbox);
            CheckBox backgroundScanCheckbox = view.findViewById(R.id.background_scan_checkbox);
            
            // Set initial values
            NetworkSwitchPagerAdapter adapter = (NetworkSwitchPagerAdapter) getParentFragmentManager()
                    .findFragmentByTag("f" + pageNumber)
                    .getParentFragment();
            
            if (adapter != null) {
                autoReconnectCheckbox.setChecked(adapter.isAutoReconnectEnabled());
                backgroundScanCheckbox.setChecked(adapter.isBackgroundScanEnabled());
            }
            
            // Set change listeners
            autoReconnectCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (adapter != null) {
                    adapter.setAutoReconnectEnabled(isChecked);
                }
            });
            
            backgroundScanCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (adapter != null) {
                    adapter.setBackgroundScanEnabled(isChecked);
                }
            });
            
            return view;
        }
        
        /**
         * Creates the summary page (step 4)
         */
        private View createSummaryPage(LayoutInflater inflater, ViewGroup container) {
            View view = inflater.inflate(R.layout.fragment_wizard_page_summary, container, false);
            
            // Find TextViews
            TextView methodText = view.findViewById(R.id.summary_method_text);
            TextView networksText = view.findViewById(R.id.summary_networks_text);
            TextView configText = view.findViewById(R.id.summary_config_text);
            
            // Get data from adapter
            NetworkSwitchPagerAdapter adapter = (NetworkSwitchPagerAdapter) getParentFragmentManager()
                    .findFragmentByTag("f" + pageNumber)
                    .getParentFragment();
            
            if (adapter != null) {
                // Set method info
                ConnectionMethod method = adapter.getSelectedMethod();
                String methodName = "Unknown";
                
                switch (method) {
                    case NATIVE:
                        methodName = "Native Multi-WiFi";
                        break;
                    case USB_ADAPTER:
                        methodName = "USB WiFi Adapter";
                        break;
                    case HYBRID:
                        methodName = "WiFi + Cellular";
                        break;
                    case VPN:
                        methodName = "VPN Routing";
                        break;
                    case PROXY:
                        methodName = "Proxy Mode";
                        break;
                }
                
                methodText.setText(methodName);
                
                // Set networks info
                List<NetworkConnection> networks = adapter.getSelectedNetworks();
                StringBuilder networksBuilder = new StringBuilder();
                
                if (networks.isEmpty()) {
                    networksBuilder.append("No networks selected");
                } else {
                    for (NetworkConnection network : networks) {
                        networksBuilder.append("• ").append(network.getSsid())
                                .append(" (").append(String.format("%.1f Mbps", network.getSpeedMbps()))
                                .append(")\n");
                    }
                }
                
                networksText.setText(networksBuilder.toString());
                
                // Set configuration info
                StringBuilder configBuilder = new StringBuilder();
                configBuilder.append("Auto-reconnect: ").append(adapter.isAutoReconnectEnabled() ? "Enabled" : "Disabled");
                configBuilder.append("\nBackground scanning: ").append(adapter.isBackgroundScanEnabled() ? "Enabled" : "Disabled");
                
                configText.setText(configBuilder.toString());
            }
            
            return view;
        }
        
        /**
         * Adapter for the methods selection RecyclerView
         */
        private class MethodsAdapter extends RecyclerView.Adapter<MethodsAdapter.ViewHolder> {
            
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_connection_method, parent, false);
                return new ViewHolder(view);
            }
            
            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                NetworkSwitchPagerAdapter adapter = (NetworkSwitchPagerAdapter) getParentFragmentManager()
                        .findFragmentByTag("f" + pageNumber)
                        .getParentFragment();
                
                if (adapter != null) {
                    SwitchOption option = adapter.switchOptions.get(position);
                    
                    holder.titleText.setText(option.getTitle());
                    holder.descriptionText.setText(option.getDescription());
                    holder.radioButton.setChecked(option.isSelected());
                    
                    holder.itemView.setOnClickListener(v -> {
                        // Update selection
                        for (int i = 0; i < adapter.switchOptions.size(); i++) {
                            adapter.switchOptions.get(i).setSelected(i == holder.getAdapterPosition());
                        }
                        
                        // Update selected method
                        adapter.selectedMethod = option.getMethod();
                        
                        // Refresh the list
                        notifyDataSetChanged();
                    });
                }
            }
            
            @Override
            public int getItemCount() {
                NetworkSwitchPagerAdapter adapter = (NetworkSwitchPagerAdapter) getParentFragmentManager()
                        .findFragmentByTag("f" + pageNumber)
                        .getParentFragment();
                
                return adapter != null ? adapter.switchOptions.size() : 0;
            }
            
            class ViewHolder extends RecyclerView.ViewHolder {
                RadioButton radioButton;
                TextView titleText;
                TextView descriptionText;
                
                ViewHolder(View itemView) {
                    super(itemView);
                    radioButton = itemView.findViewById(R.id.method_radio);
                    titleText = itemView.findViewById(R.id.method_title);
                    descriptionText = itemView.findViewById(R.id.method_description);
                }
            }
        }
        
        /**
         * Adapter for the networks selection RecyclerView
         */
        private class NetworksAdapter extends RecyclerView.Adapter<NetworksAdapter.ViewHolder> {
            
            private List<NetworkConnection> networks;
            private boolean[] checked;
            
            NetworksAdapter(List<NetworkConnection> networks) {
                this.networks = networks;
                checked = new boolean[networks.size()];
                
                // Check networks that are already selected
                NetworkSwitchPagerAdapter adapter = (NetworkSwitchPagerAdapter) getParentFragmentManager()
                        .findFragmentByTag("f" + pageNumber)
                        .getParentFragment();
                
                if (adapter != null) {
                    List<NetworkConnection> selectedNetworks = adapter.getSelectedNetworks();
                    
                    for (int i = 0; i < networks.size(); i++) {
                        for (NetworkConnection selected : selectedNetworks) {
                            if (networks.get(i).getSsid().equals(selected.getSsid())) {
                                checked[i] = true;
                                break;
                            }
                        }
                    }
                }
            }
            
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_network_selection, parent, false);
                return new ViewHolder(view);
            }
            
            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                NetworkConnection network = networks.get(position);
                
                holder.titleText.setText(network.getSsid());
                holder.detailsText.setText(String.format("Speed: %.1f Mbps, Latency: %d ms",
                        network.getSpeedMbps(), network.getLatencyMs()));
                holder.checkBox.setChecked(checked[position]);
                
                holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    checked[position] = isChecked;
                    updateSelectedNetworks();
                });
                
                holder.itemView.setOnClickListener(v -> {
                    holder.checkBox.toggle();
                });
            }
            
            @Override
            public int getItemCount() {
                return networks.size();
            }
            
            /**
             * Updates the selected networks in the parent adapter
             */
            private void updateSelectedNetworks() {
                List<NetworkConnection> selected = new ArrayList<>();
                
                for (int i = 0; i < networks.size(); i++) {
                    if (checked[i]) {
                        selected.add(networks.get(i));
                    }
                }
                
                NetworkSwitchPagerAdapter adapter = (NetworkSwitchPagerAdapter) getParentFragmentManager()
                        .findFragmentByTag("f" + pageNumber)
                        .getParentFragment();
                
                if (adapter != null) {
                    adapter.setSelectedNetworks(selected);
                }
            }
            
            class ViewHolder extends RecyclerView.ViewHolder {
                CheckBox checkBox;
                TextView titleText;
                TextView detailsText;
                
                ViewHolder(View itemView) {
                    super(itemView);
                    checkBox = itemView.findViewById(R.id.network_checkbox);
                    titleText = itemView.findViewById(R.id.network_title);
                    detailsText = itemView.findViewById(R.id.network_details);
                }
            }
        }
    }
}
