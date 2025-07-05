package com.enterprise.dependency.service;

import com.enterprise.dependency.adapter.AdapterException;
import com.enterprise.dependency.adapter.RouterLogAdapter;
import com.enterprise.dependency.adapter.NetworkDiscoveryAdapter;
import com.enterprise.dependency.adapter.ConfigurationFileAdapter;
import com.enterprise.dependency.adapter.CustomTextAdapter;
import com.enterprise.dependency.model.core.Claim;
import com.example.mapper.model.DependencyClaim;
import com.example.mapper.model.ApplicationService;
import com.example.mapper.repo.DependencyClaimRepository;
import com.example.mapper.repo.ApplicationServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified ingestion pipeline that processes data from multiple sources
 * and normalizes it into the persistence layer.
 * 
 * <p>This service orchestrates the entire data processing pipeline:
 * <ol>
 * <li>Detects data source types</li>
 * <li>Routes data to appropriate adapters</li>
 * <li>Normalizes claims using ClaimNormalizer</li>
 * <li>Converts to persistence models</li>
 * <li>Stores in database</li>
 * </ol>
 * 
 * <p>Usage:
 * <pre>{@code
 * IngestionResult result = ingestionService.ingestFromFile("/path/to/router.log");
 * System.out.println("Processed " + result.getClaimsProcessed() + " claims");
 * }</pre>
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
@Service
@Transactional
public class IngestionService {
    
    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    
    private final RouterLogAdapter routerLogAdapter;
    private final NetworkDiscoveryAdapter networkDiscoveryAdapter;
    private final ConfigurationFileAdapter configurationFileAdapter;
    private final CustomTextAdapter customTextAdapter;
    private final ClaimNormalizer claimNormalizer;
    private final DependencyClaimRepository dependencyClaimRepository;
    private final ApplicationServiceRepository applicationServiceRepository;
    
    // Cache for application services to avoid duplicate lookups
    private final Map<String, ApplicationService> serviceCache = new ConcurrentHashMap<>();
    
    @Autowired
    public IngestionService(
            RouterLogAdapter routerLogAdapter,
            NetworkDiscoveryAdapter networkDiscoveryAdapter,
            ConfigurationFileAdapter configurationFileAdapter,
            CustomTextAdapter customTextAdapter,
            ClaimNormalizer claimNormalizer,
            DependencyClaimRepository dependencyClaimRepository,
            ApplicationServiceRepository applicationServiceRepository) {
        this.routerLogAdapter = routerLogAdapter;
        this.networkDiscoveryAdapter = networkDiscoveryAdapter;
        this.configurationFileAdapter = configurationFileAdapter;
        this.customTextAdapter = customTextAdapter;
        this.claimNormalizer = claimNormalizer;
        this.dependencyClaimRepository = dependencyClaimRepository;
        this.applicationServiceRepository = applicationServiceRepository;
    }
    
    /**
     * Ingests data from a file, automatically detecting the source type.
     * 
     * @param filePath path to the data file
     * @return ingestion result with statistics
     * @throws IngestionException if ingestion fails
     */
    public IngestionResult ingestFromFile(String filePath) throws IngestionException {
        return ingestFromFile(filePath, null);
    }
    
    /**
     * Ingests data from a file with a specific source identifier.
     * 
     * @param filePath path to the data file
     * @param sourceId identifier for this data source (e.g., "prod-router-1")
     * @return ingestion result with statistics
     * @throws IngestionException if ingestion fails
     */
    public IngestionResult ingestFromFile(String filePath, String sourceId) throws IngestionException {
        log.info("Starting ingestion from file: {} (source: {})", filePath, sourceId);
        
        try {
            String content = Files.readString(Paths.get(filePath));
            return ingestFromString(content, detectSourceType(filePath, content), sourceId);
        } catch (IOException e) {
            throw new IngestionException("Failed to read file: " + filePath, e);
        }
    }
    
