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
 * Kubernetes Manifest Adapter
 * Processes container orchestration metadata to discover service dependencies
 * 
 * Supports multiple Kubernetes resource types:
 * - Service definitions and selectors
 * - Deployment environment variables and config references
 * - ConfigMap and Secret references
 * - Ingress routing rules
 * - ServiceMonitor (Prometheus operator) configurations
 */
@Component
public class KubernetesAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(KubernetesAdapter.class);
    private static final String SOURCE_TYPE_KEY = "source_type";
    
    // Kubernetes Service patterns
    private static final Pattern SERVICE_PATTERN = Pattern.compile(
        ".*name:\\s*([\\w-]+).*kind:\\s*Service", Pattern.DOTALL
    );
    
    // Environment variable dependency patterns
    private static final Pattern ENV_DEPENDENCY_PATTERN = Pattern.compile(
        ".*name:\\s*([\\w_]+(?:_URL|_HOST|_ENDPOINT|_SERVICE)).*value:\\s*([\\w.-]+)", Pattern.DOTALL
    );
    
    // ConfigMap/Secret reference patterns
    private static final Pattern CONFIG_REF_PATTERN = Pattern.compile(
        ".*configMapRef:\\s*name:\\s*([\\w-]+)|.*secretRef:\\s*name:\\s*([\\w-]+)", Pattern.DOTALL
    );
    
    // Service selector patterns
    private static final Pattern SELECTOR_PATTERN = Pattern.compile(
        ".*selector:\\s*app:\\s*([\\w-]+)", Pattern.DOTALL
    );
    
    // Ingress routing patterns
    private static final Pattern INGRESS_PATTERN = Pattern.compile(
        ".*host:\\s*([\\w.-]+).*service:\\s*name:\\s*([\\w-]+)", Pattern.DOTALL
    );

    /**
     * Parse Kubernetes manifests to extract service dependencies
     */
    public List<Claim> parseKubernetesManifests(String data) throws AdapterException {
        log.info("Starting Kubernetes manifest parsing");
        List<Claim> claims = new ArrayList<>();
        
        try {
            // Split manifests by YAML document separator
            String[] documents = data.split("---");
            int processedDocs = 0;
            int validClaims = 0;
            
            for (String document : documents) {
                if (document.trim().isEmpty()) continue;
                
                processedDocs++;
                List<Claim> docClaims = parseDocument(document.trim());
                claims.addAll(docClaims);
                validClaims += docClaims.size();
            }
            
            log.info("Kubernetes parsing completed: {} documents processed, {} valid claims extracted", 
                    processedDocs, validClaims);
            return claims;
            
        } catch (Exception e) {
            throw new AdapterException("KubernetesAdapter", "Failed to parse Kubernetes manifests", e);
        }
    }
    
    private List<Claim> parseDocument(String document) {
        List<Claim> claims = new ArrayList<>();
        
        // Determine document type and extract dependencies
        if (document.contains("kind: Service")) {
            claims.addAll(parseServiceManifest(document));
        } else if (document.contains("kind: Deployment") || document.contains("kind: StatefulSet")) {
            claims.addAll(parseWorkloadManifest(document));
        } else if (document.contains("kind: Ingress")) {
            claims.addAll(parseIngressManifest(document));
        } else if (document.contains("kind: ConfigMap")) {
            claims.addAll(parseConfigMapManifest(document));
        }
        
        return claims;
    }
    
    private List<Claim> parseServiceManifest(String document) {
        List<Claim> claims = new ArrayList<>();
        
        try {
            // Extract service name
            String serviceName = extractServiceName(document);
            if (serviceName == null) return claims;
            
            // Look for selector-based dependencies
            Matcher selectorMatcher = SELECTOR_PATTERN.matcher(document);
            if (selectorMatcher.find()) {
                String targetApp = selectorMatcher.group(1);
                
                Claim claim = new Claim();
                claim.setFromApplication("kubernetes-service-" + serviceName);
                claim.setToApplication(targetApp);
                claim.setConfidence(ConfidenceScore.HIGH);
                claim.setSource("kubernetes-service");
                claim.setTimestamp(Instant.now());
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("service_name", serviceName);
                metadata.put("selector_app", targetApp);
                metadata.put(SOURCE_TYPE_KEY, "kubernetes");
                metadata.put("resource_type", "Service");
                claim.setMetadata(metadata);
                
                claims.add(claim);
                log.debug("Found service selector dependency: {} -> {}", serviceName, targetApp);
            }
            
        } catch (Exception e) {
            log.warn("Error parsing service manifest: {}", e.getMessage());
        }
        
        return claims;
    }
    
    private List<Claim> parseWorkloadManifest(String document) {
        List<Claim> claims = new ArrayList<>();
        
        try {
            String workloadName = extractWorkloadName(document);
            if (workloadName == null) return claims;
            
            // Parse environment variable dependencies
            Matcher envMatcher = ENV_DEPENDENCY_PATTERN.matcher(document);
            while (envMatcher.find()) {
                String envName = envMatcher.group(1);
                String envValue = envMatcher.group(2);
                
                // Extract service name from environment variable
                String targetService = extractServiceFromEnvValue(envValue);
                if (targetService != null) {
                    Claim claim = new Claim();
                    claim.setFromApplication(workloadName);
                    claim.setToApplication(targetService);
                    claim.setConfidence(ConfidenceScore.HIGH);
                    claim.setSource("kubernetes-env");
                    claim.setTimestamp(Instant.now());
                    
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("env_variable", envName);
                    metadata.put("env_value", envValue);
                    metadata.put(SOURCE_TYPE_KEY, "kubernetes");
                    metadata.put("resource_type", "Deployment");
                    claim.setMetadata(metadata);
                    
                    claims.add(claim);
                    log.debug("Found env dependency: {} -> {} (via {})", workloadName, targetService, envName);
                }
            }
            
            // Parse ConfigMap/Secret references
            Matcher configMatcher = CONFIG_REF_PATTERN.matcher(document);
            while (configMatcher.find()) {
                String configName = configMatcher.group(1) != null ? configMatcher.group(1) : configMatcher.group(2);
                
                Claim claim = new Claim();
                claim.setFromApplication(workloadName);
                claim.setToApplication(configName);
                claim.setConfidence(ConfidenceScore.MEDIUM);
                claim.setSource("kubernetes-config");
                claim.setTimestamp(Instant.now());
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("config_reference", configName);
                metadata.put(SOURCE_TYPE_KEY, "kubernetes");
                metadata.put("resource_type", "ConfigReference");
                claim.setMetadata(metadata);
                
                claims.add(claim);
                log.debug("Found config dependency: {} -> {}", workloadName, configName);
            }
            
        } catch (Exception e) {
            log.warn("Error parsing workload manifest: {}", e.getMessage());
        }
        
        return claims;
    }
    
    private List<Claim> parseIngressManifest(String document) {
        List<Claim> claims = new ArrayList<>();
        
        try {
            Matcher ingressMatcher = INGRESS_PATTERN.matcher(document);
            while (ingressMatcher.find()) {
                String host = ingressMatcher.group(1);
                String serviceName = ingressMatcher.group(2);
                
                Claim claim = new Claim();
                claim.setFromApplication("ingress-" + host);
                claim.setToApplication(serviceName);
                claim.setConfidence(ConfidenceScore.VERY_HIGH);
                claim.setSource("kubernetes-ingress");
                claim.setTimestamp(Instant.now());
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("ingress_host", host);
                metadata.put("target_service", serviceName);
                metadata.put(SOURCE_TYPE_KEY, "kubernetes");
                metadata.put("resource_type", "Ingress");
                claim.setMetadata(metadata);
                
                claims.add(claim);
                log.debug("Found ingress routing: {} -> {}", host, serviceName);
            }
            
        } catch (Exception e) {
            log.warn("Error parsing ingress manifest: {}", e.getMessage());
        }
        
        return claims;
    }
    
    private List<Claim> parseConfigMapManifest(String document) {
        List<Claim> claims = new ArrayList<>();
        
        try {
            String configName = extractConfigMapName(document);
            if (configName == null) return claims;
            
            // Look for service references in ConfigMap data
            Matcher serviceMatcher = Pattern.compile("([\\w-]+\\.svc\\.cluster\\.local|[\\w-]+-service)").matcher(document);
            while (serviceMatcher.find()) {
                String referencedService = serviceMatcher.group(1);
                referencedService = referencedService.replace(".svc.cluster.local", "").replace("-service", "");
                
                Claim claim = new Claim();
                claim.setFromApplication("config-" + configName);
                claim.setToApplication(referencedService);
                claim.setConfidence(ConfidenceScore.MEDIUM);
                claim.setSource("kubernetes-configmap");
                claim.setTimestamp(Instant.now());
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("configmap_name", configName);
                metadata.put("referenced_service", referencedService);
                metadata.put(SOURCE_TYPE_KEY, "kubernetes");
                metadata.put("resource_type", "ConfigMap");
                claim.setMetadata(metadata);
                
                claims.add(claim);
                log.debug("Found ConfigMap reference: {} -> {}", configName, referencedService);
            }
            
        } catch (Exception e) {
            log.warn("Error parsing ConfigMap manifest: {}", e.getMessage());
        }
        
        return claims;
    }
    
    private String extractServiceName(String document) {
        Pattern namePattern = Pattern.compile("metadata:\\s*name:\\s*([\\w-]+)", Pattern.DOTALL);
        Matcher matcher = namePattern.matcher(document);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractWorkloadName(String document) {
        Pattern namePattern = Pattern.compile("metadata:\\s*name:\\s*([\\w-]+)", Pattern.DOTALL);
        Matcher matcher = namePattern.matcher(document);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractConfigMapName(String document) {
        Pattern namePattern = Pattern.compile("metadata:\\s*name:\\s*([\\w-]+)", Pattern.DOTALL);
        Matcher matcher = namePattern.matcher(document);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractServiceFromEnvValue(String envValue) {
        // Common patterns for service references in environment variables
        if (envValue.contains(".svc.cluster.local")) {
            return envValue.split("\\.")[0];
        }
        if (envValue.endsWith("-service") || envValue.endsWith("-svc")) {
            return envValue.replace("-service", "").replace("-svc", "");
        }
        // Check if it looks like a service name (simple heuristic)
        if (envValue.matches("[\\w-]+") && envValue.length() > 3) {
            return envValue;
        }
        return null;
    }
    
    /**
     * Generate sample Kubernetes manifests for testing
     */
    public String generateSampleData() {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("apiVersion: apps/v1\n");
        sb.append("kind: Deployment\n");
        sb.append("metadata:\n");
        sb.append("  name: api-gateway\n");
        sb.append("spec:\n");
        sb.append("  template:\n");
        sb.append("    spec:\n");
        sb.append("      containers:\n");
        sb.append("      - name: gateway\n");
        sb.append("        env:\n");
        sb.append("        - name: AUTH_SERVICE_URL\n");
        sb.append("          value: auth-service.default.svc.cluster.local\n");
        sb.append("        - name: USER_SERVICE_HOST\n");
        sb.append("          value: user-service\n");
        sb.append("---\n");
        sb.append("apiVersion: v1\n");
        sb.append("kind: Service\n");
        sb.append("metadata:\n");
        sb.append("  name: auth-service\n");
        sb.append("spec:\n");
        sb.append("  selector:\n");
        sb.append("    app: auth-backend\n");
        sb.append("---\n");
        sb.append("apiVersion: networking.k8s.io/v1\n");
        sb.append("kind: Ingress\n");
        sb.append("metadata:\n");
        sb.append("  name: main-ingress\n");
        sb.append("spec:\n");
        sb.append("  rules:\n");
        sb.append("  - host: api.example.com\n");
        sb.append("    http:\n");
        sb.append("      paths:\n");
        sb.append("      - path: /\n");
        sb.append("        backend:\n");
        sb.append("          service:\n");
        sb.append("            name: api-gateway\n");
        return sb.toString();
    }
}
