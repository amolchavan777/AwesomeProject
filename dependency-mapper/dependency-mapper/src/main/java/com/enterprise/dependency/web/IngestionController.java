package com.enterprise.dependency.web;

import com.enterprise.dependency.service.IngestionService;
import com.enterprise.dependency.service.IngestionResult;
import com.enterprise.dependency.service.IngestionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for data ingestion endpoints.
 * 
 * <p>Provides endpoints for:
 * <ul>
 * <li>File upload and ingestion</li>
 * <li>Raw data string ingestion</li>
 * <li>Ingestion status and statistics</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * curl -X POST -F "file=@router.log" http://localhost:8080/api/ingest/file
 * curl -X POST -H "Content-Type: application/json" \
 *      -d '{"data":"log data", "sourceType":"ROUTER_LOG"}' \
 *      http://localhost:8080/api/ingest/data
 * }</pre>
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/ingest")
@CrossOrigin(origins = "*")
public class IngestionController {
    
    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);
    
    private final IngestionService ingestionService;
    
    @Autowired
    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }
    
    /**
     * Ingests data from an uploaded file.
     * 
     * @param file the uploaded file
     * @param sourceId optional source identifier
     * @return ingestion result
     */
    @PostMapping("/file")
    public ResponseEntity<Map<String, Object>> ingestFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceId", required = false) String sourceId) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("File is empty", null));
        }
        
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String filename = file.getOriginalFilename();
            
            // Use filename as sourceId if not provided
            if (sourceId == null || sourceId.trim().isEmpty()) {
                sourceId = filename;
            }
            
            log.info("Processing file upload: {} (size: {} bytes, sourceId: {})", 
                filename, file.getSize(), sourceId);
            
            IngestionService.SourceType sourceType = detectSourceTypeFromFilename(filename);
            IngestionResult result = ingestionService.ingestFromString(content, sourceType, sourceId);
            
            return ResponseEntity.ok(createSuccessResponse(result));
            
        } catch (IOException e) {
            log.error("Failed to read uploaded file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to read file", e.getMessage()));
                
        } catch (IngestionException e) {
            log.error("Ingestion failed for file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(createErrorResponse("Ingestion failed", e.getMessage()));
                
        } catch (Exception e) {
            log.error("Unexpected error processing file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Unexpected error", e.getMessage()));
        }
    }
    
    /**
     * Ingests data from a raw string.
     */
    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> ingestData(@RequestBody DataIngestionRequest request) {
        
        if (request.getData() == null || request.getData().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Data is empty", null));
        }
        
        try {
            IngestionService.SourceType sourceType = request.getSourceType() != null 
                ? request.getSourceType() 
                : IngestionService.SourceType.ROUTER_LOG; // default
                
            String sourceId = request.getSourceId() != null 
                ? request.getSourceId() 
                : "api-data-" + System.currentTimeMillis();
            
            log.info("Processing data ingestion: sourceType={}, sourceId={}, dataLength={}", 
                sourceType, sourceId, request.getData().length());
            
            IngestionResult result = ingestionService.ingestFromString(
                request.getData(), sourceType, sourceId);
            
            return ResponseEntity.ok(createSuccessResponse(result));
            
        } catch (IngestionException e) {
            log.error("Ingestion failed for data request", e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(createErrorResponse("Ingestion failed", e.getMessage()));
                
        } catch (Exception e) {
            log.error("Unexpected error processing data request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Unexpected error", e.getMessage()));
        }
    }
    
    /**
     * Ingests custom dependency data in simple text format.
     * Each line: fromService -> toService [confidence] [source] [timestamp]
     */
    @PostMapping("/custom-text")
    public ResponseEntity<Map<String, Object>> ingestCustomText(@RequestBody String data,
            @RequestParam(value = "sourceId", required = false) String sourceId) {
        if (data == null || data.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("Data is empty", null));
        }
        try {
            if (sourceId == null || sourceId.trim().isEmpty()) {
                sourceId = "custom-text-" + System.currentTimeMillis();
            }
            IngestionResult result = ingestionService.ingestFromString(
                data, IngestionService.SourceType.CUSTOM_TEXT, sourceId);
            return ResponseEntity.ok(createSuccessResponse(result));
        } catch (Exception e) {
            log.error("Custom text ingestion failed", e);
            return ResponseEntity.status(422).body(createErrorResponse("Custom text ingestion failed", e.getMessage()));
        }
    }
    
    /**
     * Gets ingestion statistics and system status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "healthy");
        status.put("timestamp", System.currentTimeMillis());
        status.put("supportedSourceTypes", IngestionService.SourceType.values());
        
        return ResponseEntity.ok(status);
    }
    
    private IngestionService.SourceType detectSourceTypeFromFilename(String filename) {
        if (filename == null) {
            return IngestionService.SourceType.ROUTER_LOG;
        }
        
        String lower = filename.toLowerCase();
        if (lower.contains("router") || lower.contains("access") || lower.contains(".log")) {
            return IngestionService.SourceType.ROUTER_LOG;
        }
        if (lower.contains("nmap") || lower.contains("scan") || lower.contains("network")) {
            return IngestionService.SourceType.NETWORK_DISCOVERY;
        }
        if (lower.contains("application.properties") || lower.contains("config") || lower.contains(".properties")) {
            return IngestionService.SourceType.CONFIGURATION_FILE;
        }
        
        return IngestionService.SourceType.ROUTER_LOG; // default
    }
    
    private Map<String, Object> createSuccessResponse(IngestionResult result) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", Map.of(
            "sourceType", result.getSourceType(),
            "sourceId", result.getSourceId(),
            "rawClaimsExtracted", result.getRawClaimsExtracted(),
            "claimsAfterNormalization", result.getClaimsAfterNormalization(),
            "claimsSaved", result.getClaimsSaved(),
            "processingTimeMs", result.getProcessingTimeMs()
        ));
        return response;
    }
    
    private Map<String, Object> createErrorResponse(String message, String details) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        if (details != null) {
            response.put("details", details);
        }
        return response;
    }
    
    /**
     * Request object for data ingestion endpoint.
     */
    public static class DataIngestionRequest {
        private String data;
        private IngestionService.SourceType sourceType;
        private String sourceId;
        
        // Getters and setters
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        
        public IngestionService.SourceType getSourceType() { return sourceType; }
        public void setSourceType(IngestionService.SourceType sourceType) { this.sourceType = sourceType; }
        
        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    }
}
