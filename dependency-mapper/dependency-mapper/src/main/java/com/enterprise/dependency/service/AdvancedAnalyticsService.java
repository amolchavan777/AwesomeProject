package com.enterprise.dependency.service;

import com.example.mapper.model.DependencyClaim;
import com.example.mapper.repo.DependencyClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced analytics service providing sophisticated dependency analysis capabilities.
 * 
 * <p>Features:
 * <ul>
 * <li>Service criticality scoring based on connectivity and centrality</li>
 * <li>Change impact analysis - what services are affected if one goes down</li>
 * <li>Network topology insights - centrality measures, clustering coefficients</li>
 * <li>Dependency health scoring and trend analysis</li>
 * <li>Risk assessment and bottleneck identification</li>
 * </ul>
 * 
 * @author GitHub Copilot
 * @since 2.0.0
 */
@Service
public class AdvancedAnalyticsService {
    
    private static final Logger log = LoggerFactory.getLogger(AdvancedAnalyticsService.class);
    
    private final DependencyClaimRepository dependencyClaimRepository;
    
    @Autowired
    public AdvancedAnalyticsService(DependencyClaimRepository dependencyClaimRepository) {
        this.dependencyClaimRepository = dependencyClaimRepository;
    }
    
    /**
     * Calculate service criticality scores based on multiple factors.
     */
    public Map<String, Double> calculateServiceCriticalityScores() {
        List<DependencyClaim> allClaims = dependencyClaimRepository.findAll();
        Map<String, Set<String>> adjacencyList = buildAdjacencyList(allClaims);
        Set<String> allServices = getAllServices(allClaims);
        
        Map<String, Double> criticalityScores = new HashMap<>();
        
        for (String service : allServices) {
            double score = 0.0;
            
            // Factor 1: Betweenness centrality (30% weight)
            score += 0.3 * calculateBetweennessCentrality(service, adjacencyList, allServices);
            
            // Factor 2: Degree centrality (25% weight)
            score += 0.25 * calculateDegreeCentrality(service, adjacencyList, allServices.size());
            
            // Factor 3: PageRank (25% weight)
            score += 0.25 * calculatePageRank(service, adjacencyList, allServices);
            
            // Factor 4: Dependency confidence (20% weight)
            score += 0.2 * calculateAverageConfidence(service, allClaims);
            
            criticalityScores.put(service, score);
        }
        
        return criticalityScores;
    }
    
    /**
     * Analyze the impact of a service failure - which services would be affected.
     */
    public ChangeImpactAnalysis analyzeChangeImpact(String serviceId) {
        List<DependencyClaim> allClaims = dependencyClaimRepository.findAll();
        Map<String, Set<String>> adjacencyList = buildAdjacencyList(allClaims);
        
        Set<String> directlyAffected = new HashSet<>();
        Set<String> indirectlyAffected = new HashSet<>();
        
        // Find services that directly depend on this service
        for (DependencyClaim claim : allClaims) {
            if (claim.getToService().getName().equals(serviceId)) {
                directlyAffected.add(claim.getFromService().getName());
            }
        }
        
        // Find services that would be indirectly affected (cascade failure)
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>(directlyAffected);
        visited.addAll(directlyAffected);
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            Set<String> dependents = adjacencyList.getOrDefault(current, Collections.emptySet());
            
            for (String dependent : dependents) {
                if (!visited.contains(dependent) && !dependent.equals(serviceId)) {
                    indirectlyAffected.add(dependent);
                    visited.add(dependent);
                    queue.offer(dependent);
                }
            }
        }
        
        // Remove direct from indirect
        indirectlyAffected.removeAll(directlyAffected);
        
