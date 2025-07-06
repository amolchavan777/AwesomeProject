package com.enterprise.dependency.web;

import com.enterprise.dependency.service.ConflictDetectionService;
import com.enterprise.dependency.service.ConflictDetectionService.ConflictReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for conflict detection and analysis.
 * 
 * <p>Provides endpoints for:
 * <ul>
 * <li>Detecting all conflicts in the system</li>
 * <li>Analyzing specific dependency pairs</li>
 * <li>Getting conflict statistics</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * curl http://localhost:8080/api/conflicts/all
 * curl http://localhost:8080/api/conflicts/dependency?from=web-app&to=user-service
 * curl http://localhost:8080/api/conflicts/stats
 * }</pre>
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/conflicts")
@CrossOrigin(origins = "*")
public class ConflictController {
    
    private static final Logger log = LoggerFactory.getLogger(ConflictController.class);
    
    private final ConflictDetectionService conflictDetectionService;
    
    @Autowired
    public ConflictController(ConflictDetectionService conflictDetectionService) {
        this.conflictDetectionService = conflictDetectionService;
    }
    
    /**
     * Detects and returns all conflicts in the system.
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllConflicts() {
        try {
            log.info("Detecting all conflicts");
            List<ConflictReport> conflicts = conflictDetectionService.detectAllConflicts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalConflicts", conflicts.size());
            response.put("conflicts", conflicts.stream()
                .map(this::convertConflictToMap)
                .collect(Collectors.toList()));
            
            // Add summary statistics
            Map<String, Long> conflictsByType = conflicts.stream()
                .collect(Collectors.groupingBy(
                    conflict -> conflict.getType().name(),
                    Collectors.counting()
                ));
            
            Map<String, Long> conflictsBySeverity = conflicts.stream()
                .collect(Collectors.groupingBy(
                    conflict -> conflict.getSeverity().name(),
                    Collectors.counting()
                ));
            
            response.put("conflictsByType", conflictsByType);
            response.put("conflictsBySeverity", conflictsBySeverity);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to detect conflicts", e);
            return ResponseEntity.status(500)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Detects conflicts for a specific dependency pair.
     */
    @GetMapping("/dependency")
    public ResponseEntity<Map<String, Object>> getDependencyConflicts(
            @RequestParam("from") String fromService,
            @RequestParam("to") String toService) {
        
        try {
            log.info("Detecting conflicts for dependency: {} -> {}", fromService, toService);
            
            List<ConflictReport> conflicts = conflictDetectionService.detectConflictsForDependency(
                fromService, toService);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("dependency", Map.of("from", fromService, "to", toService));
            response.put("conflictsFound", conflicts.size());
            response.put("conflicts", conflicts.stream()
                .map(this::convertConflictToMap)
                .collect(Collectors.toList()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to detect conflicts for dependency: {} -> {}", fromService, toService, e);
            return ResponseEntity.status(500)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Gets conflict detection statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getConflictStats() {
        try {
            List<ConflictReport> allConflicts = conflictDetectionService.detectAllConflicts();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalConflicts", allConflicts.size());
            
            // Statistics by type
            Map<String, Long> byType = allConflicts.stream()
                .collect(Collectors.groupingBy(
                    conflict -> conflict.getType().getDisplayName(),
                    Collectors.counting()
                ));
            stats.put("conflictsByType", byType);
            
            // Statistics by severity
            Map<String, Long> bySeverity = allConflicts.stream()
                .collect(Collectors.groupingBy(
                    conflict -> conflict.getSeverity().getDisplayName(),
                    Collectors.counting()
                ));
            stats.put("conflictsBySeverity", bySeverity);
            
            // Most conflicted dependencies
            Map<String, Long> dependencyConflictCounts = allConflicts.stream()
                .collect(Collectors.groupingBy(
                    conflict -> conflict.getDependency().toString(),
                    Collectors.counting()
                ));
            
            List<Map<String, Object>> mostConflicted = dependencyConflictCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("dependency", entry.getKey());
                    map.put("conflictCount", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
            
            stats.put("mostConflictedDependencies", mostConflicted);
            stats.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Failed to get conflict statistics", e);
            return ResponseEntity.status(500)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Converts a ConflictReport to a Map for JSON serialization.
     */
    private Map<String, Object> convertConflictToMap(ConflictReport conflict) {
        Map<String, Object> conflictMap = new HashMap<>();
        conflictMap.put("type", conflict.getType().getDisplayName());
        conflictMap.put("typeDescription", conflict.getType().getDescription());
        conflictMap.put("severity", conflict.getSeverity().getDisplayName());
        conflictMap.put("severityDescription", conflict.getSeverity().getDescription());
        conflictMap.put("dependency", Map.of(
            "from", conflict.getDependency().getFromService(),
            "to", conflict.getDependency().getToService()
        ));
        conflictMap.put("description", conflict.getDescription());
        conflictMap.put("detectedAt", conflict.getDetectedAt().toString());
        conflictMap.put("conflictingClaimsCount", conflict.getConflictingClaims().size());
        
        // Add details about conflicting claims
        List<Map<String, Object>> claimDetails = conflict.getConflictingClaims().stream()
            .map(claim -> {
                Map<String, Object> claimMap = new HashMap<>();
                claimMap.put("source", claim.getSource());
                claimMap.put("confidence", claim.getConfidence());
                claimMap.put("timestamp", claim.getTimestamp().toString());
                return claimMap;
            })
            .collect(Collectors.toList());
        conflictMap.put("conflictingClaims", claimDetails);
        
        return conflictMap;
    }
}
