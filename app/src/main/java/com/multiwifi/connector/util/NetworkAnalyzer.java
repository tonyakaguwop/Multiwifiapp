package com.multiwifi.connector.util;

import com.multiwifi.connector.model.NetworkConnection;
import com.multiwifi.connector.model.NetworkRecommendation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class for analyzing networks and generating recommendations
 */
public class NetworkAnalyzer {
    
    private static final int MIN_NETWORKS = 1;
    private static final int MAX_NETWORKS = 5;
    private static final double SPEED_WEIGHT = 0.7;
    private static final double SIGNAL_WEIGHT = 0.3;
    
    /**
     * Generates recommendations based on available networks
     *
     * @param availableNetworks List of available networks
     * @return List of network recommendations
     */
    public List<NetworkRecommendation> generateRecommendations(List<NetworkConnection> availableNetworks) {
        List<NetworkRecommendation> recommendations = new ArrayList<>();
        
        if (availableNetworks == null || availableNetworks.isEmpty()) {
            return recommendations;
        }
        
        // Make a copy to avoid modifying the original list
        List<NetworkConnection> networks = new ArrayList<>(availableNetworks);
        
        // Sort networks by various criteria
        List<NetworkConnection> bySpeed = new ArrayList<>(networks);
        List<NetworkConnection> byReliability = new ArrayList<>(networks);
        List<NetworkConnection> byEfficiency = new ArrayList<>(networks);
        
        // Sort by speed (higher is better)
        Collections.sort(bySpeed, (n1, n2) -> Double.compare(n2.getSpeedMbps(), n1.getSpeedMbps()));
        
        // Sort by reliability (lower latency is better)
        Collections.sort(byReliability, Comparator.comparingInt(NetworkConnection::getLatencyMs));
        
        // Sort by efficiency (balance of speed and signal strength)
        Collections.sort(byEfficiency, (n1, n2) -> {
            double score1 = calculateEfficiencyScore(n1);
            double score2 = calculateEfficiencyScore(n2);
            return Double.compare(score2, score1);
        });
        
        // 1. Speed-optimized recommendation
        if (bySpeed.size() > 0) {
            List<NetworkConnection> speedNetworks = getTopNetworks(bySpeed, 3);
            NetworkRecommendation speedRec = new NetworkRecommendation(
                    NetworkRecommendation.RecommendationType.SPEED_OPTIMIZED,
                    speedNetworks);
            
            // Estimate performance
            speedRec.setEstimatedSpeed(estimateCombinedSpeed(speedNetworks));
            speedRec.setEstimatedReliability(estimateReliability(speedNetworks));
            speedRec.setEstimatedPowerUsage(0.8); // Speed optimized uses more power
            
            recommendations.add(speedRec);
        }
        
        // 2. Reliability-optimized recommendation
        if (byReliability.size() > 0) {
            List<NetworkConnection> reliabilityNetworks = getTopNetworks(byReliability, 2);
            NetworkRecommendation reliabilityRec = new NetworkRecommendation(
                    NetworkRecommendation.RecommendationType.RELIABILITY_OPTIMIZED, 
                    reliabilityNetworks);
            
            // Estimate performance
            reliabilityRec.setEstimatedSpeed(estimateCombinedSpeed(reliabilityNetworks));
            reliabilityRec.setEstimatedReliability(estimateReliability(reliabilityNetworks));
            reliabilityRec.setEstimatedPowerUsage(0.6); // Moderate power usage
            
            recommendations.add(reliabilityRec);
        }
        
        // 3. Balanced recommendation
        if (networks.size() > 0) {
            List<NetworkConnection> balancedNetworks = new ArrayList<>();
            
            // Pick top networks from different categories
            if (!bySpeed.isEmpty()) balancedNetworks.add(bySpeed.get(0));
            if (byReliability.size() > 1) balancedNetworks.add(byReliability.get(1));
            if (balancedNetworks.size() < 2 && networks.size() > 1) {
                for (NetworkConnection network : networks) {
                    if (!balancedNetworks.contains(network)) {
                        balancedNetworks.add(network);
                        break;
                    }
                }
            }
            
            NetworkRecommendation balancedRec = new NetworkRecommendation(
                    NetworkRecommendation.RecommendationType.BALANCED, 
                    balancedNetworks);
            
            // Estimate performance
            balancedRec.setEstimatedSpeed(estimateCombinedSpeed(balancedNetworks));
            balancedRec.setEstimatedReliability(estimateReliability(balancedNetworks));
            balancedRec.setEstimatedPowerUsage(0.5); // Moderate power usage
            
            recommendations.add(balancedRec);
        }
        
        // 4. Power-saving recommendation
        if (byEfficiency.size() > 0) {
            List<NetworkConnection> efficiencyNetworks = getTopNetworks(byEfficiency, 1);
            NetworkRecommendation efficiencyRec = new NetworkRecommendation(
                    NetworkRecommendation.RecommendationType.POWER_SAVING, 
                    efficiencyNetworks);
            
            // Estimate performance
            efficiencyRec.setEstimatedSpeed(estimateCombinedSpeed(efficiencyNetworks));
            efficiencyRec.setEstimatedReliability(estimateReliability(efficiencyNetworks));
            efficiencyRec.setEstimatedPowerUsage(0.2); // Low power usage
            
            recommendations.add(efficiencyRec);
        }
        
        // Mark the first recommendation as selected by default
        if (!recommendations.isEmpty()) {
            recommendations.get(0).setSelected(true);
        }
        
        return recommendations;
    }
    
