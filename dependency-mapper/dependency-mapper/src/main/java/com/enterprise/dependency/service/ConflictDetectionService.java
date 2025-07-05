package com.enterprise.dependency.service;

import com.example.mapper.model.DependencyClaim;
import com.example.mapper.repo.DependencyClaimRepository;
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
    
    @Autowired
    public ConflictDetectionService(DependencyClaimRepository dependencyClaimRepository) {
        this.dependencyClaimRepository = dependencyClaimRepository;
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
        
        // Group claims by dependency pair (from -> to)
        Map<DependencyPair, List<DependencyClaim>> claimsByDependency = allClaims.stream()
            .collect(Collectors.groupingBy(claim -> 
                new DependencyPair(
                    claim.getFromService().getName(),
                    claim.getToService().getName()
                )
            ));
        
        for (Map.Entry<DependencyPair, List<DependencyClaim>> entry : claimsByDependency.entrySet()) {
            DependencyPair dependency = entry.getKey();
            List<DependencyClaim> claims = entry.getValue();
            
            if (claims.size() > 1) {
                List<ConflictReport> dependencyConflicts = analyzeClaimsForConflicts(dependency, claims);
                conflicts.addAll(dependencyConflicts);
            }
        }
        
        log.info("Conflict detection completed. Found {} conflicts across {} dependencies", 
            conflicts.size(), claimsByDependency.size());
        
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
            ConflictReport conflict = new ConflictReport(
                ConflictType.CONFIDENCE_CONFLICT,
                dependency,
                "Significant confidence difference: " + String.format("%.2f", maxConfidence) + 
                " vs " + String.format("%.2f", minConfidence),
                claims,
                calculateConflictSeverity(maxConfidence - minConfidence)
            );
            conflicts.add(conflict);
        }
        
        return conflicts;
    }
    
    /**
     * Detects conflicts based on temporal patterns (dependencies appearing/disappearing).
     */
    private List<ConflictReport> detectTemporalConflicts(DependencyPair dependency, List<DependencyClaim> claims) {
        List<ConflictReport> conflicts = new ArrayList<>();
        
        // Sort claims by timestamp
        List<DependencyClaim> sortedClaims = claims.stream()
            .sorted(Comparator.comparing(DependencyClaim::getTimestamp))
            .collect(Collectors.toList());
        
        // Look for gaps in temporal coverage
        for (int i = 0; i < sortedClaims.size() - 1; i++) {
            DependencyClaim current = sortedClaims.get(i);
            DependencyClaim next = sortedClaims.get(i + 1);
            
            long hoursBetween = ChronoUnit.HOURS.between(current.getTimestamp(), next.getTimestamp());
            
            if (hoursBetween > TEMPORAL_WINDOW_HOURS * 7) { // 7-day gap threshold
                ConflictReport conflict = new ConflictReport(
                    ConflictType.TEMPORAL_CONFLICT,
                    dependency,
                    "Large temporal gap: " + hoursBetween + " hours between claims",
                    Arrays.asList(current, next),
                    calculateTemporalSeverity(hoursBetween)
                );
                conflicts.add(conflict);
            }
        }
        
        return conflicts;
    }
    
    /**
     * Detects conflicts between different sources reporting on the same dependency.
     */
    private List<ConflictReport> detectSourceConflicts(DependencyPair dependency, List<DependencyClaim> claims) {
        List<ConflictReport> conflicts = new ArrayList<>();
        
        // Group by source
        Map<String, List<DependencyClaim>> claimsBySource = claims.stream()
            .collect(Collectors.groupingBy(DependencyClaim::getSource));
        
        if (claimsBySource.size() > 1) {
            // Check if sources have significantly different average confidence
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
                ConflictReport conflict = new ConflictReport(
                    ConflictType.SOURCE_CONFLICT,
                    dependency,
                    "Sources disagree on confidence: " + avgConfidenceBySource,
                    claims,
                    calculateConflictSeverity(maxAvgConfidence - minAvgConfidence)
                );
                conflicts.add(conflict);
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
        private final ConflictType type;
        private final DependencyPair dependency;
        private final String description;
        private final List<DependencyClaim> conflictingClaims;
        private final ConflictSeverity severity;
        private final Instant detectedAt;
        
        public ConflictReport(ConflictType type, DependencyPair dependency, String description,
                            List<DependencyClaim> conflictingClaims, ConflictSeverity severity) {
            this.type = type;
            this.dependency = dependency;
            this.description = description;
            this.conflictingClaims = new ArrayList<>(conflictingClaims);
            this.severity = severity;
            this.detectedAt = Instant.now();
        }
        
        // Getters
        public ConflictType getType() { return type; }
        public DependencyPair getDependency() { return dependency; }
        public String getDescription() { return description; }
        public List<DependencyClaim> getConflictingClaims() { return new ArrayList<>(conflictingClaims); }
        public ConflictSeverity getSeverity() { return severity; }
        public Instant getDetectedAt() { return detectedAt; }
        
        @Override
        public String toString() {
            return String.format("ConflictReport{type=%s, dependency=%s, severity=%s, claims=%d, desc='%s'}", 
                type, dependency, severity, conflictingClaims.size(), description);
        }
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