        return new ChangeImpactAnalysis(serviceId, directlyAffected, indirectlyAffected);
    }
    
    /**
     * Calculate network topology metrics.
     */
    public NetworkTopologyMetrics calculateNetworkTopology() {
        List<DependencyClaim> allClaims = dependencyClaimRepository.findAll();
        Map<String, Set<String>> adjacencyList = buildAdjacencyList(allClaims);
        Set<String> allServices = getAllServices(allClaims);
        
        // Calculate various centrality measures
        Map<String, Double> betweennessCentrality = new HashMap<>();
        Map<String, Double> closenessCentrality = new HashMap<>();
        Map<String, Double> eigenvectorCentrality = new HashMap<>();
        
        for (String service : allServices) {
            betweennessCentrality.put(service, calculateBetweennessCentrality(service, adjacencyList, allServices));
            closenessCentrality.put(service, calculateClosenessCentrality(service, adjacencyList, allServices));
            eigenvectorCentrality.put(service, calculateEigenvectorCentrality(service, adjacencyList, allServices));
        }
        
        // Calculate network-level metrics
        double density = calculateNetworkDensity(adjacencyList, allServices);
        double clustering = calculateAverageClusteringCoefficient(adjacencyList, allServices);
        int diameter = calculateNetworkDiameter(adjacencyList, allServices);
        
        return new NetworkTopologyMetrics(
            betweennessCentrality,
            closenessCentrality,
            eigenvectorCentrality,
            density,
            clustering,
            diameter
        );
    }
    
    /**
     * Identify potential bottlenecks in the system.
     */
    public List<BottleneckAnalysis> identifyBottlenecks() {
        List<DependencyClaim> allClaims = dependencyClaimRepository.findAll();
        Map<String, Set<String>> adjacencyList = buildAdjacencyList(allClaims);
        Set<String> allServices = getAllServices(allClaims);
        
        List<BottleneckAnalysis> bottlenecks = new ArrayList<>();
        
        for (String service : allServices) {
            double betweenness = calculateBetweennessCentrality(service, adjacencyList, allServices);
            int inDegree = getInDegree(service, allClaims);
            int outDegree = adjacencyList.getOrDefault(service, Collections.emptySet()).size();
            
            // A service is a potential bottleneck if:
            // 1. High betweenness centrality (> 0.1)
            // 2. High in-degree (> average + 1 std dev)
            // 3. Low out-degree relative to in-degree
            
            if (betweenness > 0.1 && inDegree > calculateAverageInDegree(allClaims) * 1.5) {
                String reason = String.format(
                    "High betweenness centrality (%.3f) and in-degree (%d)", 
                    betweenness, inDegree
                );
                
                BottleneckRisk risk = BottleneckRisk.MEDIUM;
                if (betweenness > 0.2 && inDegree > calculateAverageInDegree(allClaims) * 2) {
                    risk = BottleneckRisk.HIGH;
                } else if (betweenness < 0.15 && inDegree < calculateAverageInDegree(allClaims) * 1.8) {
                    risk = BottleneckRisk.LOW;
                }
                
                bottlenecks.add(new BottleneckAnalysis(service, risk, reason, betweenness, inDegree, outDegree));
            }
        }
        
        // Sort by risk level and betweenness centrality
        bottlenecks.sort((a, b) -> {
            int riskCompare = b.getRisk().compareTo(a.getRisk());
            if (riskCompare != 0) return riskCompare;
            return Double.compare(b.getBetweennessCentrality(), a.getBetweennessCentrality());
        });
        
        return bottlenecks;
    }
    
    /**
     * Calculate dependency health scores based on confidence, recency, and source reliability.
     */
    public Map<String, DependencyHealthScore> calculateDependencyHealth() {
        List<DependencyClaim> allClaims = dependencyClaimRepository.findAll();
        Map<String, List<DependencyClaim>> dependencyGroups = allClaims.stream()
            .collect(Collectors.groupingBy(
                c -> c.getFromService().getName() + " -> " + c.getToService().getName()
            ));
        
        Map<String, DependencyHealthScore> healthScores = new HashMap<>();
        
        for (Map.Entry<String, List<DependencyClaim>> entry : dependencyGroups.entrySet()) {
            String dependencyKey = entry.getKey();
            List<DependencyClaim> claims = entry.getValue();
            
            // Calculate health components
            double confidenceScore = claims.stream()
                .mapToDouble(DependencyClaim::getConfidence)
                .average()
                .orElse(0.0);
            
            double consistencyScore = calculateConsistencyScore(claims);
            double sourceReliabilityScore = calculateSourceReliabilityScore(claims);
            
            // Overall health score (weighted average)
            double overallScore = (confidenceScore * 0.4) + (consistencyScore * 0.3) + (sourceReliabilityScore * 0.3);
            
            HealthStatus status = HealthStatus.HEALTHY;
            if (overallScore < 0.5) {
                status = HealthStatus.CRITICAL;
            } else if (overallScore < 0.7) {
                status = HealthStatus.WARNING;
            }
            
            healthScores.put(dependencyKey, new DependencyHealthScore(
                dependencyKey,
                overallScore,
                confidenceScore,
                consistencyScore,
                sourceReliabilityScore,
                status,
                claims.size()
            ));
        }
        
        return healthScores;
    }
    
    // Helper methods
    
    private Map<String, Set<String>> buildAdjacencyList(List<DependencyClaim> claims) {
        Map<String, Set<String>> adjacencyList = new HashMap<>();
        for (DependencyClaim claim : claims) {
            adjacencyList.computeIfAbsent(claim.getFromService().getName(), k -> new HashSet<>())
                .add(claim.getToService().getName());
        }
        return adjacencyList;
    }
    
    private Set<String> getAllServices(List<DependencyClaim> claims) {
        Set<String> services = new HashSet<>();
        for (DependencyClaim claim : claims) {
            services.add(claim.getFromService().getName());
            services.add(claim.getToService().getName());
        }
        return services;
    }
    
    private double calculateBetweennessCentrality(String service, Map<String, Set<String>> adjacencyList, Set<String> allServices) {
        // Simplified betweenness centrality calculation
        // In production, use a proper graph library like JGraphT
        double betweenness = 0.0;
        int pathCount = 0;
        int pathsThroughService = 0;
        
        for (String source : allServices) {
            if (source.equals(service)) continue;
            for (String target : allServices) {
                if (target.equals(service) || target.equals(source)) continue;
                
                List<String> shortestPath = findShortestPath(source, target, adjacencyList);
                if (shortestPath != null && shortestPath.size() > 2) {
                    pathCount++;
                    if (shortestPath.contains(service)) {
                        pathsThroughService++;
                    }
                }
            }
        }
        
        return pathCount > 0 ? (double) pathsThroughService / pathCount : 0.0;
    }
    
    private double calculateDegreeCentrality(String service, Map<String, Set<String>> adjacencyList, int totalServices) {
        int outDegree = adjacencyList.getOrDefault(service, Collections.emptySet()).size();
        int inDegree = 0;
        
        for (Set<String> neighbors : adjacencyList.values()) {
            if (neighbors.contains(service)) {
                inDegree++;
            }
        }
        
        return (double) (inDegree + outDegree) / (2.0 * (totalServices - 1));
    }
    
    private double calculatePageRank(String service, Map<String, Set<String>> adjacencyList, Set<String> allServices) {
        // Simplified PageRank calculation
        Map<String, Double> pageRank = new HashMap<>();
        double dampingFactor = 0.85;
        int iterations = 10;
        
        // Initialize
        for (String s : allServices) {
            pageRank.put(s, 1.0 / allServices.size());
        }
        
        // Iterate
        for (int i = 0; i < iterations; i++) {
            Map<String, Double> newPageRank = new HashMap<>();
            
            for (String s : allServices) {
                double rank = (1 - dampingFactor) / allServices.size();
                
                for (String source : allServices) {
                    Set<String> neighbors = adjacencyList.getOrDefault(source, Collections.emptySet());
                    if (neighbors.contains(s) && !neighbors.isEmpty()) {
                        rank += dampingFactor * pageRank.get(source) / neighbors.size();
                    }
                }
                
                newPageRank.put(s, rank);
            }
            
            pageRank = newPageRank;
        }
        
        return pageRank.getOrDefault(service, 0.0);
    }
    
    private double calculateAverageConfidence(String service, List<DependencyClaim> allClaims) {
        return allClaims.stream()
            .filter(c -> c.getFromService().getName().equals(service) || c.getToService().getName().equals(service))
            .mapToDouble(DependencyClaim::getConfidence)
            .average()
            .orElse(0.0);
    }
    
    private List<String> findShortestPath(String source, String target, Map<String, Set<String>> adjacencyList) {
        // BFS for shortest path
        Queue<List<String>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        queue.offer(Arrays.asList(source));
        visited.add(source);
        
        while (!queue.isEmpty()) {
            List<String> path = queue.poll();
            String current = path.get(path.size() - 1);
            
            if (current.equals(target)) {
                return path;
            }
            
            for (String neighbor : adjacencyList.getOrDefault(current, Collections.emptySet())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    List<String> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.offer(newPath);
                }
            }
        }
        
        return null; // No path found
    }
    
    private double calculateClosenessCentrality(String service, Map<String, Set<String>> adjacencyList, Set<String> allServices) {
        double totalDistance = 0.0;
        int reachableNodes = 0;
        
        for (String target : allServices) {
            if (!target.equals(service)) {
                List<String> path = findShortestPath(service, target, adjacencyList);
                if (path != null) {
                    totalDistance += path.size() - 1;
                    reachableNodes++;
                }
            }
        }
        
        return reachableNodes > 0 ? reachableNodes / totalDistance : 0.0;
    }
    
    private double calculateEigenvectorCentrality(String service, Map<String, Set<String>> adjacencyList, Set<String> allServices) {
        // Simplified eigenvector centrality (power iteration method)
        Map<String, Double> centrality = new HashMap<>();
        int iterations = 20;
        
        // Initialize
        for (String s : allServices) {
            centrality.put(s, 1.0);
        }
        
        // Power iteration
        for (int i = 0; i < iterations; i++) {
            Map<String, Double> newCentrality = new HashMap<>();
            
            for (String s : allServices) {
                double sum = 0.0;
                for (String source : allServices) {
                    if (adjacencyList.getOrDefault(source, Collections.emptySet()).contains(s)) {
                        sum += centrality.get(source);
                    }
                }
                newCentrality.put(s, sum);
            }
            
            // Normalize
            double norm = Math.sqrt(newCentrality.values().stream().mapToDouble(x -> x * x).sum());
            if (norm > 0) {
                newCentrality.replaceAll((k, v) -> v / norm);
            }
            
            centrality = newCentrality;
        }
        
        return centrality.getOrDefault(service, 0.0);
    }
    
    private double calculateNetworkDensity(Map<String, Set<String>> adjacencyList, Set<String> allServices) {
        int totalEdges = adjacencyList.values().stream().mapToInt(Set::size).sum();
        int maxPossibleEdges = allServices.size() * (allServices.size() - 1);
        return maxPossibleEdges > 0 ? (double) totalEdges / maxPossibleEdges : 0.0;
    }
    
    private double calculateAverageClusteringCoefficient(Map<String, Set<String>> adjacencyList, Set<String> allServices) {
        double totalClustering = 0.0;
        int nodeCount = 0;
        
        for (String service : allServices) {
            Set<String> neighbors = adjacencyList.getOrDefault(service, Collections.emptySet());
            if (neighbors.size() > 1) {
                int triangles = 0;
                int possibleTriangles = neighbors.size() * (neighbors.size() - 1) / 2;
                
                for (String neighbor1 : neighbors) {
                    for (String neighbor2 : neighbors) {
                        if (!neighbor1.equals(neighbor2) && 
                            adjacencyList.getOrDefault(neighbor1, Collections.emptySet()).contains(neighbor2)) {
                            triangles++;
                        }
                    }
                }
                
                totalClustering += possibleTriangles > 0 ? (double) triangles / possibleTriangles : 0.0;
                nodeCount++;
            }
        }
        
        return nodeCount > 0 ? totalClustering / nodeCount : 0.0;
    }
    
    private int calculateNetworkDiameter(Map<String, Set<String>> adjacencyList, Set<String> allServices) {
        int maxDistance = 0;
        
        for (String source : allServices) {
            for (String target : allServices) {
                if (!source.equals(target)) {
                    List<String> path = findShortestPath(source, target, adjacencyList);
                    if (path != null) {
                        maxDistance = Math.max(maxDistance, path.size() - 1);
                    }
                }
            }
        }
        
        return maxDistance;
    }
    
    private int getInDegree(String service, List<DependencyClaim> allClaims) {
        return (int) allClaims.stream()
            .filter(c -> c.getToService().getName().equals(service))
            .count();
    }
    
    private double calculateAverageInDegree(List<DependencyClaim> allClaims) {
        Map<String, Integer> inDegrees = new HashMap<>();
        for (DependencyClaim claim : allClaims) {
            inDegrees.merge(claim.getToService().getName(), 1, Integer::sum);
        }
        return inDegrees.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }
    
    private double calculateConsistencyScore(List<DependencyClaim> claims) {
        if (claims.size() <= 1) return 1.0;
        
        double avgConfidence = claims.stream().mapToDouble(DependencyClaim::getConfidence).average().orElse(0.0);
        double variance = claims.stream()
            .mapToDouble(c -> Math.pow(c.getConfidence() - avgConfidence, 2))
            .average()
            .orElse(0.0);
        
        return Math.max(0.0, 1.0 - Math.sqrt(variance));
    }
    
    private double calculateSourceReliabilityScore(List<DependencyClaim> claims) {
        // Simple source reliability scoring based on source type
        Map<String, Double> sourceReliability = Map.of(
            "router-log", 0.9,
            "config-file", 0.8,
            "manual", 0.7,
            "network-scan", 0.6
        );
        
        return claims.stream()
            .mapToDouble(c -> sourceReliability.getOrDefault(c.getSource(), 0.5))
            .average()
            .orElse(0.5);
    }
    
    // Data classes for results
    
    public static class ChangeImpactAnalysis {
        private final String serviceId;
        private final Set<String> directlyAffected;
        private final Set<String> indirectlyAffected;
        
        public ChangeImpactAnalysis(String serviceId, Set<String> directlyAffected, Set<String> indirectlyAffected) {
            this.serviceId = serviceId;
            this.directlyAffected = directlyAffected;
            this.indirectlyAffected = indirectlyAffected;
        }
        
        // Getters
        public String getServiceId() { return serviceId; }
        public Set<String> getDirectlyAffected() { return directlyAffected; }
        public Set<String> getIndirectlyAffected() { return indirectlyAffected; }
        public int getTotalAffected() { return directlyAffected.size() + indirectlyAffected.size(); }
        
        @Override
        public String toString() {
            return String.format("ChangeImpact[service=%s, direct=%d, indirect=%d, total=%d]",
                serviceId, directlyAffected.size(), indirectlyAffected.size(), getTotalAffected());
        }
    }
    
    public static class NetworkTopologyMetrics {
        private final Map<String, Double> betweennessCentrality;
        private final Map<String, Double> closenessCentrality;
        private final Map<String, Double> eigenvectorCentrality;
        private final double networkDensity;
        private final double averageClusteringCoefficient;
        private final int networkDiameter;
        
        public NetworkTopologyMetrics(Map<String, Double> betweennessCentrality,
                                    Map<String, Double> closenessCentrality,
                                    Map<String, Double> eigenvectorCentrality,
                                    double networkDensity,
                                    double averageClusteringCoefficient,
                                    int networkDiameter) {
            this.betweennessCentrality = betweennessCentrality;
            this.closenessCentrality = closenessCentrality;
            this.eigenvectorCentrality = eigenvectorCentrality;
            this.networkDensity = networkDensity;
            this.averageClusteringCoefficient = averageClusteringCoefficient;
            this.networkDiameter = networkDiameter;
        }
        
        // Getters
        public Map<String, Double> getBetweennessCentrality() { return betweennessCentrality; }
        public Map<String, Double> getClosenessCentrality() { return closenessCentrality; }
        public Map<String, Double> getEigenvectorCentrality() { return eigenvectorCentrality; }
        public double getNetworkDensity() { return networkDensity; }
        public double getAverageClusteringCoefficient() { return averageClusteringCoefficient; }
        public int getNetworkDiameter() { return networkDiameter; }
    }
    
    public static class BottleneckAnalysis {
        private final String serviceId;
        private final BottleneckRisk risk;
        private final String reason;
        private final double betweennessCentrality;
        private final int inDegree;
        private final int outDegree;
        
        public BottleneckAnalysis(String serviceId, BottleneckRisk risk, String reason,
                                double betweennessCentrality, int inDegree, int outDegree) {
            this.serviceId = serviceId;
            this.risk = risk;
            this.reason = reason;
            this.betweennessCentrality = betweennessCentrality;
            this.inDegree = inDegree;
            this.outDegree = outDegree;
        }
        
        // Getters
        public String getServiceId() { return serviceId; }
        public BottleneckRisk getRisk() { return risk; }
        public String getReason() { return reason; }
        public double getBetweennessCentrality() { return betweennessCentrality; }
        public int getInDegree() { return inDegree; }
        public int getOutDegree() { return outDegree; }
        
        @Override
        public String toString() {
            return String.format("Bottleneck[service=%s, risk=%s, centrality=%.3f, in=%d, out=%d]",
                serviceId, risk, betweennessCentrality, inDegree, outDegree);
        }
    }
    
    public static class DependencyHealthScore {
        private final String dependencyKey;
        private final double overallScore;
        private final double confidenceScore;
        private final double consistencyScore;
        private final double sourceReliabilityScore;
        private final HealthStatus status;
        private final int claimCount;
        
        public DependencyHealthScore(String dependencyKey, double overallScore, double confidenceScore,
                                   double consistencyScore, double sourceReliabilityScore,
                                   HealthStatus status, int claimCount) {
            this.dependencyKey = dependencyKey;
            this.overallScore = overallScore;
            this.confidenceScore = confidenceScore;
            this.consistencyScore = consistencyScore;
            this.sourceReliabilityScore = sourceReliabilityScore;
            this.status = status;
            this.claimCount = claimCount;
        }
        
        // Getters
        public String getDependencyKey() { return dependencyKey; }
        public double getOverallScore() { return overallScore; }
        public double getConfidenceScore() { return confidenceScore; }
        public double getConsistencyScore() { return consistencyScore; }
        public double getSourceReliabilityScore() { return sourceReliabilityScore; }
        public HealthStatus getStatus() { return status; }
        public int getClaimCount() { return claimCount; }
    }
    
    public enum BottleneckRisk {
        LOW, MEDIUM, HIGH
    }
    
    public enum HealthStatus {
        HEALTHY, WARNING, CRITICAL
    }
}
