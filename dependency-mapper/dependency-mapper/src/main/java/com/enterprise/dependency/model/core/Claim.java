package com.enterprise.dependency.model.core;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.CollectionTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.JoinColumn;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * Represents a dependency claim between two applications from a specific data source.
 * 
 * <p>A claim is an assertion that one application depends on another, along with
 * contextual information about the confidence, source, and metadata of this relationship.
 * 
 * <p>Usage example:
 * <pre>{@code
 * Claim claim = Claim.builder()
 *     .fromApplication("web-portal")
 *     .toApplication("user-service")
 *     .dependencyType(DependencyType.API_CALL)
 *     .confidence(ConfidenceScore.HIGH)
 *     .source("router-log")
 *     .timestamp(Instant.now())
 *     .rawData("GET /api/users 200 125ms")
 *     .build();
 * }</pre>
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
@Entity
public class Claim {
    
    // Constants for configurable weights
    private static final double CONFIDENCE_WEIGHT = 0.7;
    private static final double PRIORITY_WEIGHT = 0.3;
    
    /**
     * Unique identifier for this claim.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Name of the source application that depends on another.
     */
    @Column(nullable = false, length = 255)
    private String fromApplication;
    
    /**
     * Name of the target application being depended upon.
     */
    @Column(nullable = false, length = 255)
    private String toApplication;
    
    /**
     * Type of dependency relationship.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DependencyType dependencyType;
    
    /**
     * Confidence level for this dependency claim.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfidenceScore confidence;
    
    /**
     * Data source that provided this claim (e.g., "router-log", "codebase", "api-gateway").
     */
    @Column(nullable = false, length = 100)
    private String source;
    
    /**
     * Timestamp when this dependency was observed or when the claim was created.
     */
    @Column(nullable = false)
    private Instant timestamp;
    
    /**
     * Raw data from which this claim was extracted (for debugging and auditing).
     */
    @Column(columnDefinition = "TEXT")
    private String rawData;
    
