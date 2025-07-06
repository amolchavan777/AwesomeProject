package com.example.mapper.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.time.Instant;

/**
 * Claim that one service depends on another.
 *
 * <pre>{@code
 * DependencyClaim claim = new DependencyClaim();
 * claim.setFromService(a);
 * claim.setToService(b);
 * claim.setSource("manual");
 * }</pre>
 */
@Entity
public class DependencyClaim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private ApplicationService fromService;
    @ManyToOne
    private ApplicationService toService;
    private String source;
    private double confidence;
    private Instant timestamp;

    // Added fields for provenance and audit trail
    @Column(length = 2048)
    private String provenance; // JSON or string with provenance info
    @Column(length = 2048)
    private String auditTrail; // JSON or string with audit trail

    // Constructors
    public DependencyClaim() {
        // Default constructor for JPA
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ApplicationService getFromService() {
        return fromService;
    }

    public void setFromService(ApplicationService fromService) {
        this.fromService = fromService;
    }

    public ApplicationService getToService() {
        return toService;
    }

    public void setToService(ApplicationService toService) {
        this.toService = toService;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }

    public String getAuditTrail() {
        return auditTrail;
    }

    public void setAuditTrail(String auditTrail) {
        this.auditTrail = auditTrail;
    }
}