    /**
     * Ingests data from a string with automatic source type detection.
     * 
     * @param data raw data content
     * @param sourceType detected or specified source type
     * @param sourceId identifier for this data source
     * @return ingestion result with statistics
     * @throws IngestionException if ingestion fails
     */
    public IngestionResult ingestFromString(String data, SourceType sourceType, String sourceId) throws IngestionException {
        long startTime = System.currentTimeMillis();
        IngestionResult.Builder resultBuilder = IngestionResult.builder()
            .sourceType(sourceType)
            .sourceId(sourceId)
            .startTime(Instant.now());
        
        try {
            // Step 1: Parse data using appropriate adapter
            List<Claim> rawClaims = parseWithAdapter(data, sourceType);
            resultBuilder.rawClaimsExtracted(rawClaims.size());
            
            // Step 2: Normalize claims
            List<ClaimNormalizer.NormalizedClaim> normalizedClaims = claimNormalizer.normalizeClaims(rawClaims);
            resultBuilder.claimsAfterNormalization(normalizedClaims.size());
            
            // Step 3: Convert to persistence models and save
            int savedClaims = convertAndSave(normalizedClaims, sourceId);
            resultBuilder.claimsSaved(savedClaims);
            
            long processingTime = System.currentTimeMillis() - startTime;
            resultBuilder.processingTimeMs(processingTime);
            
            IngestionResult result = resultBuilder.build();
            log.info("Ingestion completed: {}", result);
            return result;
            
        } catch (Exception e) {
            throw new IngestionException("Ingestion failed for source: " + sourceId, e);
        }
    }
    
    /**
     * Detects the source type based on file path and content.
     */
    private SourceType detectSourceType(String filePath, String content) {
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString().toLowerCase();
        
        // Check file extension and name patterns
        if (fileName.contains("router") || fileName.contains("access") || fileName.contains(".log")) {
            return SourceType.ROUTER_LOG;
        }
        
        if (fileName.contains("nmap") || fileName.contains("scan") || fileName.contains("network")) {
            return SourceType.NETWORK_DISCOVERY;
        }
        
        if (fileName.contains("application.properties") || fileName.contains("config") || fileName.contains(".properties")) {
            return SourceType.CONFIGURATION_FILE;
        }
        
        // Check content patterns
        if (content.contains("GET ") || content.contains("POST ") || content.contains("HTTP/")) {
            return SourceType.ROUTER_LOG;
        }
        
        if (content.contains("Nmap scan") || content.contains("PORT") || content.contains("open tcp")) {
            return SourceType.NETWORK_DISCOVERY;
        }
        
        if (content.contains("=") && (content.contains(".url") || content.contains(".host") || content.contains(".port"))) {
            return SourceType.CONFIGURATION_FILE;
        }
        
        // Default to router log for unknown formats
        log.warn("Could not detect source type for file: {}, defaulting to ROUTER_LOG", filePath);
        return SourceType.ROUTER_LOG;
    }
    
    /**
     * Parses data using the appropriate adapter based on source type.
     */
    private List<Claim> parseWithAdapter(String data, SourceType sourceType) throws AdapterException {
        try {
            switch (sourceType) {
                case ROUTER_LOG:
                    return parseRouterLogFromString(data);
                case NETWORK_DISCOVERY:
                    return parseNetworkScanFromString(data);
                case CONFIGURATION_FILE:
                    return parseConfigurationFromString(data);
                case CUSTOM_TEXT:
                    return customTextAdapter.parseCustomText(data);
                default:
                    throw new AdapterException("Unknown", "Unsupported source type: " + sourceType);
            }
        } catch (Exception e) {
            throw new AdapterException("Parser", "Failed to parse data", e);
        }
    }
    
    private List<Claim> parseRouterLogFromString(String data) throws AdapterException {
        // For now, we'll create temp files. In production, adapters should support string input
        java.nio.file.Path tempFile = null;
        try {
            tempFile = java.nio.file.Files.createTempFile("router_log", ".log");
            java.nio.file.Files.write(tempFile, data.getBytes());
            return routerLogAdapter.parseLogFile(tempFile.toString());
        } catch (Exception e) {
            throw new AdapterException("RouterLogAdapter", "Failed to parse router log data", e);
        } finally {
            try {
                if (tempFile != null) {
                    java.nio.file.Files.deleteIfExists(tempFile);
                }
            } catch (Exception ignored) {
                // Ignore cleanup errors
            }
        }
    }
    
