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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter for parsing application configuration files to discover dependencies.
 * 
 * <p>Parses various configuration formats (properties, YAML-like, JSON-like) 
 * to extract database connections, service URLs, and other dependency declarations.
 * 
 * <p>Example configuration entries:
 * <pre>{@code
 * # Database connections
 * db.primary.url=jdbc:mysql://database-service:3306/main
 * db.cache.redis.host=redis-service
 * db.cache.redis.port=6379
 * 
 * # Service endpoints
 * service.user.url=http://user-service:8080/api
 * service.payment.endpoint=https://payment-service:8443/payments
 * 
 * # Message queues
 * messaging.rabbitmq.host=mq-service
 * messaging.kafka.brokers=kafka-service:9092
 * }</pre>
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
@Component
public class ConfigurationFileAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(ConfigurationFileAdapter.class);
    
    // Constants for metadata keys and values
    private static final String SOURCE_NAME = "configuration-file";
    private static final String TARGET_HOST_KEY = "target_host";
    private static final String TARGET_PORT_KEY = "target_port";
    private static final String CONFIG_LINE_KEY = "config_line";
    private static final String DEPENDENCY_EVIDENCE_KEY = "dependency_evidence";
    private static final String UNKNOWN_VALUE = "unknown";
    
    // Patterns for detecting various dependency types in configuration
    private static final Pattern DATABASE_URL_PATTERN = Pattern.compile(
        "(?:^|\\W)(?:url|connection|jdbc)\\s*[=:]\\s*(?:jdbc:)?([^:]+)://([^:/]+)(?::(\\d+))?(/\\w+)?.*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SERVICE_URL_PATTERN = Pattern.compile(
        "(?:^|\\W)(?:url|endpoint)\\s*[=:]\\s*https?://([^:/]+)(?::(\\d+))?(?:/.*)?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HOST_PATTERN = Pattern.compile(
        "(?:^|\\W)(?:host|server)\\s*[=:]\\s*([^:\\s]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern KAFKA_PATTERN = Pattern.compile(
        "(?:^|\\W)(?:brokers?|bootstrap[._-]servers?)\\s*[=:]\\s*([^:\\s,]+)(?::(\\d+))?",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Parse a configuration file and extract dependency claims.
     * 
     * @param configFilePath absolute path to the configuration file
     * @param applicationName name of the application using this configuration
     * @return list of dependency claims found in the configuration
     * @throws IOException if file cannot be read
     * @throws IllegalArgumentException if parameters are null or empty
     */
    public List<Claim> parseConfigurationFile(String configFilePath, String applicationName) throws IOException {
        if (configFilePath == null || configFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Config file path cannot be null or empty");
        }
        if (applicationName == null || applicationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name cannot be null or empty");
        }
        
        long startTime = System.currentTimeMillis();
        log.info("Starting configuration parsing for application '{}' from file: {}", 
            applicationName, configFilePath);
        
        ParseResult result = parseConfigurationFileInternal(configFilePath, applicationName);
        
        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Configuration parsing completed in {}ms. Total lines: {}, Dependencies: {}", 
            processingTime, result.totalLines, result.claims.size());
        
        return result.claims;
    }
    
    /**
     * Internal method to parse the configuration file.
     */
    private ParseResult parseConfigurationFileInternal(String configFilePath, String applicationName) throws IOException {
        ParseResult result = new ParseResult();
        Instant timestamp = Instant.now();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                result.totalLines++;
                line = line.trim();
                
                if (shouldSkipLine(line)) {
                    continue;
                }
                
                try {
                    // Try to parse different types of dependencies
                    List<Claim> lineClaims = new ArrayList<>();
                    
                    // Database connections
                    lineClaims.addAll(parseDatabaseDependency(line, applicationName, timestamp, lineNumber));
                    
                    // Service URLs
                    lineClaims.addAll(parseServiceDependency(line, applicationName, timestamp, lineNumber));
                    
                    // Host/port combinations
                    lineClaims.addAll(parseHostPortDependency(line, applicationName, timestamp, lineNumber));
                    
                    // Kafka brokers
                    lineClaims.addAll(parseKafkaDependency(line, applicationName, timestamp, lineNumber));
                    
                    result.claims.addAll(lineClaims);
                    
                } catch (Exception e) {
                    log.warn("Failed to parse line {}: {} - Error: {}", 
                        lineNumber, line, e.getMessage());
                }
            }
        }
        
        return result;
    }
    
    private boolean shouldSkipLine(String line) {
        return line.isEmpty() || 
               line.startsWith("#") || 
               line.startsWith("//") || 
               line.startsWith("/*") ||
               line.startsWith("*");
    }
    
    private List<Claim> parseDatabaseDependency(String line, String fromApp, Instant timestamp, int lineNumber) {
        List<Claim> claims = new ArrayList<>();
        Matcher matcher = DATABASE_URL_PATTERN.matcher(line);
        
        if (matcher.find()) {
            String dbType = matcher.group(1); // mysql, postgresql, etc.
            String host = matcher.group(2);
            String port = matcher.group(3);
            String database = matcher.group(4);
            
            String targetService = host.contains("-") ? host : host + "-database";
            
            Claim claim = Claim.builder()
                .fromApplication(fromApp)
                .toApplication(targetService)
                .dependencyType(DependencyType.RUNTIME)
                .confidence(ConfidenceScore.VERY_HIGH)
                .source(SOURCE_NAME)
                .timestamp(timestamp)
                .rawData(line.trim())
                .build()
                .addMetadata(TARGET_HOST_KEY, host)
                .addMetadata(TARGET_PORT_KEY, port != null ? port : getDefaultPort(dbType))
                .addMetadata("database_type", dbType)
                .addMetadata("database_name", database != null ? database.substring(1) : UNKNOWN_VALUE)
                .addMetadata(CONFIG_LINE_KEY, String.valueOf(lineNumber))
                .addMetadata(DEPENDENCY_EVIDENCE_KEY, "explicit_database_url");
            
            claims.add(claim);
        }
        
        return claims;
    }
    
    private List<Claim> parseServiceDependency(String line, String fromApp, Instant timestamp, int lineNumber) {
        List<Claim> claims = new ArrayList<>();
        Matcher matcher = SERVICE_URL_PATTERN.matcher(line);
        
        if (matcher.find()) {
            String host = matcher.group(1);
            String port = matcher.group(2);
            
            String targetService = host.contains("-") ? host : host + "-service";
            
            Claim claim = Claim.builder()
                .fromApplication(fromApp)
                .toApplication(targetService)
                .dependencyType(DependencyType.RUNTIME)
                .confidence(ConfidenceScore.VERY_HIGH)
                .source(SOURCE_NAME)
                .timestamp(timestamp)
                .rawData(line.trim())
                .build()
                .addMetadata(TARGET_HOST_KEY, host)
                .addMetadata(TARGET_PORT_KEY, port != null ? port : "80")
                .addMetadata("protocol", line.contains("https") ? "https" : "http")
                .addMetadata(CONFIG_LINE_KEY, String.valueOf(lineNumber))
                .addMetadata(DEPENDENCY_EVIDENCE_KEY, "explicit_service_url");
            
            claims.add(claim);
        }
        
        return claims;
    }
    
    private List<Claim> parseHostPortDependency(String line, String fromApp, Instant timestamp, int lineNumber) {
        List<Claim> claims = new ArrayList<>();
        Matcher matcher = HOST_PATTERN.matcher(line);
        
        if (matcher.find()) {
            String host = matcher.group(1);
            
            // Skip localhost and IP addresses for now
            if (host.equals("localhost") || host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                return claims;
            }
            
            String targetService = host.contains("-") ? host : host + "-service";
            
            Claim claim = Claim.builder()
                .fromApplication(fromApp)
                .toApplication(targetService)
                .dependencyType(DependencyType.RUNTIME)
                .confidence(ConfidenceScore.HIGH)
                .source(SOURCE_NAME)
                .timestamp(timestamp)
                .rawData(line.trim())
                .build()
                .addMetadata(TARGET_HOST_KEY, host)
                .addMetadata(TARGET_PORT_KEY, UNKNOWN_VALUE)
                .addMetadata(CONFIG_LINE_KEY, String.valueOf(lineNumber))
                .addMetadata(DEPENDENCY_EVIDENCE_KEY, "host_port_reference");
            
            claims.add(claim);
        }
        
        return claims;
    }
    
    private List<Claim> parseKafkaDependency(String line, String fromApp, Instant timestamp, int lineNumber) {
        List<Claim> claims = new ArrayList<>();
        Matcher matcher = KAFKA_PATTERN.matcher(line);
        
        if (matcher.find()) {
            String host = matcher.group(1);
            String port = matcher.group(2);
            
            String targetService = host.contains("-") ? host : host + "-kafka";
            
            Claim claim = Claim.builder()
                .fromApplication(fromApp)
                .toApplication(targetService)
                .dependencyType(DependencyType.RUNTIME)
                .confidence(ConfidenceScore.VERY_HIGH)
                .source(SOURCE_NAME)
                .timestamp(timestamp)
                .rawData(line.trim())
                .build()
                .addMetadata(TARGET_HOST_KEY, host)
                .addMetadata(TARGET_PORT_KEY, port != null ? port : "9092")
                .addMetadata("service_type", "kafka")
                .addMetadata(CONFIG_LINE_KEY, String.valueOf(lineNumber))
                .addMetadata(DEPENDENCY_EVIDENCE_KEY, "kafka_brokers_config");
            
            claims.add(claim);
        }
        
        return claims;
    }
    
    private String getDefaultPort(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql": return "3306";
            case "postgresql": return "5432";
            case "oracle": return "1521";
            case "mongodb": return "27017";
            case "redis": return "6379";
            default: return UNKNOWN_VALUE;
        }
    }
    
    /**
     * Internal class to track parsing results.
     */
    private static class ParseResult {
        final List<Claim> claims = new ArrayList<>();
        int totalLines = 0;
    }
}
