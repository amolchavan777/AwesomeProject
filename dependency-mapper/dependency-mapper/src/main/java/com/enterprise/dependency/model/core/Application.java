package com.enterprise.dependency.model.core;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.CollectionTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.JoinColumn;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * Represents an application or service in the enterprise dependency graph.
 * 
 * <p>This entity stores information about applications including their name,
 * type, environment, and additional metadata for service discovery and mapping.
 * 
 * <p>Usage example:
 * <pre>{@code
 * Application app = Application.builder()
 *     .name("user-service")
 *     .type("microservice")
 *     .environment("production")
 *     .owner("platform-team")
 *     .build();
 * }</pre>
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
@Entity
@Table(name = "applications", indexes = {
    @Index(name = "idx_app_name", columnList = "name", unique = true),
    @Index(name = "idx_app_type_env", columnList = "type, environment")
})
public class Application {
    
    // Constants for common values
    private static final String UNKNOWN = "unknown";
    private static final String ACTIVE = "active";
    private static final String MEDIUM = "medium";
    private static final String PRODUCTION = "production";
    private static final String PROD = "prod";
    private static final String DEPRECATED = "deprecated";
    private static final String HIGH = "high";
    private static final String CRITICAL = "critical";
    
    /**
     * Unique identifier for this application.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Application name (must be unique).
     */
    @Column(nullable = false, unique = true, length = 255)
    private String name;
    
    /**
     * Type of application (e.g., "microservice", "database", "web-app").
     */
    @Column(length = 100)
    private String type = UNKNOWN;
    
    /**
     * Environment where this application runs (e.g., "production", "staging", "development").
     */
    @Column(length = 50)
    private String environment = UNKNOWN;
    
    /**
     * Team or person responsible for this application.
     */
    @Column(length = 100)
    private String owner;
    
    /**
     * Version or build number of this application.
     */
    @Column(length = 50)
    private String version;
    
    /**
     * Primary URL or endpoint for this application.
     */
    @Column(length = 500)
    private String primaryUrl;
    
    /**
     * Status of the application (e.g., "active", "deprecated", "decommissioned").
     */
    @Column(length = 50)
    private String status = ACTIVE;
    
    /**
     * Timestamp when this application was first discovered or registered.
     */
    @Column(nullable = false)
    private Instant discoveredAt = Instant.now();
    
    /**
     * Timestamp when this application information was last updated.
     */
    @Column(nullable = false)
    private Instant lastUpdatedAt = Instant.now();
    
