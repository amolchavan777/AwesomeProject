package com.example.mapper.service;

import com.example.mapper.model.DependencyClaim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class AuditTrailService {
    private static final Logger auditLogger = LoggerFactory.getLogger("AuditTrailLogger");

    public void logClaimEvent(DependencyClaim claim, String eventType, String details) {
        String logEntry = String.format("%s | %s | from=%s | to=%s | source=%s | conf=%.2f | event=%s | details=%s",
                Instant.now(),
                claim.getId(),
                claim.getFromService() != null ? claim.getFromService().getName() : "?",
                claim.getToService() != null ? claim.getToService().getName() : "?",
                claim.getSource(),
                claim.getConfidence(),
                eventType,
                details);
        auditLogger.info(logEntry);
    }
}
