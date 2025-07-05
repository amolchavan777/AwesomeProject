package com.enterprise.dependency.service;

import com.enterprise.dependency.model.core.Claim;
import com.enterprise.dependency.model.core.ConfidenceScore;
import com.enterprise.dependency.model.core.DependencyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for normalizing dependency claims from different sources
 * and ensuring consistent representation across the entire system.
 * 
 * <p>Handles:
 * - Application name normalization (consistent naming conventions)
 * - Service name mapping and aliasing
 * - Confidence score calibration across sources
 * - Provenance tracking and metadata standardization
 * - Duplicate detection and merging
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
@Service
public class ClaimNormalizer {
    
    private static final Logger log = LoggerFactory.getLogger(ClaimNormalizer.class);
    
    // Service name mappings for consistent naming
    private static final Map<String, String> SERVICE_NAME_MAPPINGS = new HashMap<>();
    static {
        // Database service mappings
        SERVICE_NAME_MAPPINGS.put("mysql-primary", "mysql-database");
        SERVICE_NAME_MAPPINGS.put("mysql-service", "mysql-database");
        SERVICE_NAME_MAPPINGS.put("db-mysql", "mysql-database");
        
        SERVICE_NAME_MAPPINGS.put("postgresql-primary", "postgresql-database");
        SERVICE_NAME_MAPPINGS.put("postgres-service", "postgresql-database");
        SERVICE_NAME_MAPPINGS.put("db-postgres", "postgresql-database");
        
        // Cache service mappings
        SERVICE_NAME_MAPPINGS.put("redis-cache", "redis-service");
        SERVICE_NAME_MAPPINGS.put("cache-redis", "redis-service");
        
        // Message queue mappings
        SERVICE_NAME_MAPPINGS.put("kafka-cluster", "kafka-service");
        SERVICE_NAME_MAPPINGS.put("kafka-broker", "kafka-service");
        SERVICE_NAME_MAPPINGS.put("message-broker", "kafka-service");
        
        // Common service name patterns
        SERVICE_NAME_MAPPINGS.put("auth-service", "authentication-service");
        SERVICE_NAME_MAPPINGS.put("user-service", "user-management-service");
        SERVICE_NAME_MAPPINGS.put("payment-service", "payment-gateway");
        SERVICE_NAME_MAPPINGS.put("payment-gateway", "payment-gateway");
    }
    
    // Source confidence weights for calibration
    private static final Map<String, Double> SOURCE_CONFIDENCE_WEIGHTS = new HashMap<>();
    static {
        SOURCE_CONFIDENCE_WEIGHTS.put("configuration-file", 1.0);    // Explicit config = highest confidence
        SOURCE_CONFIDENCE_WEIGHTS.put("router-log", 0.9);           // Runtime evidence = very high
        SOURCE_CONFIDENCE_WEIGHTS.put("network-discovery", 0.7);    // Inferred dependencies = medium-high
        SOURCE_CONFIDENCE_WEIGHTS.put("unknown", 0.5);              // Default for unknown sources
    }
    
    /**
     * Normalize a collection of raw dependency claims from various sources.
     * 
     * @param rawClaims the raw claims to normalize
     * @return normalized claims with consistent naming and metadata
     */
    public List<NormalizedClaim> normalizeClaims(Collection<Claim> rawClaims) {
        if (rawClaims == null || rawClaims.isEmpty()) {
            return new ArrayList<>();
        }
        
        long startTime = System.currentTimeMillis();
        log.info("Starting claim normalization for {} raw claims", rawClaims.size());
        
        List<NormalizedClaim> normalizedClaims = rawClaims.stream()
            .map(this::normalizeIndividualClaim)
            .collect(Collectors.toList());
        
        // Group and merge duplicate claims
        Map<String, List<NormalizedClaim>> groupedClaims = groupClaimsByDependency(normalizedClaims);
        List<NormalizedClaim> mergedClaims = new ArrayList<>();
        
        for (Map.Entry<String, List<NormalizedClaim>> entry : groupedClaims.entrySet()) {
            List<NormalizedClaim> duplicates = entry.getValue();
            if (duplicates.size() == 1) {
                mergedClaims.add(duplicates.get(0));
            } else {
                mergedClaims.add(mergeDuplicateClaims(duplicates));
            }
        }
        
        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Claim normalization completed in {}ms. Raw: {}, Normalized: {}, Merged: {}", 
            processingTime, rawClaims.size(), normalizedClaims.size(), mergedClaims.size());
        
