package com.enterprise.dependency.service;

import com.example.mapper.model.DependencyClaim;
import com.example.mapper.repo.DependencyClaimRepository;
import com.example.mapper.model.SourceReliability;
import com.example.mapper.service.SourceReliabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting conflicts between dependency claims from different sources.
 * 
 * <p>Identifies various types of conflicts:
 * <ul>
 * <li>Contradictory claims: Different sources claiming opposite dependencies</li>
 * <li>Confidence conflicts: Same dependency with significantly different confidence scores</li>
 * <li>Temporal conflicts: Dependencies that appear and disappear over time</li>
 * <li>Source conflicts: Different sources providing conflicting information</li>
 * </ul>
 * 
 * <p>This is a basic implementation focusing on conflict detection rather than resolution.
 * Full LCA (Least Common Ancestor) and advanced conflict resolution will be implemented later.
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
@Service
public class ConflictDetectionService {
    
    private static final Logger log = LoggerFactory.getLogger(ConflictDetectionService.class);
    
    // Thresholds for conflict detection
    private static final double CONFIDENCE_THRESHOLD = 0.3; // 30% difference triggers conflict
    private static final long TEMPORAL_WINDOW_HOURS = 24; // Consider claims within 24 hours as concurrent
    
    private final DependencyClaimRepository dependencyClaimRepository;
    private final SourceReliabilityService reliabilityService;
    
    @Autowired
    public ConflictDetectionService(DependencyClaimRepository dependencyClaimRepository, SourceReliabilityService reliabilityService) {
        this.dependencyClaimRepository = dependencyClaimRepository;
        this.reliabilityService = reliabilityService;
    }
    
    /**
     * Detects all conflicts in the current dataset.
     * 
     * @return list of detected conflicts
     */
    public List<ConflictReport> detectAllConflicts() {
        log.info("Starting comprehensive conflict detection");
        List<DependencyClaim> allClaims = dependencyClaimRepository.findAll();
        List<ConflictReport> conflicts = new ArrayList<>();
        Map<DependencyPair, List<DependencyClaim>> claimsByDependency = allClaims.stream()
            .collect(Collectors.groupingBy(c -> new DependencyPair(
                c.getFromService().getName(), c.getToService().getName())));
        for (Map.Entry<DependencyPair, List<DependencyClaim>> entry : claimsByDependency.entrySet()) {
            DependencyPair pair = entry.getKey();
            List<DependencyClaim> claims = entry.getValue();
            Set<String> sources = new HashSet<>();
            double minReliability = 1.0, maxReliability = 0.0;
            for (DependencyClaim claim : claims) {
                double rel = reliabilityService.getReliability(claim.getSource());
                minReliability = Math.min(minReliability, rel);
                maxReliability = Math.max(maxReliability, rel);
                sources.add(claim.getSource());
            }
            if (sources.size() > 1 && (maxReliability - minReliability) > 0.3) {
                ConflictReport report = new ConflictReport(pair, claims, String.format("min=%.2f, max=%.2f", minReliability, maxReliability));
                conflicts.add(report);
            }
        }
        return conflicts;
    }
    
    /**
     * Detects conflicts for a specific dependency pair.
     */
    public List<ConflictReport> detectConflictsForDependency(String fromService, String toService) {
        List<DependencyClaim> claims = dependencyClaimRepository.findByFromServiceNameAndToServiceName(
            fromService, toService);
        
        if (claims.size() <= 1) {
            return Collections.emptyList();
        }
        
        return analyzeClaimsForConflicts(new DependencyPair(fromService, toService), claims);
    }
    
    /**
     * Analyzes a list of claims for the same dependency to detect conflicts.
     */
    private List<ConflictReport> analyzeClaimsForConflicts(DependencyPair dependency, List<DependencyClaim> claims) {
        List<ConflictReport> conflicts = new ArrayList<>();
        
        // Check for confidence conflicts
        conflicts.addAll(detectConfidenceConflicts(dependency, claims));
        
        // Check for temporal conflicts
        conflicts.addAll(detectTemporalConflicts(dependency, claims));
        
        // Check for source conflicts
        conflicts.addAll(detectSourceConflicts(dependency, claims));
        
        return conflicts;
    }
    
    /**
     * Detects conflicts based on significantly different confidence scores.
     */
    private List<ConflictReport> detectConfidenceConflicts(DependencyPair dependency, List<DependencyClaim> claims) {
        List<ConflictReport> conflicts = new ArrayList<>();
        double maxConfidence = claims.stream().mapToDouble(DependencyClaim::getConfidence).max().orElse(0.0);
        double minConfidence = claims.stream().mapToDouble(DependencyClaim::getConfidence).min().orElse(0.0);
        if (maxConfidence - minConfidence > CONFIDENCE_THRESHOLD) {
            String reliabilityStats = String.format("confidenceDiff=%.2f", maxConfidence - minConfidence);
            conflicts.add(new ConflictReport(dependency, claims, reliabilityStats));
        }
        return conflicts;
    }
    
