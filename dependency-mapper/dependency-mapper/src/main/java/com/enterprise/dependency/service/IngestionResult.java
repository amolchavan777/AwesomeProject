package com.enterprise.dependency.service;

import com.enterprise.dependency.service.IngestionService.SourceType;
import java.time.Instant;

/**
 * Result of an ingestion operation containing statistics and metadata.
 */
public class IngestionResult {
    
    private final SourceType sourceType;
    private final String sourceId;
    private final Instant startTime;
    private final int rawClaimsExtracted;
    private final int claimsAfterNormalization;
    private final int claimsSaved;
    private final long processingTimeMs;
    
    private IngestionResult(Builder builder) {
        this.sourceType = builder.sourceType;
        this.sourceId = builder.sourceId;
        this.startTime = builder.startTime;
        this.rawClaimsExtracted = builder.rawClaimsExtracted;
        this.claimsAfterNormalization = builder.claimsAfterNormalization;
        this.claimsSaved = builder.claimsSaved;
        this.processingTimeMs = builder.processingTimeMs;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public SourceType getSourceType() { return sourceType; }
    public String getSourceId() { return sourceId; }
    public Instant getStartTime() { return startTime; }
    public int getRawClaimsExtracted() { return rawClaimsExtracted; }
    public int getClaimsAfterNormalization() { return claimsAfterNormalization; }
    public int getClaimsSaved() { return claimsSaved; }
    public long getProcessingTimeMs() { return processingTimeMs; }
    
    public int getClaimsProcessed() {
        return claimsSaved;
    }
    
    @Override
    public String toString() {
        return String.format(
            "IngestionResult{sourceType=%s, sourceId='%s', extracted=%d, normalized=%d, saved=%d, time=%dms}",
            sourceType, sourceId, rawClaimsExtracted, claimsAfterNormalization, claimsSaved, processingTimeMs
        );
    }
    
    public static class Builder {
        private SourceType sourceType;
        private String sourceId;
        private Instant startTime;
        private int rawClaimsExtracted;
        private int claimsAfterNormalization;
        private int claimsSaved;
        private long processingTimeMs;
        
        public Builder sourceType(SourceType sourceType) {
            this.sourceType = sourceType;
            return this;
        }
        
        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }
        
        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder rawClaimsExtracted(int rawClaimsExtracted) {
            this.rawClaimsExtracted = rawClaimsExtracted;
            return this;
        }
        
        public Builder claimsAfterNormalization(int claimsAfterNormalization) {
            this.claimsAfterNormalization = claimsAfterNormalization;
            return this;
        }
        
        public Builder claimsSaved(int claimsSaved) {
            this.claimsSaved = claimsSaved;
            return this;
        }
        
        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }
        
        public IngestionResult build() {
            return new IngestionResult(this);
        }
    }
}
