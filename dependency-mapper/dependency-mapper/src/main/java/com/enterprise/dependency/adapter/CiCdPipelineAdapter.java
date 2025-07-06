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
 * Adapter for processing CI/CD pipeline logs to extract deployment dependencies.
 * 
 * Parses Jenkins, GitLab CI, and generic pipeline logs to identify:
 * - Service deployment sequences
 * - Build dependencies
 * - Environment promotion flows
 * - Container orchestration dependencies
 * 
 * @author System
 * @since 1.0
 */
@Component
public class CiCdPipelineAdapter implements DataSourceAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CiCdPipelineAdapter.class);
    
    // Confidence score for CI/CD pipeline data (medium-high confidence)
    private static final double CICD_CONFIDENCE = 0.8;
    
    // Patterns for different CI/CD systems
    private static final Pattern JENKINS_DEPLOY_PATTERN = Pattern.compile(
        "\\[(.*?)\\].*?Deploying\\s+(\\S+)\\s+to\\s+(\\S+).*?depends on\\s+(.*?)(?:\\n|$)", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern GITLAB_CI_PATTERN = Pattern.compile(
        "(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}).*?stage:\\s*(\\S+).*?service:\\s*(\\S+).*?requires:\\s*\\[(.*?)\\]",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern DOCKER_COMPOSE_PATTERN = Pattern.compile(
        "Creating\\s+(\\S+).*?depends_on:\\s*\\[(.*?)\\]",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HELM_DEPLOY_PATTERN = Pattern.compile(
        "Installing\\s+(\\S+).*?dependencies:\\s*\\[(.*?)\\]",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getAdapterName() {
        return "CI/CD Pipeline Adapter";
    }

    @Override
    public List<DependencyClaim> processData(String sourceData) throws AdapterException {
        logger.info("Starting CI/CD pipeline parsing");
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
                    logger.warn("Error parsing CI/CD line: {} - Error: {}", line, e.getMessage());
                }
            }
            
        } catch (IOException e) {
            logger.error("Error reading CI/CD pipeline data", e);
            throw new AdapterException(getAdapterName(), "Failed to parse CI/CD pipeline data", e);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("CI/CD pipeline parsing completed in {}ms. Total lines: {}, Valid: {}, Errors: {}", 
                   duration, totalLines, validEntries, errors);
        
        return claims;
    }

    @Override
    public boolean canProcess(String data) {
        if (data == null || data.trim().isEmpty()) {
            return false;
        }
        
        // Check for CI/CD pipeline indicators
        String lowerData = data.toLowerCase();
        return lowerData.contains("deploying") || 
               lowerData.contains("depends_on") ||
               lowerData.contains("gitlab ci") ||
               lowerData.contains("jenkins build") ||
               lowerData.contains("installing") ||
               lowerData.contains("helm") ||
               lowerData.contains("docker-compose");
    }

    @Override
    public double getDefaultConfidence() {
        return CICD_CONFIDENCE;
    }
    
    /**
     * Parses a single line from CI/CD pipeline logs
     */
    private List<DependencyClaim> parseLine(String line) {
        List<DependencyClaim> claims = new ArrayList<>();
        
        // Try Jenkins pattern
        Matcher jenkinsMatcher = JENKINS_DEPLOY_PATTERN.matcher(line);
        if (jenkinsMatcher.find()) {
            claims.addAll(parseJenkinsEntry(jenkinsMatcher));
            return claims;
        }
        
        // Try GitLab CI pattern
        Matcher gitlabMatcher = GITLAB_CI_PATTERN.matcher(line);
        if (gitlabMatcher.find()) {
            claims.addAll(parseGitLabEntry(gitlabMatcher));
            return claims;
        }
        
        // Try Docker Compose pattern
        Matcher dockerMatcher = DOCKER_COMPOSE_PATTERN.matcher(line);
        if (dockerMatcher.find()) {
            claims.addAll(parseDockerEntry(dockerMatcher));
            return claims;
        }
        
        // Try Helm pattern
        Matcher helmMatcher = HELM_DEPLOY_PATTERN.matcher(line);
        if (helmMatcher.find()) {
            claims.addAll(parseHelmEntry(helmMatcher));
            return claims;
        }
        
        return claims;
    }
    
    private List<DependencyClaim> parseJenkinsEntry(Matcher matcher) {
        List<DependencyClaim> claims = new ArrayList<>();
        
        String timestamp = matcher.group(1);
        String service = normalizeServiceName(matcher.group(2));
        String environment = matcher.group(3);
        String dependencies = matcher.group(4);
        
        // Parse dependencies
        String[] deps = dependencies.split(",");
        for (String dep : deps) {
            dep = normalizeServiceName(dep.trim());
            if (!dep.isEmpty() && !dep.equals(service)) {
                claims.add(createClaim(service, dep));
            }
        }
        
        return claims;
    }
    
    private List<DependencyClaim> parseGitLabEntry(Matcher matcher) {
        List<DependencyClaim> claims = new ArrayList<>();
        
        String timestamp = matcher.group(1);
        String stage = matcher.group(2);
        String service = normalizeServiceName(matcher.group(3));
        String requirements = matcher.group(4);
        
        String[] deps = requirements.split(",");
        for (String dep : deps) {
            dep = normalizeServiceName(dep.trim().replaceAll("[\"']", ""));
            if (!dep.isEmpty() && !dep.equals(service)) {
                claims.add(createClaim(service, dep));
            }
        }
        
        return claims;
    }
    
    private List<DependencyClaim> parseDockerEntry(Matcher matcher) {
        List<DependencyClaim> claims = new ArrayList<>();
        
        String service = normalizeServiceName(matcher.group(1));
        String dependencies = matcher.group(2);
        
        String[] deps = dependencies.split(",");
        for (String dep : deps) {
            dep = normalizeServiceName(dep.trim().replaceAll("[\"']", ""));
            if (!dep.isEmpty() && !dep.equals(service)) {
                claims.add(createClaim(service, dep));
            }
        }
        
        return claims;
    }
    
    private List<DependencyClaim> parseHelmEntry(Matcher matcher) {
        List<DependencyClaim> claims = new ArrayList<>();
        
        String service = normalizeServiceName(matcher.group(1));
        String dependencies = matcher.group(2);
        
        String[] deps = dependencies.split(",");
        for (String dep : deps) {
            dep = normalizeServiceName(dep.trim().replaceAll("[\"']", ""));
            if (!dep.isEmpty() && !dep.equals(service)) {
                claims.add(createClaim(service, dep));
            }
        }
        
        return claims;
    }
    
    private DependencyClaim createClaim(String source, String target) {
        DependencyClaim claim = new DependencyClaim();
        claim.setFromService(new ApplicationService(source));
        claim.setToService(new ApplicationService(target));
        claim.setConfidence(CICD_CONFIDENCE);
        claim.setSource("CICD_PIPELINE");
        claim.setTimestamp(Instant.now());
        return claim;
    }
    
    private String normalizeServiceName(String name) {
        if (name == null) return "";
        
        return name.trim()
                  .toLowerCase()
                  .replaceAll("[^a-z0-9\\-_]", "")
                  .replaceAll("[-_]+", "-")
                  .replaceAll("^-+|-+$", "");
    }
    
    /**
     * Creates sample CI/CD pipeline log data for demonstration
     */
    public static String createSampleCiCdLog() {
        return "[2024-07-06 10:30:00] Jenkins Build #245: Deploying user-service to production depends on auth-service, database-service\n" +
               "[2024-07-06 10:32:15] Jenkins Build #246: Deploying order-service to staging depends on user-service, payment-service, inventory-service\n" +
               "\n" +
               "2024-07-06 10:35:22 GitLab CI Pipeline stage: deploy service: web-portal requires: [\"auth-service\", \"user-service\", \"cdn-service\"]\n" +
               "2024-07-06 10:37:44 GitLab CI Pipeline stage: test service: integration-tests requires: [\"user-service\", \"order-service\", \"payment-service\"]\n" +
               "\n" +
               "Creating notification-service container depends_on: [\"rabbitmq\", \"redis\", \"config-service\"]\n" +
               "Creating api-gateway container depends_on: [\"auth-service\", \"rate-limiter\", \"monitoring-service\"]\n" +
               "\n" +
               "Installing payment-service-chart to k8s dependencies: [\"database-service\", \"vault-service\", \"audit-service\"]\n" +
               "Installing monitoring-stack-chart to k8s dependencies: [\"prometheus\", \"grafana\", \"alertmanager\"]\n";
    }
}