    /**
     * Additional metadata about this application (tags, labels, etc.).
     */
    @ElementCollection
    @CollectionTable(name = "application_metadata", joinColumns = @JoinColumn(name = "application_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata = new HashMap<>();
    
    /**
     * Criticality level of this application (e.g., "low", "medium", "high", "critical").
     */
    @Column(length = 20)
    private String criticality = MEDIUM;
    
    /**
     * Description of the application's purpose or functionality.
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * Default constructor for JPA.
     */
    public Application() {
        this.type = UNKNOWN;
        this.environment = UNKNOWN;
        this.status = ACTIVE;
        this.discoveredAt = Instant.now();
        this.lastUpdatedAt = Instant.now();
        this.metadata = new HashMap<>();
        this.criticality = MEDIUM;
    }
    
    /**
     * Constructor with all fields.
     */
    public Application(Long id, String name, String type, String environment, String owner, 
                      String version, String primaryUrl, String status, Instant discoveredAt, 
                      Instant lastUpdatedAt, Map<String, String> metadata, String criticality, 
                      String description) {
        this();  // Call default constructor to set defaults
        this.id = id;
        this.name = name;
        if (type != null) this.type = type;
        if (environment != null) this.environment = environment;
        this.owner = owner;
        this.version = version;
        this.primaryUrl = primaryUrl;
        if (status != null) this.status = status;
        if (discoveredAt != null) this.discoveredAt = discoveredAt;
        if (lastUpdatedAt != null) this.lastUpdatedAt = lastUpdatedAt;
        if (metadata != null) this.metadata = metadata;
        if (criticality != null) this.criticality = criticality;
        this.description = description;
    }
    
    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getEnvironment() { return environment; }
    public String getOwner() { return owner; }
    public String getVersion() { return version; }
    public String getPrimaryUrl() { return primaryUrl; }
    public String getStatus() { return status; }
    public Instant getDiscoveredAt() { return discoveredAt; }
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    public Map<String, String> getMetadata() { return metadata; }
    public String getCriticality() { return criticality; }
    public String getDescription() { return description; }
    
    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setVersion(String version) { this.version = version; }
    public void setPrimaryUrl(String primaryUrl) { this.primaryUrl = primaryUrl; }
    public void setStatus(String status) { this.status = status; }
    public void setDiscoveredAt(Instant discoveredAt) { this.discoveredAt = discoveredAt; }
    public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public void setCriticality(String criticality) { this.criticality = criticality; }
    public void setDescription(String description) { this.description = description; }
    
    /**
     * Create a new Builder instance.
     * 
     * @return new ApplicationBuilder
     */
    public static ApplicationBuilder builder() {
        return new ApplicationBuilder();
    }
    
    /**
     * Create a builder with current values for modification.
     * 
     * @return new ApplicationBuilder with current values
     */
    public ApplicationBuilder toBuilder() {
        return new ApplicationBuilder()
            .id(this.id)
            .name(this.name)
            .type(this.type)
            .environment(this.environment)
            .owner(this.owner)
            .version(this.version)
            .primaryUrl(this.primaryUrl)
            .status(this.status)
            .discoveredAt(this.discoveredAt)
            .lastUpdatedAt(this.lastUpdatedAt)
            .metadata(new HashMap<>(this.metadata))
            .criticality(this.criticality)
            .description(this.description);
    }
    
    /**
     * Builder class for Application.
     */
    public static class ApplicationBuilder {
        private Long id;
        private String name;
        private String type = UNKNOWN;
        private String environment = UNKNOWN;
        private String owner;
        private String version;
        private String primaryUrl;
        private String status = ACTIVE;
        private Instant discoveredAt = Instant.now();
        private Instant lastUpdatedAt = Instant.now();
        private Map<String, String> metadata = new HashMap<>();
        private String criticality = MEDIUM;
        private String description;
        
        public ApplicationBuilder id(Long id) { this.id = id; return this; }
        public ApplicationBuilder name(String name) { this.name = name; return this; }
        public ApplicationBuilder type(String type) { this.type = type; return this; }
        public ApplicationBuilder environment(String environment) { this.environment = environment; return this; }
        public ApplicationBuilder owner(String owner) { this.owner = owner; return this; }
        public ApplicationBuilder version(String version) { this.version = version; return this; }
        public ApplicationBuilder primaryUrl(String primaryUrl) { this.primaryUrl = primaryUrl; return this; }
        public ApplicationBuilder status(String status) { this.status = status; return this; }
        public ApplicationBuilder discoveredAt(Instant discoveredAt) { this.discoveredAt = discoveredAt; return this; }
        public ApplicationBuilder lastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; return this; }
        public ApplicationBuilder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
        public ApplicationBuilder criticality(String criticality) { this.criticality = criticality; return this; }
        public ApplicationBuilder description(String description) { this.description = description; return this; }
        
        public Application build() {
            return new Application(id, name, type, environment, owner, version, primaryUrl, 
                                 status, discoveredAt, lastUpdatedAt, metadata, criticality, description);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(name, that.name) &&
               Objects.equals(type, that.type) &&
               Objects.equals(environment, that.environment);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, environment);
    }
    
    /**
     * Check if this application is currently active.
     * 
     * @return true if status is "active"
     */
    public boolean isActive() {
        return ACTIVE.equalsIgnoreCase(status);
    }
    
    /**
     * Check if this application is deprecated.
     * 
     * @return true if status is "deprecated"
     */
    public boolean isDeprecated() {
        return DEPRECATED.equalsIgnoreCase(status);
    }
    
    /**
     * Check if this application is critical.
     * 
     * @return true if criticality is "high" or "critical"
     */
    public boolean isCritical() {
        return HIGH.equalsIgnoreCase(criticality) || CRITICAL.equalsIgnoreCase(criticality);
    }
    
    /**
     * Add metadata to this application.
     * 
     * @param key metadata key
     * @param value metadata value
     * @return this application for method chaining
     */
    public Application addMetadata(String key, String value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        lastUpdatedAt = Instant.now();
        return this;
    }
    
    /**
     * Get metadata value by key.
     * 
     * @param key metadata key
     * @return metadata value or null if not found
     */
    public String getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * Update the last updated timestamp to current time.
     * 
     * @return this application for method chaining
     */
    public Application touch() {
        lastUpdatedAt = Instant.now();
        return this;
    }
    
    /**
     * Create a copy of this application with updated status.
     * 
     * @param newStatus new status value
     * @return new Application instance with updated status
     */
    public Application withStatus(String newStatus) {
        return this.toBuilder()
            .status(newStatus)
            .lastUpdatedAt(Instant.now())
            .build();
    }
    
    /**
     * Check if this application matches the given name (case-insensitive).
     * 
     * @param appName application name to check
     * @return true if names match
     */
    public boolean hasName(String appName) {
        return name != null && name.equalsIgnoreCase(appName);
    }
    
    /**
     * Get a qualified name including environment if different from production.
     * 
     * @return qualified name like "user-service" or "user-service-staging"
     */
    public String getQualifiedName() {
        if (PRODUCTION.equalsIgnoreCase(environment) || PROD.equalsIgnoreCase(environment)) {
            return name;
        }
        return name + "-" + environment;
    }
    
    /**
     * Check if this application is in the specified environment.
     * 
     * @param env environment name to check
     * @return true if environments match (case-insensitive)
     */
    public boolean isInEnvironment(String env) {
        return environment != null && environment.equalsIgnoreCase(env);
    }
    
    /**
     * Get display-friendly information about this application.
     * 
     * @return formatted string with key application details
     */
    public String getDisplayInfo() {
        StringBuilder info = new StringBuilder();
        info.append(name);
        
        if (type != null && !UNKNOWN.equals(type)) {
            info.append(" (").append(type).append(")");
        }
        
        if (environment != null && !UNKNOWN.equals(environment) && !PRODUCTION.equals(environment)) {
            info.append(" [").append(environment).append("]");
        }
        
        if (owner != null) {
            info.append(" - ").append(owner);
        }
        
        return info.toString();
    }
    
    @Override
    public String toString() {
        return String.format("Application[id=%d, name=%s, type=%s, env=%s, status=%s]", 
            id, name, type, environment, status);
    }
}
