package com.enterprise.dependency.adapter;

/**
 * Exception thrown when a data source adapter encounters an error
 * during data processing.
 */
public class AdapterException extends Exception {
    
    private final String adapterName;
    private final String sourceIdentifier;
    
    public AdapterException(String adapterName, String message) {
        super(message);
        this.adapterName = adapterName;
        this.sourceIdentifier = null;
    }
    
    public AdapterException(String adapterName, String message, Throwable cause) {
        super(message, cause);
        this.adapterName = adapterName;
        this.sourceIdentifier = null;
    }
    
    public AdapterException(String adapterName, String sourceIdentifier, String message) {
        super(message);
        this.adapterName = adapterName;
        this.sourceIdentifier = sourceIdentifier;
    }
    
    public AdapterException(String adapterName, String sourceIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.adapterName = adapterName;
        this.sourceIdentifier = sourceIdentifier;
    }
    
    public String getAdapterName() {
        return adapterName;
    }
    
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }
    
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(adapterName).append("]");
        if (sourceIdentifier != null) {
            sb.append(" (").append(sourceIdentifier).append(")");
        }
        sb.append(": ").append(super.getMessage());
        return sb.toString();
    }
}
