package com.multiwifi.connector.util;

import com.multiwifi.connector.model.NetworkConnection;
import java.util.List;

public class LoadBalancer {
    
    // Load balancing strategies
    public enum Strategy {
        ROUND_ROBIN,
        WEIGHTED,
        LATENCY_BASED,
        ADAPTIVE
    }
    
    private Strategy currentStrategy = Strategy.ADAPTIVE;
    
    public LoadBalancer() {
        // Default constructor
    }
    
    /**
     * Set the load balancing strategy
     */
    public void setStrategy(Strategy strategy) {
        this.currentStrategy = strategy;
    }
    
    /**
     * Get the current load balancing strategy
     */
    public Strategy getStrategy() {
        return currentStrategy;
    }
    
    /**
     * Distribute traffic load across available networks
     * @param networks List of available network connections
     * @param dataSize Size of data to be transmitted in bytes
     * @return List of networks with updated allocation percentages
     */
    public List<NetworkConnection> distributeLoad(List<NetworkConnection> networks, long dataSize) {
        switch (currentStrategy) {
            case ROUND_ROBIN:
                return applyRoundRobinStrategy(networks);
            case WEIGHTED:
                return applyWeightedStrategy(networks);
            case LATENCY_BASED:
                return applyLatencyBasedStrategy(networks);
            case ADAPTIVE:
            default:
                return applyAdaptiveStrategy(networks, dataSize);
        }
    }
    
    /**
     * Round Robin strategy - distributes load equally among networks
     */
    private List<NetworkConnection> applyRoundRobinStrategy(List<NetworkConnection> networks) {
        if (networks.isEmpty()) return networks;
        
        float equalShare = 100.0f / networks.size();
        
        for (NetworkConnection network : networks) {
            network.setAllocationPercentage(equalShare);
        }
        
        return networks;
    }
    
    /**
     * Weighted strategy - distributes load based on network speed
     */
    private List<NetworkConnection> applyWeightedStrategy(List<NetworkConnection> networks) {
        if (networks.isEmpty()) return networks;
        
        // Calculate total speed of all networks
        float totalSpeed = 0;
        for (NetworkConnection network : networks) {
            totalSpeed += network.getSpeed();
        }
        
        // If total speed is 0, fall back to round robin
        if (totalSpeed == 0) {
            return applyRoundRobinStrategy(networks);
        }
        
        // Distribute allocation based on speed ratio
        for (NetworkConnection network : networks) {
            float allocation = (network.getSpeed() / totalSpeed) * 100;
            network.setAllocationPercentage(allocation);
        }
        
        return networks;
    }
    
    /**
     * Latency-based strategy - distributes load inversely proportional to latency
     */
    private List<NetworkConnection> applyLatencyBasedStrategy(List<NetworkConnection> networks) {
        if (networks.isEmpty()) return networks;
        
        // Calculate total inverse latency (lower latency gets higher weight)
        float totalInverseLatency = 0;
        for (NetworkConnection network : networks) {
            // Add a small value to avoid division by zero
            float latency = Math.max(network.getLatency(), 1);
            totalInverseLatency += (1000.0f / latency);
        }
        
        // If total inverse latency is 0, fall back to round robin
        if (totalInverseLatency == 0) {
            return applyRoundRobinStrategy(networks);
        }
        
        // Distribute allocation based on inverse latency ratio
        for (NetworkConnection network : networks) {
            float latency = Math.max(network.getLatency(), 1);
            float inverseLatency = 1000.0f / latency;
            float allocation = (inverseLatency / totalInverseLatency) * 100;
            network.setAllocationPercentage(allocation);
        }
        
        return networks;
    }
    
    /**
     * Adaptive strategy - considers speed, latency, and reliability
     */
    private List<NetworkConnection> applyAdaptiveStrategy(List<NetworkConnection> networks, long dataSize) {
        if (networks.isEmpty()) return networks;
        
        // For small data transfers, prioritize low latency
        if (dataSize < 100000) { // Less than 100KB
            return applyLatencyBasedStrategy(networks);
        }
        
        // For large data transfers, prioritize speed
        return applyWeightedStrategy(networks);
    }
    
    /**
     * Measure and update network performance metrics
     */
    public void updateNetworkMetrics(List<NetworkConnection> networks) {
        // In a real implementation, this would measure and update each network's
        // current speed, latency, and reliability metrics
        
        // For demo purposes, we'll use the existing values or simulate them
        for (NetworkConnection network : networks) {
            // Update speed based on recent measurements
            // Update latency based on recent measurements
            // Update reliability based on recent error rates
            
            // In a real app, these would be determined through active monitoring
        }
    }
}
