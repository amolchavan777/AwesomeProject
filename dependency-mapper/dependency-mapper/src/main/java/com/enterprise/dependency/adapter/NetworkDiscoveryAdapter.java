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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter for parsing network discovery data (Nmap-style scan results).
 * 
 * <p>Discovers services running on hosts and infers dependencies based on
 * open ports and service types. This is a passive discovery method that
 * can conflict with active monitoring data from other sources.
 * 
 * <p>Example network scan entry:
 * <pre>{@code
 * HOST: 192.168.1.100 (web-portal)
 * PORT: 80/tcp open http nginx/1.18.0
 * PORT: 443/tcp open https nginx/1.18.0
 * PORT: 3306/tcp filtered mysql
 * }</pre>
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
@Component
public class NetworkDiscoveryAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(NetworkDiscoveryAdapter.class);
    
    // Pattern to match host definitions
    private static final Pattern HOST_PATTERN = Pattern.compile(
        "HOST:\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s*\\(([^)]+)\\)?"
    );
    
    // Pattern to match port/service definitions
    private static final Pattern PORT_PATTERN = Pattern.compile(
        "PORT:\\s+(\\d+)/(tcp|udp)\\s+(open|closed|filtered)\\s+(\\S+)(?:\\s+(.+))?"
    );
    
    // Common service dependencies based on typical architecture patterns
    private static final String MYSQL = "mysql";
    private static final String POSTGRESQL = "postgresql";
    private static final String REDIS = "redis";
    private static final String MONGODB = "mongodb";
    private static final String ORACLE = "oracle";
    
    private static final Map<String, List<String>> SERVICE_DEPENDENCIES = new HashMap<>();
    static {
        SERVICE_DEPENDENCIES.put("http", List.of(MYSQL, POSTGRESQL, REDIS, MONGODB));
        SERVICE_DEPENDENCIES.put("https", List.of(MYSQL, POSTGRESQL, REDIS, MONGODB));
        SERVICE_DEPENDENCIES.put("nginx", List.of(MYSQL, POSTGRESQL, REDIS));
        SERVICE_DEPENDENCIES.put("apache", List.of(MYSQL, POSTGRESQL));
        SERVICE_DEPENDENCIES.put("tomcat", List.of(MYSQL, POSTGRESQL, ORACLE));
        SERVICE_DEPENDENCIES.put("nodejs", List.of(MONGODB, REDIS, POSTGRESQL));
    }
    
    /**
     * Parse a network discovery file and extract dependency claims.
     * 
     * @param scanFilePath absolute path to the network scan file
     * @return list of dependency claims inferred from network discovery
     * @throws IOException if file cannot be read
     * @throws IllegalArgumentException if scanFilePath is null or empty
     */
    public List<Claim> parseNetworkScan(String scanFilePath) throws IOException {
        if (scanFilePath == null || scanFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Scan file path cannot be null or empty");
        }
        
        long startTime = System.currentTimeMillis();
        log.info("Starting network discovery parsing for file: {}", scanFilePath);
        
        ParseResult result = parseNetworkScanFile(scanFilePath);
        
        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Network discovery parsing completed in {}ms. Total lines: {}, Hosts: {}, Services: {}, Dependencies: {}", 
            processingTime, result.totalLines, result.hostsDiscovered, result.servicesFound, result.claims.size());
        
        return result.claims;
    }
    
    /**
     * Internal method to parse the scan file and track statistics.
     */
    private ParseResult parseNetworkScanFile(String scanFilePath) throws IOException {
        ParseResult result = new ParseResult();
        Map<String, HostInfo> discoveredHosts = new HashMap<>();
        String currentHost = null;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(scanFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.totalLines++;
                line = line.trim();
                
                if (shouldSkipLine(line)) {
                    continue;
                }
                
                try {
                    // Process host definition
                    HostInfo hostInfo = parseHostLine(line, discoveredHosts);
                    if (hostInfo != null) {
                        currentHost = hostInfo.ipAddress;
                        result.hostsDiscovered++;
                        continue;
                    }
                    
                    // Process port/service definition
                    if (currentHost != null) {
                        ServiceInfo serviceInfo = parsePortLine(line);
                        if (serviceInfo != null) {
                            HostInfo host = discoveredHosts.get(currentHost);
                            host.addService(serviceInfo);
                            result.servicesFound++;
                        }
                    }
                    
                } catch (Exception e) {
                    log.warn("Failed to parse line {}: {} - Error: {}", 
                        result.totalLines, line, e.getMessage());
                }
            }
        }
        
        // After parsing all hosts and services, infer dependencies
        result.claims.addAll(inferAllDependencies(discoveredHosts));
        
        return result;
    }
    
    private boolean shouldSkipLine(String line) {
        return line.isEmpty() || line.startsWith("#");
    }
    
    private HostInfo parseHostLine(String line, Map<String, HostInfo> discoveredHosts) {
        Matcher hostMatcher = HOST_PATTERN.matcher(line);
        if (hostMatcher.matches()) {
            String ipAddress = hostMatcher.group(1);
            String hostName = hostMatcher.group(2);
            if (hostName == null || hostName.trim().isEmpty()) {
                hostName = "host-" + ipAddress.replace(".", "-");
            }
            
            HostInfo hostInfo = new HostInfo(ipAddress, hostName);
            discoveredHosts.put(ipAddress, hostInfo);
            return hostInfo;
        }
        return null;
    }
    
    private ServiceInfo parsePortLine(String line) {
        Matcher portMatcher = PORT_PATTERN.matcher(line);
        if (portMatcher.matches()) {
            String port = portMatcher.group(1);
            String protocol = portMatcher.group(2);
            String state = portMatcher.group(3);
            String service = portMatcher.group(4);
            String version = portMatcher.group(5);
            
            if ("open".equals(state)) {
                return new ServiceInfo(port, protocol, service, version);
            }
        }
        return null;
    }
    
    /**
     * Infer dependencies between all discovered hosts and services.
     */
    private List<Claim> inferAllDependencies(Map<String, HostInfo> allHosts) {
        List<Claim> claims = new ArrayList<>();
        Instant timestamp = Instant.now();
        
        // For each host and service, look for dependencies
        for (HostInfo host : allHosts.values()) {
            for (ServiceInfo service : host.services) {
                // Look for likely dependencies based on service type
                List<String> likelyDependencies = SERVICE_DEPENDENCIES.getOrDefault(
                    service.service.toLowerCase(), List.of());
                
                for (String dependencyService : likelyDependencies) {
                    // Check if we've discovered this dependency service on any other host
                    for (HostInfo otherHost : allHosts.values()) {
                        if (!otherHost.ipAddress.equals(host.ipAddress) && 
                            otherHost.hasService(dependencyService)) {
                            
                            ConfidenceScore confidence = calculateNetworkConfidence(service, dependencyService);
                            
                            Claim claim = Claim.builder()
                                .fromApplication(host.hostName)
                                .toApplication(otherHost.hostName)
                                .dependencyType(DependencyType.RUNTIME)
                                .confidence(confidence)
                                .source("network-discovery")
                                .timestamp(timestamp)
                                .rawData("HOST: " + host.ipAddress + " SERVICE: " + service.service + 
                                        " -> INFERRED_DEP: " + dependencyService + " ON: " + otherHost.ipAddress)
                                .build()
                                .addMetadata("source_ip", host.ipAddress)
                                .addMetadata("source_port", service.port)
                                .addMetadata("source_service", service.service)
                                .addMetadata("target_ip", otherHost.ipAddress)
                                .addMetadata("target_service", dependencyService)
                                .addMetadata("inference_reason", "common_architecture_pattern");
                            
                            claims.add(claim);
                        }
                    }
                }
            }
        }
        
        return claims;
    }
    
    /**
     * Calculate confidence for network-inferred dependencies.
     */
    private ConfidenceScore calculateNetworkConfidence(ServiceInfo source, String targetService) {
        // Network discovery provides medium confidence since it's based on inference
        // Higher confidence for well-known service patterns
        switch (source.service.toLowerCase()) {
            case "http":
            case "https":
            case "nginx":
            case "apache":
                return targetService.contains("sql") ? ConfidenceScore.HIGH : ConfidenceScore.MEDIUM;
            case "tomcat":
            case "nodejs":
                return ConfidenceScore.MEDIUM;
            default:
                return ConfidenceScore.LOW;
        }
    }
    
    /**
     * Internal class to track parsing results and statistics.
     */
    private static class ParseResult {
        final List<Claim> claims = new ArrayList<>();
        int totalLines = 0;
        int hostsDiscovered = 0;
        int servicesFound = 0;
    }
    
    /**
     * Information about a discovered host.
     */
    private static class HostInfo {
        final String ipAddress;
        final String hostName;
        final List<ServiceInfo> services = new ArrayList<>();
        
        HostInfo(String ipAddress, String hostName) {
            this.ipAddress = ipAddress;
            this.hostName = hostName;
        }
        
        void addService(ServiceInfo service) {
            services.add(service);
        }
        
        boolean hasService(String serviceName) {
            return services.stream()
                .anyMatch(s -> s.service.toLowerCase().contains(serviceName.toLowerCase()));
        }
    }
    
    /**
     * Information about a discovered service.
     */
    private static class ServiceInfo {
        final String port;
        final String service;
        
        ServiceInfo(String port, String protocol, String service, String version) {
            this.port = port;
            this.service = service;
            // Store protocol and version for future use if needed
            // Currently not used but part of the constructor for compatibility
        }
    }
}