    /**
     * Detects conflicts based on temporal patterns (dependencies appearing/disappearing).
     */
    private List<ConflictReport> detectTemporalConflicts(DependencyPair dependency, List<DependencyClaim> claims) {
        List<ConflictReport> conflicts = new ArrayList<>();
        List<DependencyClaim> sortedClaims = claims.stream()
            .sorted(Comparator.comparing(DependencyClaim::getTimestamp))
            .collect(Collectors.toList());
        for (int i = 0; i < sortedClaims.size() - 1; i++) {
            DependencyClaim current = sortedClaims.get(i);
            DependencyClaim next = sortedClaims.get(i + 1);
            long hoursBetween = ChronoUnit.HOURS.between(current.getTimestamp(), next.getTimestamp());
            if (hoursBetween > TEMPORAL_WINDOW_HOURS * 7) {
                String reliabilityStats = String.format("temporalGap=%d", hoursBetween);
                conflicts.add(new ConflictReport(dependency, Arrays.asList(current, next), reliabilityStats));
            }
        }
        return conflicts;
    }
    
    /**
     * Detects conflicts between different sources reporting on the same dependency.
     */
    private List<ConflictReport> detectSourceConflicts(DependencyPair dependency, List<DependencyClaim> claims) {
        List<ConflictReport> conflicts = new ArrayList<>();
        Map<String, List<DependencyClaim>> claimsBySource = claims.stream()
            .collect(Collectors.groupingBy(DependencyClaim::getSource));
        if (claimsBySource.size() > 1) {
            Map<String, Double> avgConfidenceBySource = claimsBySource.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().stream()
                        .mapToDouble(DependencyClaim::getConfidence)
                        .average()
                        .orElse(0.0)
                ));
            double maxAvgConfidence = avgConfidenceBySource.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(0.0);
            double minAvgConfidence = avgConfidenceBySource.values().stream()
                .mapToDouble(Double::doubleValue).min().orElse(0.0);
            if (maxAvgConfidence - minAvgConfidence > CONFIDENCE_THRESHOLD) {
                String reliabilityStats = "sourceAvgConf=" + avgConfidenceBySource;
                conflicts.add(new ConflictReport(dependency, claims, reliabilityStats));
            }
        }
        return conflicts;
    }
    
    private ConflictSeverity calculateConflictSeverity(double confidenceDiff) {
        if (confidenceDiff > 0.7) return ConflictSeverity.HIGH;
        if (confidenceDiff > 0.4) return ConflictSeverity.MEDIUM;
        return ConflictSeverity.LOW;
    }
    
    private ConflictSeverity calculateTemporalSeverity(long hoursBetween) {
        if (hoursBetween > 24 * 30) return ConflictSeverity.HIGH; // > 1 month
        if (hoursBetween > 24 * 7) return ConflictSeverity.MEDIUM; // > 1 week
        return ConflictSeverity.LOW;
    }
    
    /**
     * Represents a detected conflict between dependency claims.
     */
    public static class ConflictReport {
        public DependencyPair pair;
        public List<DependencyClaim> claims;
        public String reliabilityStats;
        public ConflictReport() {}
        public ConflictReport(DependencyPair pair, List<DependencyClaim> claims, String reliabilityStats) {
            this.pair = pair;
            this.claims = claims;
            this.reliabilityStats = reliabilityStats;
        }
        @Override
        public String toString() {
            return String.format("ConflictReport{pair=%s, claims=%d, reliability=%s}", 
                pair, claims.size(), reliabilityStats);
        }
        
        // --- Add accessors for dashboard/REST compatibility ---
        public DependencyPair getDependency() { return pair; }
        public List<DependencyClaim> getConflictingClaims() { return claims; }
        public String getReliabilityStats() { return reliabilityStats; }
        public ConflictType getType() { return ConflictType.SOURCE_CONFLICT; } // Placeholder, improve logic
        public ConflictSeverity getSeverity() { return ConflictSeverity.MEDIUM; } // Placeholder, improve logic
        public String getDescription() { return "Conflict between claims for " + pair; }
        public Instant getDetectedAt() { return Instant.now(); } // Placeholder, improve logic
    }
    
    /**
     * Represents a dependency pair (from -> to).
     */
    public static class DependencyPair {
        private final String fromService;
        private final String toService;
        
        public DependencyPair(String fromService, String toService) {
            this.fromService = fromService;
            this.toService = toService;
        }
        
        public String getFromService() { return fromService; }
        public String getToService() { return toService; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DependencyPair that = (DependencyPair) o;
            return Objects.equals(fromService, that.fromService) && 
                   Objects.equals(toService, that.toService);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(fromService, toService);
        }
        
        @Override
        public String toString() {
            return fromService + " -> " + toService;
        }
    }
    
    public enum ConflictType {
        CONFIDENCE_CONFLICT("Confidence Conflict", "Sources report significantly different confidence levels"),
        TEMPORAL_CONFLICT("Temporal Conflict", "Dependency appears and disappears over time"),
        SOURCE_CONFLICT("Source Conflict", "Different sources disagree about this dependency");
        
        private final String displayName;
        private final String description;
        
        ConflictType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public enum ConflictSeverity {
        LOW("Low", "Minor disagreement, likely due to measurement variance"),
        MEDIUM("Medium", "Moderate disagreement, requires investigation"),
        HIGH("High", "Significant disagreement, requires immediate attention");
        
        private final String displayName;
        private final String description;
        
        ConflictSeverity(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
}