        return mergedClaims;
    }
    
    /**
     * Normalize an individual claim.
     */
    private NormalizedClaim normalizeIndividualClaim(Claim claim) {
        // Normalize application names
        String normalizedFrom = normalizeServiceName(claim.getFromApplication());
        String normalizedTo = normalizeServiceName(claim.getToApplication());
        
        // Calibrate confidence based on source
        ConfidenceScore calibratedConfidence = calibrateConfidence(claim.getConfidence(), claim.getSource());
        
        // Create normalized metadata
        Map<String, String> normalizedMetadata = normalizeMetadata(convertMetadata(claim.getMetadata()), claim.getSource());
        
        return new NormalizedClaim(
            normalizedFrom,
            normalizedTo,
            claim.getDependencyType(),
            calibratedConfidence,
            claim.getSource(),
            claim.getTimestamp(),
            claim.getRawData(),
            normalizedMetadata,
            Collections.singletonList(createProvenance(claim))
        );
    }
    
    /**
     * Convert Map<String, Object> to Map<String, String> for metadata handling.
     */
    private Map<String, String> convertMetadata(Map<String, Object> originalMetadata) {
        if (originalMetadata == null) {
            return new HashMap<>();
        }
        
        Map<String, String> converted = new HashMap<>();
        for (Map.Entry<String, Object> entry : originalMetadata.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue().toString() : "unknown";
            converted.put(entry.getKey(), value);
        }
        return converted;
    }
    
    /**
     * Normalize service names using consistent naming conventions.
     */
    private String normalizeServiceName(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return "unknown-service";
        }
        
        String normalized = serviceName.toLowerCase().trim();
        
        // Apply direct mappings first
        if (SERVICE_NAME_MAPPINGS.containsKey(normalized)) {
            return SERVICE_NAME_MAPPINGS.get(normalized);
        }
        
        // Apply pattern-based normalization
        normalized = applyNamingConventions(normalized);
        
        return normalized;
    }
    
    /**
     * Apply consistent naming conventions.
     */
    private String applyNamingConventions(String serviceName) {
        // Ensure service names end with appropriate suffixes
        if (serviceName.contains("database") || serviceName.contains("db") || 
            serviceName.matches(".*(mysql|postgresql|oracle|mongodb).*")) {
            if (!serviceName.endsWith("-database") && !serviceName.endsWith("-service")) {
                return serviceName + "-database";
            }
        } else if (serviceName.contains("cache") || serviceName.contains("redis")) {
            if (!serviceName.endsWith("-service") && !serviceName.endsWith("-cache")) {
                return serviceName + "-service";
            }
        } else if (serviceName.contains("kafka") || serviceName.contains("queue") || serviceName.contains("broker")) {
            if (!serviceName.endsWith("-service") && !serviceName.endsWith("-broker")) {
                return serviceName + "-service";
            }
        } else if (!serviceName.endsWith("-service") && !serviceName.endsWith("-gateway") && 
                   !serviceName.endsWith("-api") && !serviceName.endsWith("-database")) {
            return serviceName + "-service";
        }
        
        return serviceName;
    }
    
    /**
     * Calibrate confidence scores based on source reliability.
     */
    private ConfidenceScore calibrateConfidence(ConfidenceScore originalConfidence, String source) {
        double sourceWeight = SOURCE_CONFIDENCE_WEIGHTS.getOrDefault(source, 0.5);
        double originalValue = getConfidenceValue(originalConfidence);
        double calibratedValue = originalValue * sourceWeight;
        
        return valueToConfidenceScore(calibratedValue);
    }
    
    private double getConfidenceValue(ConfidenceScore confidence) {
        switch (confidence) {
            case VERY_HIGH: return 1.0;
            case HIGH: return 0.8;
            case MEDIUM: return 0.6;
            case LOW: return 0.4;
            case VERY_LOW: return 0.2;
            default: return 0.5;
        }
    }
    
    private ConfidenceScore valueToConfidenceScore(double value) {
        if (value >= 0.9) return ConfidenceScore.VERY_HIGH;
        if (value >= 0.7) return ConfidenceScore.HIGH;
        if (value >= 0.5) return ConfidenceScore.MEDIUM;
        if (value >= 0.3) return ConfidenceScore.LOW;
        return ConfidenceScore.VERY_LOW;
    }
    
    /**
     * Normalize metadata with consistent keys and values.
     */
    private Map<String, String> normalizeMetadata(Map<String, String> originalMetadata, String source) {
        Map<String, String> normalized = new HashMap<>();
        
        if (originalMetadata != null) {
            for (Map.Entry<String, String> entry : originalMetadata.entrySet()) {
                String key = normalizeMetadataKey(entry.getKey());
                String value = normalizeMetadataValue(entry.getValue());
                normalized.put(key, value);
            }
        }
        
        // Add standard metadata
        normalized.put("source_type", source);
        normalized.put("normalized_at", Instant.now().toString());
        
        return normalized;
    }
    
    private String normalizeMetadataKey(String key) {
        return key.toLowerCase().replace(" ", "_").replace("-", "_");
    }
    
    private String normalizeMetadataValue(String value) {
        if (value == null) return "unknown";
        return value.trim();
    }
    
    /**
     * Group claims by their dependency (from -> to).
     */
    private Map<String, List<NormalizedClaim>> groupClaimsByDependency(List<NormalizedClaim> claims) {
        return claims.stream()
            .collect(Collectors.groupingBy(claim -> 
                claim.getFromApplication() + " -> " + claim.getToApplication()));
    }
    
    /**
     * Merge duplicate claims from different sources.
     */
    private NormalizedClaim mergeDuplicateClaims(List<NormalizedClaim> duplicates) {
        if (duplicates.isEmpty()) {
            throw new IllegalArgumentException("Cannot merge empty list of claims");
        }
        
        if (duplicates.size() == 1) {
            return duplicates.get(0);
        }
        
        // Use the claim with highest confidence as the base
        NormalizedClaim baseClaim = duplicates.stream()
            .max(Comparator.comparing(c -> getConfidenceValue(c.getConfidence())))
            .orElse(duplicates.get(0));
        
        // Collect all provenance information
        List<ClaimProvenance> allProvenance = duplicates.stream()
            .flatMap(c -> c.getProvenance().stream())
            .collect(Collectors.toList());
        
        // Merge metadata
        Map<String, String> mergedMetadata = new HashMap<>(baseClaim.getMetadata());
        for (NormalizedClaim duplicate : duplicates) {
            for (Map.Entry<String, String> entry : duplicate.getMetadata().entrySet()) {
                String key = entry.getKey();
                if (!mergedMetadata.containsKey(key) || mergedMetadata.get(key).equals("unknown")) {
                    mergedMetadata.put(key, entry.getValue());
                }
            }
        }
        
        // Add merge information
        mergedMetadata.put("merged_from_sources", String.valueOf(duplicates.size()));
        mergedMetadata.put("all_sources", duplicates.stream()
            .map(NormalizedClaim::getSource)
            .distinct()
            .collect(Collectors.joining(",")));
        
        return new NormalizedClaim(
            baseClaim.getFromApplication(),
            baseClaim.getToApplication(),
            baseClaim.getDependencyType(),
            baseClaim.getConfidence(),
            baseClaim.getSource(),
            baseClaim.getTimestamp(),
            baseClaim.getRawData(),
            mergedMetadata,
            allProvenance
        );
    }
    
    /**
     * Create provenance information for tracking claim origins.
     */
    private ClaimProvenance createProvenance(Claim claim) {
        return new ClaimProvenance(
            claim.getSource(),
            claim.getTimestamp(),
            claim.getRawData(),
            claim.getConfidence(),
            convertMetadata(claim.getMetadata())
        );
    }
    
    /**
     * Represents a normalized dependency claim with enhanced metadata and provenance tracking.
     */
    public static class NormalizedClaim {
        private final String fromApplication;
        private final String toApplication;
        private final DependencyType dependencyType;
        private final ConfidenceScore confidence;
        private final String source;
        private final Instant timestamp;
        private final String rawData;
        private final Map<String, String> metadata;
        private final List<ClaimProvenance> provenance;
        
        public NormalizedClaim(String fromApplication, String toApplication, DependencyType dependencyType,
                              ConfidenceScore confidence, String source, Instant timestamp, String rawData,
                              Map<String, String> metadata, List<ClaimProvenance> provenance) {
            this.fromApplication = fromApplication;
            this.toApplication = toApplication;
            this.dependencyType = dependencyType;
            this.confidence = confidence;
            this.source = source;
            this.timestamp = timestamp;
            this.rawData = rawData;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            this.provenance = provenance != null ? new ArrayList<>(provenance) : new ArrayList<>();
        }
        
        // Getters
        public String getFromApplication() { return fromApplication; }
        public String getToApplication() { return toApplication; }
        public DependencyType getDependencyType() { return dependencyType; }
        public ConfidenceScore getConfidence() { return confidence; }
        public String getSource() { return source; }
        public Instant getTimestamp() { return timestamp; }
        public String getRawData() { return rawData; }
        public Map<String, String> getMetadata() { return new HashMap<>(metadata); }
        public List<ClaimProvenance> getProvenance() { return new ArrayList<>(provenance); }
        
        @Override
        public String toString() {
            return String.format("NormalizedClaim{%s -> %s, confidence=%s, sources=%d}", 
                fromApplication, toApplication, confidence, provenance.size());
        }
    }
    
    /**
     * Represents the provenance (origin) of a dependency claim.
     */
    public static class ClaimProvenance {
        private final String source;
        private final Instant timestamp;
        private final String rawData;
        private final ConfidenceScore originalConfidence;
        private final Map<String, String> originalMetadata;
        
        public ClaimProvenance(String source, Instant timestamp, String rawData,
                              ConfidenceScore originalConfidence, Map<String, String> originalMetadata) {
            this.source = source;
            this.timestamp = timestamp;
            this.rawData = rawData;
            this.originalConfidence = originalConfidence;
            this.originalMetadata = originalMetadata != null ? new HashMap<>(originalMetadata) : new HashMap<>();
        }
        
        // Getters
        public String getSource() { return source; }
        public Instant getTimestamp() { return timestamp; }
        public String getRawData() { return rawData; }
        public ConfidenceScore getOriginalConfidence() { return originalConfidence; }
        public Map<String, String> getOriginalMetadata() { return new HashMap<>(originalMetadata); }
        
        @Override
        public String toString() {
            return String.format("ClaimProvenance{source=%s, confidence=%s, timestamp=%s}", 
                source, originalConfidence, timestamp);
        }
    }
}
