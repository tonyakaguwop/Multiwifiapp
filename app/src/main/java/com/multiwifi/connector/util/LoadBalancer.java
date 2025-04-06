package com.multiwifi.connector.util;

import android.util.Log;

import com.multiwifi.connector.model.NetworkConnection;

import java.util.List;

/**
 * Utility class to handle load balancing across multiple network connections
 */
public class LoadBalancer {
    private static final String TAG = "LoadBalancer";
    
    // Load balancing strategies
    public enum Strategy {
        ROUND_ROBIN,
        SPEED_BASED,
        LATENCY_BASED,
        ADAPTIVE
    }
    
    private Strategy currentStrategy;
    
    public LoadBalancer() {
        this.currentStrategy = Strategy.ADAPTIVE; // Default
    }
    
    /**
     * Sets the load balancing strategy
     * 
     * @param strategy The strategy to use
     */
    public void setStrategy(Strategy strategy) {
        this.currentStrategy = strategy;
        Log.d(TAG, "Load balancing strategy set to: " + strategy);
    }
    
    /**
     * Gets the current load balancing strategy
     * 
     * @return Current strategy
     */
    public Strategy getStrategy() {
        return currentStrategy;
    }
    
    /**
     * Computes allocation percentages for each network connection based on the current strategy
     * 
     * @param connections List of network connections
     */
    public void computeAllocation(List<NetworkConnection> connections) {
        if (connections == null || connections.isEmpty()) {
            Log.w(TAG, "No connections to allocate traffic to");
            return;
        }
        
        switch (currentStrategy) {
            case ROUND_ROBIN:
                allocateRoundRobin(connections);
                break;
            case SPEED_BASED:
                allocateBySpeed(connections);
                break;
            case LATENCY_BASED:
                allocateByLatency(connections);
                break;
            case ADAPTIVE:
            default:
                allocateAdaptively(connections);
                break;
        }
        
        // Log the allocations
        for (NetworkConnection conn : connections) {
            Log.d(TAG, "Network " + conn.getSsid() + " allocated " + 
                    String.format("%.2f%%", conn.getAllocationPercentage()));
        }
    }
    
    /**
     * Allocate traffic equally among all connections
     * 
     * @param connections List of network connections
     */
    private void allocateRoundRobin(List<NetworkConnection> connections) {
        double equalShare = 100.0 / connections.size();
        for (NetworkConnection conn : connections) {
            conn.setAllocationPercentage(equalShare);
        }
    }
    
    /**
     * Allocate traffic proportionally to the speed of each connection
     * 
     * @param connections List of network connections
     */
    private void allocateBySpeed(List<NetworkConnection> connections) {
        double totalSpeed = 0;
        
        // Calculate total speed
        for (NetworkConnection conn : connections) {
            totalSpeed += conn.getSpeedMbps();
        }
        
        // Avoid division by zero
        if (totalSpeed <= 0) {
            allocateRoundRobin(connections);
            return;
        }
        
        // Allocate proportionally to speed
        for (NetworkConnection conn : connections) {
            double allocation = (conn.getSpeedMbps() / totalSpeed) * 100.0;
            conn.setAllocationPercentage(allocation);
        }
    }
    
    /**
     * Allocate traffic inversely proportional to latency
     * 
     * @param connections List of network connections
     */
    private void allocateByLatency(List<NetworkConnection> connections) {
        double totalInverseLatency = 0;
        
        // Calculate total inverse latency (lower latency = better)
        for (NetworkConnection conn : connections) {
            // Avoid division by zero by adding 1
            double inverseLatency = 1.0 / (conn.getLatencyMs() + 1);
            totalInverseLatency += inverseLatency;
        }
        
        // Avoid division by zero
        if (totalInverseLatency <= 0) {
            allocateRoundRobin(connections);
            return;
        }
        
        // Allocate inversely proportional to latency
        for (NetworkConnection conn : connections) {
            double inverseLatency = 1.0 / (conn.getLatencyMs() + 1);
            double allocation = (inverseLatency / totalInverseLatency) * 100.0;
            conn.setAllocationPercentage(allocation);
        }
    }
    
    /**
     * Allocate traffic adaptively based on both speed and latency
     * 
     * @param connections List of network connections
     */
    private void allocateAdaptively(List<NetworkConnection> connections) {
        double totalScore = 0;
        
        // Calculate a score for each connection based on speed and latency
        for (NetworkConnection conn : connections) {
            double score = calculateAdaptiveScore(conn);
            totalScore += score;
        }
        
        // Avoid division by zero
        if (totalScore <= 0) {
            allocateRoundRobin(connections);
            return;
        }
        
        // Allocate based on scores
        for (NetworkConnection conn : connections) {
            double score = calculateAdaptiveScore(conn);
            double allocation = (score / totalScore) * 100.0;
            conn.setAllocationPercentage(allocation);
        }
    }
    
    /**
     * Calculates a score for adaptive allocation based on speed and latency
     * 
     * @param conn The network connection
     * @return Score value
     */
    private double calculateAdaptiveScore(NetworkConnection conn) {
        // Speed is good, latency is bad
        // Add 1 to latency to avoid division by zero
        double latencyFactor = 100.0 / (conn.getLatencyMs() + 1);
        
        // Give more weight to speed
        return (conn.getSpeedMbps() * 0.7) + (latencyFactor * 0.3);
    }
}
