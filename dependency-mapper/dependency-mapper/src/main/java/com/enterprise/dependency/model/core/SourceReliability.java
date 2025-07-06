package com.example.mapper.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Column;

/**
 * Entity to track reliability score for each data source.
 */
@Entity
public class SourceReliability {
    @Id
    @Column(length = 100)
    private String sourceName;
    private double reliability; // 0.0 to 1.0
    private int claimCount;
    private int correctCount;

    public SourceReliability() {}
    public SourceReliability(String sourceName, double reliability) {
        this.sourceName = sourceName;
        this.reliability = reliability;
        this.claimCount = 0;
        this.correctCount = 0;
    }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public double getReliability() { return reliability; }
    public void setReliability(double reliability) { this.reliability = reliability; }
    public int getClaimCount() { return claimCount; }
    public void setClaimCount(int claimCount) { this.claimCount = claimCount; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
}
