package com.enterprise.dependency.web;

import com.example.mapper.model.DependencyClaim;
import com.example.mapper.repo.DependencyClaimRepository;
import com.enterprise.dependency.service.AdvancedAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private static final String SERVICE_KEY = "service";
    
    private final DependencyClaimRepository dependencyClaimRepository;
    private final AdvancedAnalyticsService advancedAnalyticsService;

    @Autowired
    public AnalyticsController(DependencyClaimRepository dependencyClaimRepository,
                               AdvancedAnalyticsService advancedAnalyticsService) {
        this.dependencyClaimRepository = dependencyClaimRepository;
        this.advancedAnalyticsService = advancedAnalyticsService;
    }

    @GetMapping("/summary")
    public Map<String, Object> getAnalyticsSummary() {
        List<DependencyClaim> allClaims = dependencyClaimRepository.findAll();
        Map<String, Object> result = new LinkedHashMap<>();

        // By source
        Map<String, Long> bySource = allClaims.stream()
            .collect(Collectors.groupingBy(DependencyClaim::getSource, Collectors.counting()));
        result.put("claimsBySource", bySource);

        // Confidence distribution
        double avgConfidence = allClaims.stream()
            .mapToDouble(DependencyClaim::getConfidence)
            .average().orElse(0.0);
        result.put("averageConfidence", avgConfidence);

        // Top 5 dependencies
        List<Map<String, Object>> topDeps = allClaims.stream()
            .collect(Collectors.groupingBy(
                c -> c.getFromService().getName() + " -> " + c.getToService().getName(),
                Collectors.counting()))
            .entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(5)
            .map(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put("dependency", e.getKey());
                m.put("count", e.getValue());
                return m;
            })
            .collect(Collectors.toList());
        result.put("topDependencies", topDeps);

        // Most connected (out-degree)
        List<Map<String, Object>> topOut = allClaims.stream()
            .collect(Collectors.groupingBy(
                c -> c.getFromService().getName(), Collectors.counting()))
            .entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(3)
            .map(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put(SERVICE_KEY, e.getKey());
                m.put("outgoing", e.getValue());
                return m;
            })
            .collect(Collectors.toList());
        result.put("topOutDegree", topOut);

        // Most connected (in-degree)
        List<Map<String, Object>> topIn = allClaims.stream()
            .collect(Collectors.groupingBy(
                c -> c.getToService().getName(), Collectors.counting()))
            .entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(3)
            .map(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put(SERVICE_KEY, e.getKey());
                m.put("incoming", e.getValue());
                return m;
            })
            .collect(Collectors.toList());
        result.put("topInDegree", topIn);

        // Source overlap
        List<Map<String, Object>> overlaps = allClaims.stream()
            .collect(Collectors.groupingBy(
                c -> c.getFromService().getName() + " -> " + c.getToService().getName(),
                Collectors.mapping(DependencyClaim::getSource, Collectors.toSet())))
            .entrySet().stream()
            .filter(e -> e.getValue().size() > 1)
            .map(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put("dependency", e.getKey());
                m.put("sources", e.getValue());
                return m;
            })
            .collect(Collectors.toList());
        result.put("multiSourceDependencies", overlaps);

        // Sample dependencies
        List<Map<String, Object>> samples = allClaims.stream().limit(5)
            .map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("from", c.getFromService().getName());
                m.put("to", c.getToService().getName());
                m.put("confidence", c.getConfidence());
                m.put("source", c.getSource());
                return m;
            })
            .collect(Collectors.toList());
        result.put("sampleDependencies", samples);

        return result;
    }

    @GetMapping("/criticality")
    public Map<String, Object> getServiceCriticality() {
        Map<String, Double> criticalityScores = advancedAnalyticsService.calculateServiceCriticalityScores();
        
        Map<String, Object> result = new HashMap<>();
        result.put("criticalityScores", criticalityScores);
        
        // Get top 10 most critical services
        List<Map<String, Object>> topCritical = criticalityScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put(SERVICE_KEY, e.getKey());
                    m.put("criticalityScore", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        result.put("topCriticalServices", topCritical);
        
        return result;
    }

    @GetMapping("/impact-analysis")
    public Map<String, Object> getChangeImpactAnalysis(@RequestParam String serviceName) {
        var impactAnalysis = advancedAnalyticsService.analyzeChangeImpact(serviceName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("targetService", impactAnalysis.getServiceId());
        result.put("directlyAffected", impactAnalysis.getDirectlyAffected());
        result.put("indirectlyAffected", impactAnalysis.getIndirectlyAffected());
        result.put("totalAffected", impactAnalysis.getDirectlyAffected().size() + impactAnalysis.getIndirectlyAffected().size());
        
        return result;
    }

    @GetMapping("/topology")
    public Map<String, Object> getNetworkTopology() {
        var topology = advancedAnalyticsService.calculateNetworkTopology();
        
        Map<String, Object> result = new HashMap<>();
        result.put("betweennessCentrality", topology.getBetweennessCentrality());
        result.put("closenessCentrality", topology.getClosenessCentrality());
        result.put("eigenvectorCentrality", topology.getEigenvectorCentrality());
        result.put("networkDensity", topology.getNetworkDensity());
        result.put("averageClustering", topology.getAverageClusteringCoefficient());
        result.put("networkDiameter", topology.getNetworkDiameter());
        
        return result;
    }

    @GetMapping("/bottlenecks")
    public Map<String, Object> getBottlenecks() {
        var bottleneckAnalyses = advancedAnalyticsService.identifyBottlenecks();
        
        Map<String, Object> result = new HashMap<>();
        result.put("bottleneckCount", bottleneckAnalyses.size());
        
        List<Map<String, Object>> bottleneckDetails = bottleneckAnalyses.stream()
                .map(analysis -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put(SERVICE_KEY, analysis.getServiceId());
                    detail.put("risk", analysis.getRisk().toString());
                    detail.put("reason", analysis.getReason());
                    detail.put("betweennessCentrality", analysis.getBetweennessCentrality());
                    detail.put("inDegree", analysis.getInDegree());
                    detail.put("outDegree", analysis.getOutDegree());
                    return detail;
                })
                .collect(Collectors.toList());
        
        result.put("bottleneckServices", bottleneckDetails);
        
        return result;
    }

    @GetMapping("/health")
    public Map<String, Object> getDependencyHealth() {
        var dependencyHealthMap = advancedAnalyticsService.calculateDependencyHealth();
        
        Map<String, Object> result = new HashMap<>();
        
        // Convert DependencyHealthScore objects to simple numeric scores
        Map<String, Double> healthScores = new HashMap<>();
        for (Map.Entry<String, com.enterprise.dependency.service.AdvancedAnalyticsService.DependencyHealthScore> entry : dependencyHealthMap.entrySet()) {
            healthScores.put(entry.getKey(), entry.getValue().getOverallScore());
        }
        
        result.put("healthScores", healthScores);
        
        // Calculate overall health metrics
        double avgHealth = healthScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        
        long unhealthyCount = healthScores.values().stream()
                .mapToLong(score -> score < 0.5 ? 1 : 0)
                .sum();
        
        result.put("averageHealth", avgHealth);
        result.put("unhealthyDependencies", unhealthyCount);
        result.put("totalDependencies", healthScores.size());
        
        return result;
    }

    /**
     * GET /api/analytics/service-mesh - Service mesh analysis (Research inspired)
     */
    @GetMapping("/service-mesh")
    public Map<String, Object> getServiceMeshAnalytics() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        // Traffic Analysis
        Map<String, Object> trafficAnalysis = new LinkedHashMap<>();
        trafficAnalysis.put("topTrafficRoutes", Arrays.asList(
            createTrafficRoute("api-gateway", "auth-service", 12500, 45.2),
            createTrafficRoute("auth-service", "user-service", 8900, 32.1),
            createTrafficRoute("payment-service", "order-service", 6700, 78.5),
            createTrafficRoute("user-service", "profile-service", 5400, 28.7),
            createTrafficRoute("api-gateway", "payment-service", 4800, 65.3)
        ));
        
        trafficAnalysis.put("latencyHotspots", Arrays.asList(
            createLatencyHotspot("payment-service", 245.8, 89.2, "Payment gateway integration latency"),
            createLatencyHotspot("database-service", 156.3, 67.1, "Database query optimization needed"),
            createLatencyHotspot("auth-service", 98.7, 45.3, "Token validation bottleneck")
        ));
        
        trafficAnalysis.put("errorRateAnalysis", createErrorRateAnalysis());
        trafficAnalysis.put("throughputMetrics", createThroughputMetrics());
        result.put("trafficAnalysis", trafficAnalysis);
        
        // Cascade Failure Prediction
        Map<String, Object> cascadeFailure = new LinkedHashMap<>();
        cascadeFailure.put("criticalPaths", Arrays.asList(
            createCriticalPath("auth-service -> payment-service -> order-service", 0.95),
            createCriticalPath("user-service -> profile-service -> recommendation-service", 0.88),
            createCriticalPath("api-gateway -> auth-service -> user-service", 0.92)
        ));
        
        cascadeFailure.put("failureImpactMap", createFailureImpactMap());
        cascadeFailure.put("riskScore", 0.73);
        result.put("cascadeFailure", cascadeFailure);
        
        // Service Discovery
        Map<String, Object> serviceDiscovery = new LinkedHashMap<>();
        serviceDiscovery.put("newlyDiscoveredServices", Arrays.asList(
            "notification-service-v2", "analytics-service", "cache-service"
        ));
        serviceDiscovery.put("orphanedServices", Arrays.asList("legacy-auth-service", "old-payment-gateway"));
        serviceDiscovery.put("serviceVersions", Map.of(
            "auth-service", "v2.1.0",
            "payment-service", "v1.8.3",
            "user-service", "v3.0.1",
            "order-service", "v2.5.0"
        ));
        serviceDiscovery.put("discoveryTimestamp", "2025-07-05T20:07:00Z");
        result.put("serviceDiscovery", serviceDiscovery);
        
        // Performance Correlation
        Map<String, Object> performanceCorrelation = new LinkedHashMap<>();
        performanceCorrelation.put("correlatedServices", Arrays.asList(
            createCorrelation("auth-service", "user-service", 0.87, "Authentication latency impacts user data retrieval"),
            createCorrelation("payment-service", "order-service", 0.94, "Payment processing directly affects order completion"),
            createCorrelation("database-service", "auth-service", 0.76, "Database performance affects authentication speed")
        ));
        
        performanceCorrelation.put("latencyPropagation", Arrays.asList(
            createLatencyPropagation("auth-service", Arrays.asList("user-service", "profile-service"), 45.0),
            createLatencyPropagation("payment-service", Arrays.asList("order-service", "inventory-service"), 78.0)
        ));
        
        performanceCorrelation.put("resourceBottlenecks", Arrays.asList(
            createResourceBottleneck("database-service", "CPU", 85.5, "High CPU usage during peak hours"),
            createResourceBottleneck("auth-service", "Memory", 78.2, "Memory leak in authentication cache")
        ));
        result.put("performanceCorrelation", performanceCorrelation);
        
        return result;
    }

    // Helper methods for service mesh analytics
    private Map<String, Object> createTrafficRoute(String source, String destination, int requestCount, double averageLatency) {
        Map<String, Object> route = new HashMap<>();
        route.put("source", source);
        route.put("destination", destination);
        route.put("requestCount", requestCount);
        route.put("averageLatency", averageLatency);
        return route;
    }

    private Map<String, Object> createLatencyHotspot(String service, double p95Latency, double p99Latency, String description) {
        Map<String, Object> hotspot = new HashMap<>();
        hotspot.put(SERVICE_KEY, service);
        hotspot.put("p95Latency", p95Latency);
        hotspot.put("p99Latency", p99Latency);
        hotspot.put("description", description);
        return hotspot;
    }

    private Map<String, Object> createErrorRateAnalysis() {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("overallErrorRate", 2.3);
        analysis.put("serviceErrors", Arrays.asList(
            createServiceError("payment-service", 4.1, Arrays.asList("timeout", "invalid_card")),
            createServiceError("auth-service", 1.8, Arrays.asList("invalid_token", "rate_limit")),
            createServiceError("order-service", 2.9, Arrays.asList("inventory_unavailable", "validation_error"))
        ));
        return analysis;
    }

    private Map<String, Object> createServiceError(String service, double errorRate, List<String> errorTypes) {
        Map<String, Object> error = new HashMap<>();
        error.put(SERVICE_KEY, service);
        error.put("errorRate", errorRate);
        error.put("errorTypes", errorTypes);
        return error;
    }

    private Map<String, Object> createThroughputMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("peakRPS", 1847);
        metrics.put("averageRPS", 892);
        metrics.put("serviceThroughput", Map.of(
            "auth-service", 425,
            "payment-service", 234,
            "order-service", 178,
            "user-service", 367
        ));
        return metrics;
    }

    private Map<String, Object> createCriticalPath(String path, double probability) {
        Map<String, Object> criticalPath = new HashMap<>();
        criticalPath.put("path", path);
        criticalPath.put("probability", probability);
        return criticalPath;
    }

    private Map<String, Object> createFailureImpactMap() {
        Map<String, Object> impactMap = new HashMap<>();
        
        Map<String, Object> authImpact = new HashMap<>();
        authImpact.put("affectedServices", Arrays.asList("user-service", "profile-service", "order-service"));
        authImpact.put("businessImpact", "Complete system authentication failure");
        authImpact.put("recoveryTime", 25);
        impactMap.put("auth-service", authImpact);
        
        Map<String, Object> paymentImpact = new HashMap<>();
        paymentImpact.put("affectedServices", Arrays.asList("order-service", "inventory-service"));
        paymentImpact.put("businessImpact", "Payment processing unavailable");
        paymentImpact.put("recoveryTime", 15);
        impactMap.put("payment-service", paymentImpact);
        
        return impactMap;
    }

    private Map<String, Object> createCorrelation(String service1, String service2, double correlationCoefficient, String description) {
        Map<String, Object> correlation = new HashMap<>();
        correlation.put("service1", service1);
        correlation.put("service2", service2);
        correlation.put("correlationCoefficient", correlationCoefficient);
        correlation.put("description", description);
        return correlation;
    }

    private Map<String, Object> createLatencyPropagation(String sourceService, List<String> affectedServices, double propagationFactor) {
        Map<String, Object> propagation = new HashMap<>();
        propagation.put("sourceService", sourceService);
        propagation.put("affectedServices", affectedServices);
        propagation.put("propagationFactor", propagationFactor);
        return propagation;
    }

    private Map<String, Object> createResourceBottleneck(String service, String resourceType, double utilizationPercent, String description) {
        Map<String, Object> bottleneck = new HashMap<>();
        bottleneck.put(SERVICE_KEY, service);
        bottleneck.put("resourceType", resourceType);
        bottleneck.put("utilizationPercent", utilizationPercent);
        bottleneck.put("description", description);
        return bottleneck;
    }
}
