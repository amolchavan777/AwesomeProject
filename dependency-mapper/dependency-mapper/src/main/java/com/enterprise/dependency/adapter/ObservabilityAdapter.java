package com.enterprise.dependency.adapter;

import com.enterprise.dependency.model.core.Claim;
import com.enterprise.dependency.model.core.ConfidenceScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced Observability Data Adapter
 * Processes metrics from Prometheus, Jaeger traces, and APM tools
 * 
 * Supports multiple observability formats:
 * - Prometheus metrics with service labels
 * - Jaeger distributed traces 
 * - OpenTelemetry spans
 * - Custom APM data
 */
@Component
public class ObservabilityAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(ObservabilityAdapter.class);
    private static final String SOURCE_TYPE_KEY = "source_type";
    
    // Simplified Prometheus metrics patterns
    private static final Pattern PROMETHEUS_PATTERN = Pattern.compile(
        "^(\\w+)\\{.*service=\"([^\"]+)\".*target_service=\"([^\"]+)\".*\\}\\s+(\\d+(?:\\.\\d+)?)(?:\\s+(\\d+))?$"
    );
    
    // Simplified Jaeger trace patterns  
    private static final Pattern JAEGER_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z)\\s+(\\w+)\\s+\"([^\"]+)\"\\s+->\\s+\"([^\"]+)\"\\s+(\\d+)ms$"
    );
    
    // Simplified OpenTelemetry span patterns
    private static final Pattern OTEL_PATTERN = Pattern.compile(
        "^span_id:(\\w+)\\s+trace_id:(\\w+)\\s+service:(\\w+)\\s+operation:\"([^\"]+)\"\\s+downstream:(\\w+)\\s+duration:(\\d+)ms\\s+status:(\\w+)$"
    );

    /**
     * Parse observability data from various monitoring tools
     */
    public List<Claim> parseObservabilityData(String data) throws AdapterException {
        log.info("Starting observability data parsing");
        List<Claim> claims = new ArrayList<>();
        
        String[] lines = data.split("\n");
        int processedLines = 0;
        int validClaims = 0;
        
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            processedLines++;
            Claim claim = parseLine(line.trim());
            if (claim != null) {
                claims.add(claim);
                validClaims++;
            }
        }
        
        log.info("Observability parsing completed: {} lines processed, {} valid claims extracted", 
                processedLines, validClaims);
        return claims;
    }
    
    private Claim parseLine(String line) {
        // Try Prometheus format first
        Claim claim = parsePrometheusMetric(line);
        if (claim != null) return claim;
        
        // Try Jaeger trace format
        claim = parseJaegerTrace(line);
        if (claim != null) return claim;
        
        // Try OpenTelemetry format
        claim = parseOpenTelemetrySpan(line);
        if (claim != null) return claim;
        
        log.debug("Could not parse observability line: {}", line);
        return null;
    }
    
    private Claim parsePrometheusMetric(String line) {
        Matcher matcher = PROMETHEUS_PATTERN.matcher(line);
        if (!matcher.matches()) return null;
        
        String metricName = matcher.group(1);
        String sourceService = matcher.group(2);
        String targetService = matcher.group(3);
        double metricValue = Double.parseDouble(matcher.group(4));
        
        // Calculate confidence based on metric type and value
        ConfidenceScore confidence = convertToConfidenceScore(calculatePrometheusConfidence(metricName, metricValue));
        
        Claim claim = new Claim();
        claim.setFromApplication(sourceService);
        claim.setToApplication(targetService);
        claim.setConfidence(confidence);
        claim.setSource("prometheus-metrics");
        claim.setTimestamp(Instant.now());
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("metric_name", metricName);
        metadata.put("metric_value", String.valueOf(metricValue));
        metadata.put(SOURCE_TYPE_KEY, "prometheus");
        claim.setMetadata(metadata);
        
        return claim;
    }
    
    private Claim parseJaegerTrace(String line) {
        Matcher matcher = JAEGER_PATTERN.matcher(line);
        if (!matcher.matches()) return null;
        
        String timestamp = matcher.group(1);
        String traceId = matcher.group(2);
        String sourceService = matcher.group(3);
        String targetService = matcher.group(4);
        int durationMs = Integer.parseInt(matcher.group(5));
        
        // Higher confidence for longer-duration calls (more significant dependencies)
        double confidenceValue = Math.min(0.95, 0.7 + (durationMs / 1000.0) * 0.1);
        ConfidenceScore confidence = convertToConfidenceScore(confidenceValue);
        
        Claim claim = new Claim();
        claim.setFromApplication(sourceService);
        claim.setToApplication(targetService);
        claim.setConfidence(confidence);
        claim.setSource("jaeger-traces");
        
        try {
            claim.setTimestamp(Instant.parse(timestamp));
        } catch (Exception e) {
            claim.setTimestamp(Instant.now());
        }
        
        // Add trace metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("trace_id", traceId);
        metadata.put("duration_ms", String.valueOf(durationMs));
        metadata.put(SOURCE_TYPE_KEY, "jaeger");
        claim.setMetadata(metadata);
        
        return claim;
    }
    
    private Claim parseOpenTelemetrySpan(String line) {
        Matcher matcher = OTEL_PATTERN.matcher(line);
        if (!matcher.matches()) return null;
        
        String spanId = matcher.group(1);
        String traceId = matcher.group(2);
        String sourceService = matcher.group(3);
        String operation = matcher.group(4);
        String targetService = matcher.group(5);
        int durationMs = Integer.parseInt(matcher.group(6));
        String status = matcher.group(7);
        
        // Calculate confidence based on status and duration
        double baseConfidence = "OK".equals(status) ? 0.9 : 0.7;
        double durationBonus = Math.min(0.05, durationMs / 10000.0); // Small bonus for longer calls
        double confidenceValue = Math.min(0.98, baseConfidence + durationBonus);
        ConfidenceScore confidence = convertToConfidenceScore(confidenceValue);
        
        Claim claim = new Claim();
        claim.setFromApplication(sourceService);
        claim.setToApplication(targetService);
        claim.setConfidence(confidence);
        claim.setSource("opentelemetry-spans");
        claim.setTimestamp(Instant.now());
        
        // Add OpenTelemetry metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("span_id", spanId);
        metadata.put("trace_id", traceId);
        metadata.put("operation", operation);
        metadata.put("duration_ms", String.valueOf(durationMs));
        metadata.put("status", status);
        metadata.put(SOURCE_TYPE_KEY, "opentelemetry");
        claim.setMetadata(metadata);
        
        return claim;
    }
    
    /**
     * Convert double confidence to ConfidenceScore enum
     */
    private ConfidenceScore convertToConfidenceScore(double value) {
        if (value >= 0.9) return ConfidenceScore.VERY_HIGH;
        if (value >= 0.7) return ConfidenceScore.HIGH;
        if (value >= 0.5) return ConfidenceScore.MEDIUM;
        if (value >= 0.3) return ConfidenceScore.LOW;
        return ConfidenceScore.VERY_LOW;
    }
    
    private double calculatePrometheusConfidence(String metricName, double value) {
        // Different confidence calculation based on metric type
        switch (metricName.toLowerCase()) {
            case "http_requests_total":
                // Higher confidence for more requests
                return Math.min(0.95, 0.6 + Math.log10(Math.max(1, value)) * 0.1);
                
            case "grpc_client_calls_total":
                return Math.min(0.90, 0.7 + Math.log10(Math.max(1, value)) * 0.05);
                
            case "database_connections_active":
                // Active connections indicate strong dependency
                return Math.min(0.98, 0.8 + (value / 100.0) * 0.1);
                
            case "service_dependency_health":
                // Health metrics are very reliable indicators
                return Math.min(0.99, 0.85 + (value / 10.0));
                
            default:
                // Generic metric confidence
                return 0.75;
        }
    }
    
    /**
     * Generate sample observability data for testing
     */
    public String generateSampleData() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Prometheus metrics\n");
        sb.append("http_requests_total{service=\"api-gateway\",target_service=\"auth-service\"} 1250 1720180800\n");
        sb.append("grpc_client_calls_total{service=\"auth-service\",target_service=\"user-db\"} 890 1720180800\n");
        sb.append("database_connections_active{service=\"order-service\",target_service=\"postgres-primary\"} 25 1720180800\n");
        sb.append("\n");
        sb.append("# Jaeger traces\n");
        sb.append("2025-07-05T10:30:45.123Z trace_abc123 \"payment-service\" -> \"stripe-api\" 240ms tags:{method=POST,status=200}\n");
        sb.append("2025-07-05T10:30:46.456Z trace_def456 \"user-service\" -> \"redis-cache\" 15ms tags:{operation=GET,cache_hit=true}\n");
        sb.append("\n");
        sb.append("# OpenTelemetry spans\n");
        sb.append("span_id:span123 trace_id:trace456 service:notification-service operation:\"send_email\" downstream:smtp-relay duration:340ms status:OK\n");
        sb.append("span_id:span789 trace_id:trace101 service:analytics-service operation:\"process_events\" downstream:kafka-cluster duration:125ms status:OK\n");
        return sb.toString();
    }
}