    private List<Claim> parseNetworkScanFromString(String data) throws AdapterException {
        java.nio.file.Path tempFile = null;
        try {
            tempFile = java.nio.file.Files.createTempFile("network_scan", ".txt");
            java.nio.file.Files.write(tempFile, data.getBytes());
            return networkDiscoveryAdapter.parseNetworkScan(tempFile.toString());
        } catch (Exception e) {
            throw new AdapterException("NetworkDiscoveryAdapter", "Failed to parse network scan data", e);
        } finally {
            try {
                if (tempFile != null) {
                    java.nio.file.Files.deleteIfExists(tempFile);
                }
            } catch (Exception ignored) {
                // Ignore cleanup errors
            }
        }
    }
    
    private List<Claim> parseConfigurationFromString(String data) throws AdapterException {
        java.nio.file.Path tempFile = null;
        try {
            tempFile = java.nio.file.Files.createTempFile("config", ".properties");
            java.nio.file.Files.write(tempFile, data.getBytes());
            return configurationFileAdapter.parseConfigurationFile(tempFile.toString(), "auto-detected");
        } catch (Exception e) {
            throw new AdapterException("ConfigurationFileAdapter", "Failed to parse configuration data", e);
        } finally {
            try {
                if (tempFile != null) {
                    java.nio.file.Files.deleteIfExists(tempFile);
                }
            } catch (Exception ignored) {
                // Ignore cleanup errors
            }
        }
    }
    
    /**
     * Converts normalized claims to persistence models and saves them.
     */
    private int convertAndSave(List<ClaimNormalizer.NormalizedClaim> claims, String sourceId) {
        int savedCount = 0;
        
        for (ClaimNormalizer.NormalizedClaim claim : claims) {
            try {
                DependencyClaim persistenceClaim = convertToPersistenceModel(claim, sourceId);
                dependencyClaimRepository.save(persistenceClaim);
                savedCount++;
            } catch (Exception e) {
                log.error("Failed to save claim: {} -> {}", claim.getFromApplication(), claim.getToApplication(), e);
            }
        }
        
        return savedCount;
    }
    
    /**
     * Converts a normalized claim to a DependencyClaim for persistence.
     */
    private DependencyClaim convertToPersistenceModel(ClaimNormalizer.NormalizedClaim claim, String sourceId) {
        // Get or create application services
        ApplicationService fromService = getOrCreateApplicationService(claim.getFromApplication());
        ApplicationService toService = getOrCreateApplicationService(claim.getToApplication());
        
        // Create the dependency claim
        DependencyClaim dependencyClaim = new DependencyClaim();
        dependencyClaim.setFromService(fromService);
        dependencyClaim.setToService(toService);
        dependencyClaim.setSource(sourceId != null ? sourceId : claim.getSource());
        dependencyClaim.setConfidence(claim.getConfidence().getValue());
        dependencyClaim.setTimestamp(claim.getTimestamp());
        
        return dependencyClaim;
    }
    
    /**
     * Gets an existing application service or creates a new one.
     */
    private ApplicationService getOrCreateApplicationService(String serviceName) {
        return serviceCache.computeIfAbsent(serviceName, name -> {
            ApplicationService existing = applicationServiceRepository.findByName(name);
            if (existing != null) {
                return existing;
            } else {
                ApplicationService newService = new ApplicationService();
                newService.setName(name);
                return applicationServiceRepository.save(newService);
            }
        });
    }
    
    /**
     * Enum for supported source types.
     */
    public enum SourceType {
        ROUTER_LOG,
        NETWORK_DISCOVERY,
        CONFIGURATION_FILE,
        CUSTOM_TEXT
    }
}
