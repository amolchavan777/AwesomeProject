package com.enterprise.dependency.adapter;

import com.enterprise.dependency.model.core.Claim;
import com.enterprise.dependency.model.core.ConfidenceScore;
import com.enterprise.dependency.model.core.DependencyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter for parsing router/access logs and extracting dependency claims.
 * 
 * <p>Supports common log formats including Apache/Nginx access logs.
 * Example log entry:
 * <pre>{@code
 * 2024-07-04 10:30:45 [INFO] 192.168.1.100 -> 192.168.1.200:8080 GET /api/users 200 125ms
 * }</pre>
 * 
 * <p>Usage:
 * <pre>{@code
 * RouterLogAdapter adapter = new RouterLogAdapter();
 * List<Claim> claims = adapter.parseLogFile("/var/log/router.log");
 * }</pre>
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
@Component
public class RouterLogAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(RouterLogAdapter.class);
    
    // Simplified log pattern - focusing on core functionality for prototype
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}).*?(\\d+\\.\\d+\\.\\d+\\.\\d+).*?(\\d+\\.\\d+\\.\\d+\\.\\d+):(\\d+).*(\\d{3}) (\\d+)ms"
    );
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Parse a router log file and extract dependency claims.
     * 
     * @param logFilePath absolute path to the log file
     * @return list of dependency claims extracted from the log
     * @throws IOException if file cannot be read
     * @throws IllegalArgumentException if logFilePath is null or empty
     */
    public List<Claim> parseLogFile(String logFilePath) throws IOException {
        if (logFilePath == null || logFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Log file path cannot be null or empty");
        }
        
        long startTime = System.currentTimeMillis();
        log.info("Starting router log parsing for file: {}", logFilePath);
        
        List<Claim> claims = new ArrayList<>();
        int totalLines = 0;
        int validLines = 0;
        int errorLines = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                try {
                    Claim claim = parseLine(line);
                    if (claim != null) {
                        claims.add(claim);
                        validLines++;
                    }
                } catch (Exception e) {
                    errorLines++;
                    log.warn("Failed to parse line {}: {} - Error: {}", 
                        totalLines, line, e.getMessage());
                }
            }
        }
        
        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Router log parsing completed in {}ms. Total lines: {}, Valid: {}, Errors: {}", 
            processingTime, totalLines, validLines, errorLines);
        
        return claims;
    }
    
    /**
     * Parse a single log line and extract a dependency claim.
     * 
     * @param line the log line to parse
     * @return dependency claim or null if line cannot be parsed
     */
    private Claim parseLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        Matcher matcher = LOG_PATTERN.matcher(line.trim());
        if (!matcher.matches()) {
            // Try simpler patterns for different log formats
            return parseSimpleFormat(line);
        }
        
        try {
            String timestampStr = matcher.group(1);
            String sourceIp = matcher.group(2);
            String targetIp = matcher.group(3);
            String targetPort = matcher.group(4);
            int status = Integer.parseInt(matcher.group(5));
            int responseTime = Integer.parseInt(matcher.group(6));
            
            // Simple IP-to-service mapping for prototype
            String sourceApp = mapIpToService(sourceIp);
            String targetApp = mapIpToService(targetIp);
            
            // Calculate confidence based on response status and time
            ConfidenceScore confidence = calculateConfidence(status, responseTime);
            
            // Determine dependency type - simplified for prototype
            DependencyType depType = DependencyType.RUNTIME;
            
            Instant timestamp = parseTimestamp(timestampStr);
            
            return Claim.builder()
                .fromApplication(sourceApp)
                .toApplication(targetApp)
                .dependencyType(depType)
                .confidence(confidence)
                .source("router-log")
                .timestamp(timestamp)
                .rawData(line)
                .build()
                .addMetadata("target_port", targetPort)
                .addMetadata("http_status", status)
                .addMetadata("response_time_ms", responseTime);
                
        } catch (Exception e) {
            log.debug("Failed to parse structured log line: {}", line, e);
            return null;
        }
    }
    
    /**
     * Parse simpler log formats like "ServiceA->ServiceB".
     */
    private Claim parseSimpleFormat(String line) {
        String[] parts = line.split("->");
        if (parts.length == 2) {
            String sourceApp = parts[0].trim();
            String targetApp = parts[1].trim();
            
            if (!sourceApp.isEmpty() && !targetApp.isEmpty()) {
                return Claim.builder()
                    .fromApplication(sourceApp)
                    .toApplication(targetApp)
                    .dependencyType(DependencyType.RUNTIME)
                    .confidence(ConfidenceScore.HIGH)
                    .source("router-log-simple")
                    .timestamp(Instant.now())
                    .rawData(line)
                    .build();
            }
        }
        return null;
    }
    
    /**
     * Map IP address to service name.
     * Simple mapping for prototype - in production this would integrate with service discovery.
     */
    private String mapIpToService(String ipAddress) {
        // Basic IP-to-service mapping for demonstration
        switch (ipAddress) {
            case "192.168.1.100": return "web-portal";
            case "192.168.1.200": return "user-service";
            case "192.168.1.150": return "database-service";
            default: return "service-" + ipAddress.replace(".", "-");
        }
    }
    
    /**
     * Calculate confidence score based on HTTP response indicators.
     */
    private ConfidenceScore calculateConfidence(int status, int responseTime) {
        if (status == 200 && responseTime < 1000) {
            return ConfidenceScore.VERY_HIGH;
        } else if (status >= 200 && status < 300) {
            return ConfidenceScore.HIGH;
        } else if (status >= 400 && status < 500) {
            return ConfidenceScore.MEDIUM; // Client errors still indicate dependency
        } else {
            return ConfidenceScore.LOW;
        }
    }
    
    /**
     * Parse timestamp string to Instant.
     */
    private Instant parseTimestamp(String timestampStr) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timestampStr, TIMESTAMP_FORMAT);
            return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse timestamp: {}, using current time", timestampStr);
            return Instant.now();
        }
    }
}
