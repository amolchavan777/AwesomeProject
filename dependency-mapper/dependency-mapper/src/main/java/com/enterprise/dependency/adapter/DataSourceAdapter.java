package com.enterprise.dependency.adapter;

import com.example.mapper.model.DependencyClaim;
import java.util.List;

/**
 * Interface for data source adapters that convert various data formats
 * into normalized dependency claims.
 */
public interface DataSourceAdapter {
    
    /**
     * Gets the name of this adapter for logging and provenance tracking.
     * 
     * @return adapter name
     */
    String getAdapterName();
    
    /**
     * Processes data from a source and returns a list of dependency claims.
     * 
     * @param sourceData raw data from the source
     * @return list of dependency claims extracted from the data
     * @throws AdapterException if processing fails
     */
    List<DependencyClaim> processData(String sourceData) throws AdapterException;
    
    /**
     * Validates if the given data can be processed by this adapter.
     * 
     * @param sourceData raw data to validate
     * @return true if this adapter can process the data
     */
    boolean canProcess(String sourceData);
    
    /**
     * Gets the confidence level for claims produced by this adapter.
     * 
     * @return confidence score between 0.0 and 1.0
     */
    double getDefaultConfidence();
}
