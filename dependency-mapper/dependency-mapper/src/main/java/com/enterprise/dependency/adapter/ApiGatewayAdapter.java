package com.enterprise.dependency.adapter;

import com.example.mapper.model.ApplicationService;
import com.example.mapper.model.DependencyClaim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter for processing API Gateway logs to extract service communication dependencies.
 * 
 * Parses logs from various API gateways (Kong, AWS API Gateway, NGINX, Istio Gateway) to identify:
 * - API call patterns between services
 * - Load balancer routing configurations
 * - Service discovery patterns
 * - Request routing and dependency flows
 * 
 * @author System
 * @since 1.0
 */
@Component
public class ApiGatewayAdapter implements DataSourceAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayAdapter.class);
    
    // High confidence for API gateway data as it represents actual traffic
    private static final double API_GATEWAY_CONFIDENCE = 0.95;
    
    // Patterns for different API gateway systems
    private static final Pattern KONG_PATTERN = Pattern.compile(
        "(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}).*?\"(\\w+)\\s+([^\"]+)\".*?upstream_service:\\s*\"([^\"]+)\".*?(\\d{3})",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern AWS_API_GATEWAY_PATTERN = Pattern.compile(
        "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z).*?requestId:\\s*([^\\s,]+).*?resource:\\s*\"([^\"]+)\".*?backend:\\s*\"([^\"]+)\".*?status:\\s*(\\d{3})",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern NGINX_PATTERN = Pattern.compile(
        "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}).*?\"(\\w+)\\s+([^\"]+)\".*?(\\d{3}).*?upstream:\\s*\"([^\"]+)\"",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ISTIO_PATTERN = Pattern.compile(
        "\\[(.*?)\\].*?method=(\\w+).*?path=\"([^\"]+)\".*?source_app=\"([^\"]+)\".*?destination_service_name=\"([^\"]+)\".*?response_code=(\\d{3})",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern for service mesh routing configs
    private static final Pattern SERVICE_MESH_CONFIG_PATTERN = Pattern.compile(
        "route:\\s*([^\\s]+)\\s*->\\s*([^\\s]+).*?weight:\\s*(\\d+)",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getAdapterName() {
        return "API Gateway Adapter";
    }

    @Override
    public List<DependencyClaim> processData(String sourceData) throws AdapterException {
        logger.info("Starting API Gateway log parsing");
        long startTime = System.currentTimeMillis();
        
        List<DependencyClaim> claims = new ArrayList<>();
        int totalLines = 0;
        int validEntries = 0;
        int errors = 0;
        
        try (BufferedReader reader = new BufferedReader(new StringReader(sourceData))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                totalLines++;
                
                try {
                    List<DependencyClaim> lineClaims = parseLine(line);
                    claims.addAll(lineClaims);
                    if (!lineClaims.isEmpty()) {
                        validEntries++;
                    }
                } catch (Exception e) {
                    errors++;
                    logger.warn("Error parsing API Gateway line: {} - Error: {}", line, e.getMessage());
                }
            }
            
        } catch (IOException e) {
            logger.error("Error reading API Gateway data", e);
            throw new AdapterException(getAdapterName(), "Failed to parse API Gateway data", e);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("API Gateway parsing completed in {}ms. Total lines: {}, Valid: {}, Errors: {}", 
                   duration, totalLines, validEntries, errors);
        
        return claims;
    }

    @Override
    public boolean canProcess(String data) {
        if (data == null || data.trim().isEmpty()) {
            return false;
        }
        
        // Check for API gateway indicators
        String lowerData = data.toLowerCase();
        return lowerData.contains("kong") || 
               lowerData.contains("api gateway") ||
               lowerData.contains("upstream") ||
               lowerData.contains("nginx") ||
               lowerData.contains("istio") ||
               lowerData.contains("envoy") ||
               lowerData.contains("destination_service") ||
               lowerData.contains("backend:") ||
               lowerData.contains("route:");
    }

    @Override
    public double getDefaultConfidence() {
        return API_GATEWAY_CONFIDENCE;
    }
    
    /**
     * Parses a single line from API Gateway logs
     */
    private List<DependencyClaim> parseLine(String line) {
        List<DependencyClaim> claims = new ArrayList<>();
        
        // Try Kong pattern
        Matcher kongMatcher = KONG_PATTERN.matcher(line);
        if (kongMatcher.find()) {
            claims.addAll(parseKongEntry(kongMatcher));
            return claims;
        }
        
        // Try AWS API Gateway pattern
        Matcher awsMatcher = AWS_API_GATEWAY_PATTERN.matcher(line);
        if (awsMatcher.find()) {
            claims.addAll(parseAwsApiGatewayEntry(awsMatcher));
            return claims;
        }
        
        // Try NGINX pattern
        Matcher nginxMatcher = NGINX_PATTERN.matcher(line);
        if (nginxMatcher.find()) {
            claims.addAll(parseNginxEntry(nginxMatcher));
            return claims;
        }
        
        // Try Istio pattern
        Matcher istioMatcher = ISTIO_PATTERN.matcher(line);
        if (istioMatcher.find()) {
            claims.addAll(parseIstioEntry(istioMatcher));
            return claims;
        }
        
        // Try service mesh config pattern
        Matcher meshMatcher = SERVICE_MESH_CONFIG_PATTERN.matcher(line);
        if (meshMatcher.find()) {
            claims.addAll(parseServiceMeshConfig(meshMatcher));
            return claims;
        }
        
        return claims;
    }
    
    private List<DependencyClaim> parseKongEntry(Matcher matcher) {
        List<DependencyClaim> claims = new ArrayList<>();
        
        String timestamp = matcher.group(1);
        String method = matcher.group(2);
        String path = matcher.group(3);
        String upstreamService = normalizeServiceName(matcher.group(4));
        String statusCode = matcher.group(5);
        
        // Extract caller service from path if possible
        String callerService = extractServiceFromPath(path);
        
        if (callerService != null && !callerService.equals(upstreamService)) {
            claims.add(createClaim(callerService, upstreamService));
        }
        
        return claims;
    }
    
    private List<DependencyClaim> parseAwsApiGatewayEntry(Matcher matcher) {
        List<DependencyClaim> claims = new ArrayList<>();
        
        String timestamp = matcher.group(1);
        String requestId = matcher.group(2);
        String resource = matcher.group(3);
        String backend = normalizeServiceName(matcher.group(4));
        String status = matcher.group(5);
        
        String callerService = extractServiceFromPath(resource);
        
        if (callerService != null && !callerService.equals(backend)) {
            claims.add(createClaim(callerService, backend));
        }
        
        return claims;
    }
    
    private List<DependencyClaim> parseNginxEntry(Matcher matcher) {
        List<DependencyClaim> claims = new ArrayList<>();
        
        String upstream = normalizeServiceName(matcher.group(5));
        String path = matcher.group(3);
        
        String callerService = extractServiceFromPath(path);
        
        if (callerService != null && !callerService.equals(upstream)) {
            claims.add(createClaim(callerService, upstream));
        }
        
        return claims;
    }
    
    private List<DependencyClaim> parseIstioEntry(Matcher matcher) {
        List<DependencyClaim> claims = new ArrayList<>();
        
        String sourceApp = normalizeServiceName(matcher.group(4));
        String destinationService = normalizeServiceName(matcher.group(5));
        
        if (sourceApp != null && destinationService != null && !sourceApp.equals(destinationService)) {
            claims.add(createClaim(sourceApp, destinationService));
        }
        
        return claims;
    }
    
    private List<DependencyClaim> parseServiceMeshConfig(Matcher matcher) {
        List<DependencyClaim> claims = new ArrayList<>();
        
        String sourceService = normalizeServiceName(matcher.group(1));
        String targetService = normalizeServiceName(matcher.group(2));
        
        if (sourceService != null && targetService != null && !sourceService.equals(targetService)) {
            claims.add(createClaim(sourceService, targetService));
        }
        
        return claims;
    }
    
    private DependencyClaim createClaim(String source, String target) {
        DependencyClaim claim = new DependencyClaim();
        claim.setFromService(new ApplicationService(source));
        claim.setToService(new ApplicationService(target));
        claim.setConfidence(API_GATEWAY_CONFIDENCE);
        claim.setSource("API_GATEWAY");
        claim.setTimestamp(Instant.now());
        return claim;
    }
    
    private String normalizeServiceName(String name) {
        if (name == null) return null;
        
        // Remove protocol and port info
        name = name.replaceAll("https?://", "").replaceAll(":\\d+", "");
        
        // Extract service name from various formats
        if (name.contains("/")) {
            String[] parts = name.split("/");
            name = parts[0]; // Take the first part as service name
        }
        
        return name.trim()
                  .toLowerCase()
                  .replaceAll("[^a-z0-9\\-_]", "")
                  .replaceAll("[-_]+", "-")
                  .replaceAll("^-+|-+$", "");
    }
    
    /**
     * Extracts service name from API path
     */
    private String extractServiceFromPath(String path) {
        if (path == null || path.isEmpty()) return null;
        
        // Common patterns: /api/v1/users -> users, /service/user-service -> user-service
        String[] pathParts = path.split("/");
        
        for (String part : pathParts) {
            part = part.trim();
            if (!part.isEmpty() && 
                !part.equals("api") && 
                !part.matches("v\\d+") && 
                !part.equals("service") &&
                part.length() > 2) {
                return normalizeServiceName(part + "-service");
            }
        }
        
        return null;
    }
    
    /**
     * Creates sample API Gateway log data for demonstration
     */
    public static String createSampleApiGatewayLog() {
        return "2024-07-06 10:30:15 Kong: \"GET /api/v1/users/profile\" upstream_service:\"user-service\" 200 latency:45ms\n" +
               "2024-07-06 10:30:16 Kong: \"POST /api/v1/orders\" upstream_service:\"order-service\" 201 latency:120ms\n" +
               "2024-07-06 10:30:17 Kong: \"GET /api/v1/payment/status\" upstream_service:\"payment-service\" 200 latency:32ms\n" +
               "\n" +
               "2024-07-06T10:31:00.123Z AWS API Gateway requestId: abc123-def456 resource: \"/users/{id}\" backend: \"user-service.internal\" status: 200\n" +
               "2024-07-06T10:31:01.456Z AWS API Gateway requestId: def456-ghi789 resource: \"/orders/create\" backend: \"order-service.internal\" status: 201\n" +
               "2024-07-06T10:31:02.789Z AWS API Gateway requestId: ghi789-jkl012 resource: \"/payments/process\" backend: \"payment-service.internal\" status: 200\n" +
               "\n" +
               "192.168.1.10 \"GET /api/users\" 200 upstream: \"user-service:8080\"\n" +
               "192.168.1.11 \"POST /api/orders/new\" 201 upstream: \"order-service:8080\"\n" +
               "192.168.1.12 \"GET /api/inventory/check\" 200 upstream: \"inventory-service:8080\"\n" +
               "\n" +
               "[2024-07-06T10:32:00] Istio method=GET path=\"/users/123\" source_app=\"web-frontend\" destination_service_name=\"user-service\" response_code=200\n" +
               "[2024-07-06T10:32:01] Istio method=POST path=\"/orders\" source_app=\"web-frontend\" destination_service_name=\"order-service\" response_code=201\n" +
               "[2024-07-06T10:32:02] Istio method=GET path=\"/payment/validate\" source_app=\"order-service\" destination_service_name=\"payment-service\" response_code=200\n" +
               "\n" +
               "route: web-frontend -> user-service weight: 80\n" +
               "route: web-frontend -> order-service weight: 70\n" +
               "route: order-service -> payment-service weight: 90\n" +
               "route: order-service -> inventory-service weight: 85\n";
    }
}