    /**
     * Gets the top N networks from a list
     *
     * @param networks List of networks
     * @param count Number of networks to pick
     * @return List of top networks
     */
    private List<NetworkConnection> getTopNetworks(List<NetworkConnection> networks, int count) {
        int actualCount = Math.min(count, networks.size());
        actualCount = Math.max(MIN_NETWORKS, Math.min(actualCount, MAX_NETWORKS));
        
        List<NetworkConnection> result = new ArrayList<>();
        for (int i = 0; i < actualCount; i++) {
            result.add(networks.get(i));
        }
        
        return result;
    }
    
    /**
     * Calculates efficiency score for a network (balance of speed and battery usage)
     *
     * @param network The network to evaluate
     * @return Efficiency score
     */
    private double calculateEfficiencyScore(NetworkConnection network) {
        // Higher speed is better but consumes more power
        // Higher signal strength is better and consumes less power
        double speedScore = Math.min(1.0, network.getSpeedMbps() / 100.0);
        double signalScore = 1.0; // Placeholder for signal strength
        
        return (speedScore * SPEED_WEIGHT) + (signalScore * SIGNAL_WEIGHT);
    }
    
    /**
     * Estimates combined speed for multiple networks
     *
     * @param networks List of networks
     * @return Estimated combined speed in Mbps
     */
    private double estimateCombinedSpeed(List<NetworkConnection> networks) {
        double totalSpeed = 0;
        double efficiencyFactor = 0.85; // Networks don't combine with 100% efficiency
        
        for (NetworkConnection network : networks) {
            totalSpeed += network.getSpeedMbps();
        }
        
        // Apply diminishing returns for multiple networks
        if (networks.size() > 1) {
            totalSpeed *= Math.pow(efficiencyFactor, networks.size() - 1);
        }
        
        return totalSpeed;
    }
    
    /**
     * Estimates reliability for multiple networks
     *
     * @param networks List of networks
     * @return Reliability score (0.0-1.0)
     */
    private double estimateReliability(List<NetworkConnection> networks) {
        if (networks.isEmpty()) {
            return 0.0;
        }
        
        // Calculate average latency (lower is better)
        double totalLatency = 0;
        for (NetworkConnection network : networks) {
            totalLatency += network.getLatencyMs();
        }
        double avgLatency = totalLatency / networks.size();
        
        // Convert latency to reliability score (0.0-1.0)
        // Lower latency = higher reliability
        // 0ms -> 1.0, 100ms -> 0.9, 200ms -> 0.6, 300ms+ -> 0.3
        double reliability = Math.max(0.3, 1.0 - (avgLatency / 500.0));
        
        // Multiple networks improve reliability
        if (networks.size() > 1) {
            // Each additional network improves reliability up to a maximum
            double multiNetworkBonus = 0.1 * (networks.size() - 1);
            reliability = Math.min(0.99, reliability + multiNetworkBonus);
        }
        
        return reliability;
    }
}
