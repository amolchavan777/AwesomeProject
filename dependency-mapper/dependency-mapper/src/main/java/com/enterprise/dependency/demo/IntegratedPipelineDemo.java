package com.enterprise.dependency.demo;

import com.enterprise.dependency.service.IngestionService;
import com.enterprise.dependency.service.IngestionResult;
import com.enterprise.dependency.service.IngestionException;
import com.enterprise.dependency.service.ConflictDetectionService;
import com.enterprise.dependency.service.ConflictDetectionService.ConflictReport;
import com.example.mapper.model.DependencyClaim;
import com.example.mapper.repo.DependencyClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Comprehensive demo of the integrated Application Dependency Matrix system.
 * 
 * <p>Demonstrates:
 * <ul>
 * <li>Multi-source data ingestion (router logs, network scans, config files)</li>
 * <li>Claim normalization and deduplication</li>
 * <li>Conflict detection across sources</li>
 * <li>Data persistence and querying</li>
 * </ul>
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
@Component
@Order(100) // Run after other demos
public class IntegratedPipelineDemo implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(IntegratedPipelineDemo.class);
    
    private final IngestionService ingestionService;
    private final ConflictDetectionService conflictDetectionService;
    private final DependencyClaimRepository dependencyClaimRepository;
    
    @Autowired
    public IntegratedPipelineDemo(
            IngestionService ingestionService,
            ConflictDetectionService conflictDetectionService,
            DependencyClaimRepository dependencyClaimRepository) {
        this.ingestionService = ingestionService;
        this.conflictDetectionService = conflictDetectionService;
        this.dependencyClaimRepository = dependencyClaimRepository;
    }
    
    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && args[0].equals("--skip-demo")) {
            return;
        }
        
        log.info("=== Integrated Application Dependency Matrix Demo ===");
        
        try {
            // Demo 1: Multi-source ingestion
            demonstrateMultiSourceIngestion();
            
            // Demo 2: Conflict detection
            demonstrateConflictDetection();
            
            // Demo 3: Data querying and analysis
            demonstrateDataAnalysis();
            
            log.info("=== Demo Complete - System Ready for Production Use ===");
            
        } catch (Exception e) {
            log.error("Demo failed", e);
        }
    }
    
    private void demonstrateMultiSourceIngestion() throws IngestionException {
        log.info("--- Multi-Source Data Ingestion Demo ---");
        
        // Ingest from router logs
        String routerLogData = 
            "2024-07-05 10:30:45 [INFO] 192.168.1.100 -> 192.168.1.200:8080 GET /api/users 200 125ms\n" +
            "2024-07-05 10:30:46 [INFO] 192.168.1.200 -> 192.168.1.300:3306 SELECT users 200 45ms\n" +
            "2024-07-05 10:30:47 [INFO] 192.168.1.100 -> 192.168.1.400:9092 PUBLISH events 200 30ms";
        
        IngestionResult routerResult = ingestionService.ingestFromString(
            routerLogData, IngestionService.SourceType.ROUTER_LOG, "prod-router-1");
        log.info("Router logs: {}", routerResult);
        
        // Ingest from configuration files
        String configData = 
            "spring.datasource.url=jdbc:mysql://mysql-primary:3306/userdb\n" +
            "spring.kafka.bootstrap-servers=kafka-cluster:9092\n" +
            "service.user-api.url=http://user-service:8080\n" +
            "service.notification.url=http://notification-service:8081";
        
        IngestionResult configResult = ingestionService.ingestFromString(
            configData, IngestionService.SourceType.CONFIGURATION_FILE, "web-app-config");
        log.info("Configuration: {}", configResult);
        
        // Ingest conflicting data from another source
        String conflictingConfigData = 
            "spring.datasource.url=jdbc:mysql://mysql-backup:3306/userdb\n" +
            "spring.kafka.bootstrap-servers=kafka-backup:9092\n" +
            "service.user-api.url=http://user-service-v2:8080";
        
        IngestionResult conflictResult = ingestionService.ingestFromString(
            conflictingConfigData, IngestionService.SourceType.CONFIGURATION_FILE, "backup-config");
        log.info("Conflicting config: {}", conflictResult);
        
        log.info("Multi-source ingestion completed successfully!");
    }
    
    private void demonstrateConflictDetection() {
        log.info("--- Conflict Detection Demo ---");
        
        List<ConflictReport> allConflicts = conflictDetectionService.detectAllConflicts();
        log.info("Total conflicts detected: {}", allConflicts.size());
        
        if (allConflicts.isEmpty()) {
            log.info("No conflicts found - all sources are in agreement!");
        } else {
            for (ConflictReport conflict : allConflicts) {
                log.warn("CONFLICT DETECTED: {}", conflict);
                log.warn("  Type: {} ({})", conflict.getType().getDisplayName(), conflict.getType().getDescription());
                log.warn("  Severity: {} ({})", conflict.getSeverity().getDisplayName(), conflict.getSeverity().getDescription());
                log.warn("  Description: {}", conflict.getDescription());
                log.warn("  Conflicting claims: {}", conflict.getConflictingClaims().size());
            }
        }
    }
    
    private void demonstrateDataAnalysis() {
        log.info("--- Data Analysis Demo ---");
        List<DependencyClaim> allClaims = dependencyClaimRepository.findAll();
        log.info("Total dependency claims in database: {}", allClaims.size());

        // --- Analytics helpers ---
        java.util.Map<String, Long> outDegree = allClaims.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                c -> c.getFromService().getName(), java.util.stream.Collectors.counting()));
        java.util.Map<String, Long> inDegree = allClaims.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                c -> c.getToService().getName(), java.util.stream.Collectors.counting()));
        java.util.Set<String> allServices = new java.util.HashSet<>();
        for (DependencyClaim c : allClaims) {
            allServices.add(c.getFromService().getName());
            allServices.add(c.getToService().getName());
        }

        // --- Analytics sections ---
        logAnalyticsBySource(allClaims);
        logConfidenceDistribution(allClaims);
        logSampleDependencies(allClaims);
        logTopDependencies(allClaims);
        logMostConnected(outDegree, inDegree);
        logSourceOverlap(allClaims);
        logOrphansEntrypointsEndpoints(allServices, inDegree, outDegree);
        logCycles(allClaims);
        logLargestComponent(allClaims, allServices);
        logAveragePathLength(allClaims, allServices);
    }

    // --- Helper methods for analytics ---
    private void logAnalyticsBySource(List<DependencyClaim> allClaims) {
        allClaims.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                DependencyClaim::getSource,
                java.util.stream.Collectors.counting()))
            .forEach((source, count) ->
                log.info("Source '{}': {} claims", source, count));
    }

    private void logConfidenceDistribution(List<DependencyClaim> allClaims) {
        double avgConfidence = allClaims.stream()
            .mapToDouble(DependencyClaim::getConfidence)
            .average()
            .orElse(0.0);
        log.info("Average confidence across all claims: {}", String.format("%.2f", avgConfidence));
        long low = allClaims.stream().filter(c -> c.getConfidence() < 0.5).count();
        long mid = allClaims.stream().filter(c -> c.getConfidence() >= 0.5 && c.getConfidence() < 0.8).count();
        long high = allClaims.stream().filter(c -> c.getConfidence() >= 0.8).count();
        log.info("Claims by confidence: <0.5: {}, 0.5-0.8: {}, >=0.8: {}", low, mid, high);
    }

    private void logSampleDependencies(List<DependencyClaim> allClaims) {
        log.info("Sample dependencies discovered:");
        allClaims.stream()
            .limit(5)
            .forEach(claim ->
                log.info("  {} -> {} (confidence: {}, source: {})",
                    claim.getFromService().getName(),
                    claim.getToService().getName(),
                    String.format("%.2f", claim.getConfidence()),
                    claim.getSource()));
    }

    private void logTopDependencies(List<DependencyClaim> allClaims) {
        log.info("Top 5 most common dependencies:");
        allClaims.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                c -> c.getFromService().getName() + " -> " + c.getToService().getName(),
                java.util.stream.Collectors.counting()))
            .entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(5)
            .forEach(e -> log.info("  {} ({} claims)", e.getKey(), e.getValue()));
    }

    private void logMostConnected(java.util.Map<String, Long> outDegree, java.util.Map<String, Long> inDegree) {
        log.info("Top 3 most connected (out-degree):");
        outDegree.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(3)
            .forEach(e -> log.info("  {} ({} outgoing)", e.getKey(), e.getValue()));
        log.info("Top 3 most connected (in-degree):");
        inDegree.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(3)
            .forEach(e -> log.info("  {} ({} incoming)", e.getKey(), e.getValue()));
    }

    private void logSourceOverlap(List<DependencyClaim> allClaims) {
        log.info("Dependencies claimed by multiple sources:");
        allClaims.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                c -> c.getFromService().getName() + " -> " + c.getToService().getName(),
                java.util.stream.Collectors.mapping(DependencyClaim::getSource, java.util.stream.Collectors.toSet())))
            .entrySet().stream()
            .filter(e -> e.getValue().size() > 1)
            .forEach(e -> log.info("  {} (sources: {})", e.getKey(), e.getValue()));
    }

    private void logOrphansEntrypointsEndpoints(java.util.Set<String> allServices, java.util.Map<String, Long> inDegree, java.util.Map<String, Long> outDegree) {
        java.util.Set<String> hasIn = inDegree.keySet();
        java.util.Set<String> hasOut = outDegree.keySet();
        java.util.Set<String> orphans = new java.util.HashSet<>(allServices);
        orphans.removeAll(hasIn);
        orphans.removeAll(hasOut);
        log.info("Orphan services (no in-degree and no out-degree): {}", orphans);
        java.util.Set<String> onlyOutgoing = new java.util.HashSet<>(hasOut);
        onlyOutgoing.removeAll(hasIn);
        log.info("Entry point services (only outgoing): {}", onlyOutgoing);
        java.util.Set<String> onlyIncoming = new java.util.HashSet<>(hasIn);
        onlyIncoming.removeAll(hasOut);
        log.info("Endpoint services (only incoming): {}", onlyIncoming);
    }

    private void logCycles(List<DependencyClaim> allClaims) {
        log.info("Cyclic dependencies (up to length 4):");
        java.util.Map<String, java.util.Set<String>> adj = new java.util.HashMap<>();
        for (DependencyClaim c : allClaims) {
            adj.computeIfAbsent(c.getFromService().getName(), k -> new java.util.HashSet<>())
                .add(c.getToService().getName());
        }
        java.util.Set<String> cycles = new java.util.HashSet<>();
        for (String a : adj.keySet()) {
            for (String b : adj.getOrDefault(a, java.util.Collections.emptySet())) {
                if (adj.getOrDefault(b, java.util.Collections.emptySet()).contains(a)) {
                    cycles.add(a + " <-> " + b);
                }
                for (String c : adj.getOrDefault(b, java.util.Collections.emptySet())) {
                    if (adj.getOrDefault(c, java.util.Collections.emptySet()).contains(a)) {
                        cycles.add(a + " -> " + b + " -> " + c + " -> " + a);
                    }
                    for (String d : adj.getOrDefault(c, java.util.Collections.emptySet())) {
                        if (adj.getOrDefault(d, java.util.Collections.emptySet()).contains(a)) {
                            cycles.add(a + " -> " + b + " -> " + c + " -> " + d + " -> " + a);
                        }
                    }
                }
            }
        }
        if (cycles.isEmpty()) {
            log.info("  No cycles detected (up to length 4)");
        } else {
            cycles.forEach(cyc -> log.info("  {}", cyc));
        }
    }

    private void logLargestComponent(List<DependencyClaim> allClaims, java.util.Set<String> allServices) {
        log.info("Largest connected component (undirected):");
        java.util.Map<String, java.util.Set<String>> undirected = new java.util.HashMap<>();
        for (DependencyClaim claim : allClaims) {
            undirected.computeIfAbsent(claim.getFromService().getName(), k -> new java.util.HashSet<>())
                .add(claim.getToService().getName());
            undirected.computeIfAbsent(claim.getToService().getName(), k -> new java.util.HashSet<>())
                .add(claim.getFromService().getName());
        }
        java.util.Set<String> visited = new java.util.HashSet<>();
        int maxSize = 0;
        java.util.Set<String> largest = new java.util.HashSet<>();
        for (String node : undirected.keySet()) {
            if (!visited.contains(node)) {
                java.util.Set<String> comp = new java.util.HashSet<>();
                java.util.LinkedList<String> queue = new java.util.LinkedList<>();
                queue.add(node);
                while (!queue.isEmpty()) {
                    String curr = queue.poll();
                    if (comp.add(curr)) {
                        queue.addAll(undirected.getOrDefault(curr, java.util.Collections.emptySet()));
                    }
                }
                if (comp.size() > maxSize) {
                    maxSize = comp.size();
                    largest = comp;
                }
                visited.addAll(comp);
            }
        }
        log.info("  Size: {} Members: {}", maxSize, largest);
    }

    private void logAveragePathLength(List<DependencyClaim> allClaims, java.util.Set<String> allServices) {
        log.info("Average path length (BFS sampling):");
        java.util.Map<String, java.util.Set<String>> undirected = new java.util.HashMap<>();
        for (DependencyClaim claim : allClaims) {
            undirected.computeIfAbsent(claim.getFromService().getName(), k -> new java.util.HashSet<>())
                .add(claim.getToService().getName());
            undirected.computeIfAbsent(claim.getToService().getName(), k -> new java.util.HashSet<>())
                .add(claim.getFromService().getName());
        }
        java.util.List<String> nodes = new java.util.ArrayList<>(allServices);
        int samples = Math.min(100, nodes.size());
        double totalPath = 0;
        int pathCount = 0;
        java.util.Random rand = new java.util.Random(42); // deterministic
        for (int i = 0; i < samples; i++) {
            String start = nodes.get(rand.nextInt(nodes.size()));
            java.util.Map<String, Integer> dist = new java.util.HashMap<>();
            java.util.LinkedList<String> q = new java.util.LinkedList<>();
            dist.put(start, 0);
            q.add(start);
            while (!q.isEmpty()) {
                String curr = q.poll();
                int d = dist.get(curr);
                for (String neigh : undirected.getOrDefault(curr, java.util.Collections.emptySet())) {
                    if (!dist.containsKey(neigh)) {
                        dist.put(neigh, d + 1);
                        q.add(neigh);
                        totalPath += d + 1;
                        pathCount++;
                    }
                }
            }
        }
        if (pathCount > 0) {
            log.info("  Average path length: {}", String.format("%.2f", totalPath / pathCount));
        } else {
            log.info("  Not enough data to compute path length");
        }
    }
}
