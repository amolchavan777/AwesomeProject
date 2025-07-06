package com.enterprise.dependency.model.core;

/**
 * Enumeration representing different types of dependencies between applications.
 * 
 * <p>Each dependency type represents a different kind of relationship:
 * <ul>
 * <li>{@link #API_CALL} - Direct API/REST service calls</li>
 * <li>{@link #DATA_FLOW} - Data transfer or streaming dependencies</li>
 * <li>{@link #RUNTIME} - General runtime dependencies observed in logs</li>
 * <li>{@link #BUILD_TIME} - Dependencies identified during build/compilation</li>
 * <li>{@link #HEALTH_CHECK} - Health monitoring or status check calls</li>
 * <li>{@link #CONFIGURATION} - Configuration-driven dependencies</li>
 * </ul>
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
public enum DependencyType {
    
    /**
     * Direct API or REST service call dependency.
     * Typically observed through HTTP/HTTPS traffic analysis.
     */
    API_CALL("API Call", 0.95),
    
    /**
     * Data flow dependency (streaming, batch processing, etc.).
     * Usually involves data transfer between services.
     */
    DATA_FLOW("Data Flow", 0.90),
    
    /**
     * General runtime dependency observed in operational logs.
     * Covers various forms of service-to-service communication.
     */
    RUNTIME("Runtime", 0.85),
    
    /**
     * Build-time dependency identified in code or build artifacts.
     * Highest confidence as it's explicitly declared.
     */
    BUILD_TIME("Build Time", 1.0),
    
    /**
     * Health check or monitoring dependency.
     * Lower confidence as it might not represent functional dependency.
     */
    HEALTH_CHECK("Health Check", 0.60),
    
    /**
     * Configuration-driven dependency.
     * Medium confidence based on configuration analysis.
     */
    CONFIGURATION("Configuration", 0.80);
    
    private final String displayName;
    private final double defaultConfidence;
    
    /**
     * Constructor for dependency type enum values.
     * 
     * @param displayName human-readable name for the dependency type
     * @param defaultConfidence default confidence score for this type (0.0 to 1.0)
     */
    DependencyType(String displayName, double defaultConfidence) {
        this.displayName = displayName;
        this.defaultConfidence = defaultConfidence;
    }
    
    /**
     * Get the human-readable display name for this dependency type.
     * 
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the default confidence score for this dependency type.
     * 
     * @return confidence score between 0.0 and 1.0
     */
    public double getDefaultConfidence() {
        return defaultConfidence;
    }
    
    /**
     * Check if this dependency type represents a high-confidence relationship.
     * 
     * @return true if default confidence is above 0.85
     */
    public boolean isHighConfidence() {
        return defaultConfidence > 0.85;
    }
    
    /**
     * Parse a string representation to a DependencyType enum.
     * 
     * @param typeStr string representation (case-insensitive)
     * @return corresponding DependencyType or RUNTIME as default
     */
    public static DependencyType fromString(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return RUNTIME;
        }
        
        String normalized = typeStr.trim().toUpperCase().replace(" ", "_");
        try {
            return DependencyType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try matching display names
            for (DependencyType type : values()) {
                if (type.displayName.equalsIgnoreCase(typeStr.trim())) {
                    return type;
                }
            }
            return RUNTIME; // Default fallback
        }
    }
}
