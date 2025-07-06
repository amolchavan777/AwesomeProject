package com.enterprise.dependency.model.core;

/**
 * Enumeration representing confidence levels for dependency claims.
 * 
 * <p>Confidence scores indicate how certain we are about a dependency relationship
 * based on the data source and analysis method used to detect it.
 * 
 * <p>Usage example:
 * <pre>{@code
 * ConfidenceScore score = ConfidenceScore.HIGH;
 * double numericValue = score.getValue(); // 0.85
 * boolean isReliable = score.isReliable(); // true
 * }</pre>
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
public enum ConfidenceScore {
    
    /**
     * Very low confidence (0.0 - 0.3).
     * Used for uncertain or potentially noisy data.
     */
    VERY_LOW("Very Low", 0.2),
    
    /**
     * Low confidence (0.3 - 0.5).
     * Used for indirect evidence or high-noise sources.
     */
    LOW("Low", 0.4),
    
    /**
     * Medium confidence (0.5 - 0.7).
     * Used for moderate evidence or partially validated data.
     */
    MEDIUM("Medium", 0.6),
    
    /**
     * High confidence (0.7 - 0.9).
     * Used for strong evidence from reliable sources.
     */
    HIGH("High", 0.85),
    
    /**
     * Very high confidence (0.9 - 1.0).
     * Used for explicit declarations or verified relationships.
     */
    VERY_HIGH("Very High", 0.95);
    
    private final String displayName;
    private final double value;
    
    /**
     * Constructor for confidence score enum values.
     * 
     * @param displayName human-readable name for the confidence level
     * @param value numeric confidence value between 0.0 and 1.0
     */
    ConfidenceScore(String displayName, double value) {
        this.displayName = displayName;
        this.value = value;
    }
    
    /**
     * Get the numeric confidence value.
     * 
     * @return confidence value between 0.0 and 1.0
     */
    public double getValue() {
        return value;
    }
    
    /**
     * Get the human-readable display name.
     * 
     * @return display name for this confidence level
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if this confidence level is considered reliable.
     * 
     * @return true if confidence value is 0.7 or higher
     */
    public boolean isReliable() {
        return value >= 0.7;
    }
    
    /**
     * Check if this confidence level is considered strong.
     * 
     * @return true if confidence value is 0.85 or higher
     */
    public boolean isStrong() {
        return value >= 0.85;
    }
    
    /**
     * Convert a numeric confidence value to the appropriate enum.
     * 
     * @param numericValue confidence value between 0.0 and 1.0
     * @return closest matching ConfidenceScore enum
     * @throws IllegalArgumentException if value is outside valid range
     */
    public static ConfidenceScore fromValue(double numericValue) {
        if (numericValue < 0.0 || numericValue > 1.0) {
            throw new IllegalArgumentException(
                "Confidence value must be between 0.0 and 1.0, got: " + numericValue);
        }
        
        if (numericValue >= 0.9) return VERY_HIGH;
        if (numericValue >= 0.7) return HIGH;
        if (numericValue >= 0.5) return MEDIUM;
        if (numericValue >= 0.3) return LOW;
        return VERY_LOW;
    }
    
    /**
     * Parse a string representation to a ConfidenceScore enum.
     * 
     * @param scoreStr string representation (case-insensitive)
     * @return corresponding ConfidenceScore or MEDIUM as default
     */
    public static ConfidenceScore fromString(String scoreStr) {
        if (scoreStr == null || scoreStr.trim().isEmpty()) {
            return MEDIUM;
        }
        
        // Try parsing as numeric value first
        try {
            double value = Double.parseDouble(scoreStr.trim());
            return fromValue(value);
        } catch (NumberFormatException e) {
            // Continue with string matching
        }
        
        String normalized = scoreStr.trim().toUpperCase().replace(" ", "_");
        try {
            return ConfidenceScore.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try matching display names
            for (ConfidenceScore score : values()) {
                if (score.displayName.equalsIgnoreCase(scoreStr.trim())) {
                    return score;
                }
            }
            return MEDIUM; // Default fallback
        }
    }
    
    /**
     * Combine two confidence scores using a weighted average.
     * 
     * @param other the other confidence score to combine with
     * @param weight weight for this score (0.0 to 1.0), other gets (1.0 - weight)
     * @return new ConfidenceScore representing the combined confidence
     */
    public ConfidenceScore combine(ConfidenceScore other, double weight) {
        if (weight < 0.0 || weight > 1.0) {
            throw new IllegalArgumentException("Weight must be between 0.0 and 1.0");
        }
        
        double combinedValue = (this.value * weight) + (other.value * (1.0 - weight));
        return fromValue(combinedValue);
    }
    
    /**
     * Boost this confidence score by a percentage.
     * 
     * @param percentage boost percentage (e.g., 0.1 for 10% increase)
     * @return new ConfidenceScore with boosted value, capped at 1.0
     */
    public ConfidenceScore boost(double percentage) {
        double boostedValue = Math.min(1.0, this.value * (1.0 + percentage));
        return fromValue(boostedValue);
    }
    
    /**
     * Reduce this confidence score by a percentage.
     * 
     * @param percentage reduction percentage (e.g., 0.1 for 10% decrease)
     * @return new ConfidenceScore with reduced value, floored at 0.0
     */
    public ConfidenceScore reduce(double percentage) {
        double reducedValue = Math.max(0.0, this.value * (1.0 - percentage));
        return fromValue(reducedValue);
    }
}
