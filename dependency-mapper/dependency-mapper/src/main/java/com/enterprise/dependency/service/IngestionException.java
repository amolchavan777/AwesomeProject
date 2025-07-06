package com.enterprise.dependency.service;

/**
 * Exception thrown during the ingestion pipeline process.
 */
public class IngestionException extends Exception {
    
    private final String sourceId;
    
    public IngestionException(String message) {
        super(message);
        this.sourceId = null;
    }
    
    public IngestionException(String message, Throwable cause) {
        super(message, cause);
        this.sourceId = null;
    }
    
    public IngestionException(String sourceId, String message) {
        super(message);
        this.sourceId = sourceId;
    }
    
    public IngestionException(String sourceId, String message, Throwable cause) {
        super(message, cause);
        this.sourceId = sourceId;
    }
    
    public String getSourceId() {
        return sourceId;
    }
    
    @Override
    public String getMessage() {
        if (sourceId != null) {
            return "[" + sourceId + "] " + super.getMessage();
        }
        return super.getMessage();
    }
}