    /**
     * Additional metadata about this claim (e.g., HTTP status, response time, etc.).
     */
    @ElementCollection
    @CollectionTable(name = "claim_metadata", joinColumns = @JoinColumn(name = "claim_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * Version number for optimistic locking and change tracking.
     */
    @Column(nullable = false)
    private Long version = 1L;
    
    /**
     * Flag indicating whether this claim has been verified or validated.
     */
    @Column(nullable = false)
    private Boolean verified = false;
    
    /**
     * Priority score used for conflict resolution when multiple claims exist for the same dependency.
     */
    @Column(nullable = false)
    private Double priority = 1.0;
    
    /**
     * Default constructor for JPA.
     */
    public Claim() {
        this.metadata = new HashMap<>();
        this.version = 1L;
        this.verified = false;
        this.priority = 1.0;
    }
    
    /**
     * Constructor with all fields.
     */
    public Claim(Long id, String fromApplication, String toApplication, DependencyType dependencyType, 
                ConfidenceScore confidence, String source, Instant timestamp, String rawData, 
                Map<String, Object> metadata, Long version, Boolean verified, Double priority) {
        this();  // Set defaults first
        this.id = id;
        this.fromApplication = fromApplication;
        this.toApplication = toApplication;
        this.dependencyType = dependencyType;
        this.confidence = confidence;
        this.source = source;
        this.timestamp = timestamp;
        this.rawData = rawData;
        if (metadata != null) this.metadata = metadata;
        if (version != null) this.version = version;
        if (verified != null) this.verified = verified;
        if (priority != null) this.priority = priority;
    }
    
    // Getters
    public Long getId() { return id; }
    public String getFromApplication() { return fromApplication; }
    public String getToApplication() { return toApplication; }
    public DependencyType getDependencyType() { return dependencyType; }
    public ConfidenceScore getConfidence() { return confidence; }
    public String getSource() { return source; }
    public Instant getTimestamp() { return timestamp; }
    public String getRawData() { return rawData; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Long getVersion() { return version; }
    public Boolean getVerified() { return verified; }
    public Double getPriority() { return priority; }
    
    // Setters
    public void setId(Long id) { this.id = id; }
    public void setFromApplication(String fromApplication) { this.fromApplication = fromApplication; }
    public void setToApplication(String toApplication) { this.toApplication = toApplication; }
    public void setDependencyType(DependencyType dependencyType) { this.dependencyType = dependencyType; }
    public void setConfidence(ConfidenceScore confidence) { this.confidence = confidence; }
    public void setSource(String source) { this.source = source; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public void setRawData(String rawData) { this.rawData = rawData; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public void setVersion(Long version) { this.version = version; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public void setPriority(Double priority) { this.priority = priority; }
    
    /**
     * Create a new Builder instance.
     * 
     * @return new ClaimBuilder
     */
    public static ClaimBuilder builder() {
        return new ClaimBuilder();
    }
    
    /**
     * Create a builder with current values for modification.
     * 
     * @return new ClaimBuilder with current values
     */
    public ClaimBuilder toBuilder() {
        return new ClaimBuilder()
            .id(this.id)
            .fromApplication(this.fromApplication)
            .toApplication(this.toApplication)
            .dependencyType(this.dependencyType)
            .confidence(this.confidence)
            .source(this.source)
            .timestamp(this.timestamp)
            .rawData(this.rawData)
            .metadata(new HashMap<>(this.metadata))
            .version(this.version)
            .verified(this.verified)
            .priority(this.priority);
    }
    
    /**
     * Builder class for Claim.
     */
    public static class ClaimBuilder {
        private Long id;
        private String fromApplication;
        private String toApplication;
        private DependencyType dependencyType;
        private ConfidenceScore confidence;
        private String source;
        private Instant timestamp;
        private String rawData;
        private Map<String, Object> metadata = new HashMap<>();
        private Long version = 1L;
        private Boolean verified = false;
        private Double priority = 1.0;
        
        public ClaimBuilder id(Long id) { this.id = id; return this; }
        public ClaimBuilder fromApplication(String fromApplication) { this.fromApplication = fromApplication; return this; }
        public ClaimBuilder toApplication(String toApplication) { this.toApplication = toApplication; return this; }
        public ClaimBuilder dependencyType(DependencyType dependencyType) { this.dependencyType = dependencyType; return this; }
        public ClaimBuilder confidence(ConfidenceScore confidence) { this.confidence = confidence; return this; }
        public ClaimBuilder source(String source) { this.source = source; return this; }
        public ClaimBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public ClaimBuilder rawData(String rawData) { this.rawData = rawData; return this; }
        public ClaimBuilder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }
        public ClaimBuilder version(Long version) { this.version = version; return this; }
        public ClaimBuilder verified(Boolean verified) { this.verified = verified; return this; }
        public ClaimBuilder priority(Double priority) { this.priority = priority; return this; }
        
        public Claim build() {
            return new Claim(id, fromApplication, toApplication, dependencyType, confidence, 
                           source, timestamp, rawData, metadata, version, verified, priority);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Claim claim = (Claim) o;
        return Objects.equals(id, claim.id) &&
               Objects.equals(fromApplication, claim.fromApplication) &&
               Objects.equals(toApplication, claim.toApplication) &&
               Objects.equals(dependencyType, claim.dependencyType) &&
               Objects.equals(source, claim.source);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, fromApplication, toApplication, dependencyType, source);
    }
    
    /**
     * Get the numeric confidence value.
     * 
     * @return confidence value between 0.0 and 1.0
     */
    public double getConfidenceValue() {
        return confidence != null ? confidence.getValue() : 0.5;
    }
    
    /**
     * Check if this claim represents a reliable dependency.
     * 
     * @return true if confidence is HIGH or VERY_HIGH
     */
    public boolean isReliable() {
        return confidence != null && confidence.isReliable();
    }
    
    /**
     * Get a string representation of the dependency edge.
     * 
     * @return formatted string like "web-portal -> user-service"
     */
    public String getDependencyEdge() {
        return String.format("%s -> %s", fromApplication, toApplication);
    }
    
    /**
     * Add metadata to this claim.
     * 
     * @param key metadata key
     * @param value metadata value
     * @return this claim for method chaining
     */
    public Claim addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        return this;
    }
    
    /**
     * Get metadata value by key.
     * 
     * @param key metadata key
     * @return metadata value or null if not found
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * Get metadata value by key with type casting.
     * 
     * @param key metadata key
     * @param type expected type class
     * @param <T> type parameter
     * @return typed metadata value or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = getMetadata(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Calculate a weighted score combining confidence and priority.
     * 
     * @return weighted score for conflict resolution
     */
    public double getWeightedScore() {
        double confidenceWeight = getConfidenceValue();
        double priorityWeight = priority != null ? priority : 1.0;
        
        return (confidenceWeight * CONFIDENCE_WEIGHT) + (priorityWeight * PRIORITY_WEIGHT);
    }
    
    /**
     * Create a copy of this claim with updated confidence.
     * 
     * @param newConfidence new confidence score
     * @return new Claim instance with updated confidence
     */
    public Claim withConfidence(ConfidenceScore newConfidence) {
        return this.toBuilder()
            .confidence(newConfidence)
            .version(version + 1)
            .build();
    }
    
    /**
     * Create a copy of this claim marked as verified.
     * 
     * @return new Claim instance marked as verified
     */
    public Claim markAsVerified() {
        return this.toBuilder()
            .verified(true)
            .version(version + 1)
            .build();
    }
    
    /**
     * Check if this claim conflicts with another claim (same dependency edge, different details).
     * 
     * @param other another claim to compare with
     * @return true if claims are for the same dependency but with different characteristics
     */
    public boolean conflictsWith(Claim other) {
        if (other == null) return false;
        
        // Same dependency edge
        boolean sameEdge = fromApplication.equals(other.fromApplication) 
            && toApplication.equals(other.toApplication);
        
        if (!sameEdge) return false;
        
        // Different dependency types or significantly different confidence
        boolean differentType = !dependencyType.equals(other.dependencyType);
        boolean significantConfidenceDiff = Math.abs(getConfidenceValue() - other.getConfidenceValue()) > 0.3;
        
        return differentType || significantConfidenceDiff;
    }
    
    @Override
    public String toString() {
        return String.format("Claim[id=%d, %s->%s, %s, %s, source=%s]", 
            id, fromApplication, toApplication, dependencyType, confidence, source);
    }
}
