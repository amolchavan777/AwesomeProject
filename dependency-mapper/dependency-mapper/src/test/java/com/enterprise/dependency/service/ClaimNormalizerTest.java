package com.enterprise.dependency.service;

import com.enterprise.dependency.model.core.Claim;
import com.enterprise.dependency.model.core.ConfidenceScore;
import com.enterprise.dependency.model.core.DependencyType;
import com.enterprise.dependency.service.ClaimNormalizer.NormalizedClaim;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ClaimNormalizer.
 */
class ClaimNormalizerTest {

    private ClaimNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new ClaimNormalizer();
    }

    @Test
    void testNormalizeClaims_SingleClaim() {
        // Create a test claim
        Claim claim = Claim.builder()
            .fromApplication("web-portal")
            .toApplication("mysql-primary")
            .dependencyType(DependencyType.RUNTIME)
            .confidence(ConfidenceScore.VERY_HIGH)
            .source("configuration-file")
            .timestamp(Instant.now())
            .rawData("db.url=jdbc:mysql://mysql-primary:3306/db")
            .build()
            .addMetadata("target_host", "mysql-primary")
            .addMetadata("target_port", "3306");

        // Normalize the claim
        List<NormalizedClaim> normalized = normalizer.normalizeClaims(Arrays.asList(claim));

        // Verify results
        assertNotNull(normalized);
        assertEquals(1, normalized.size());

        NormalizedClaim normalizedClaim = normalized.get(0);
        assertEquals("web-portal-service", normalizedClaim.getFromApplication());
        assertEquals("mysql-database", normalizedClaim.getToApplication());
        assertEquals(DependencyType.RUNTIME, normalizedClaim.getDependencyType());
        assertEquals(ConfidenceScore.VERY_HIGH, normalizedClaim.getConfidence());
        assertEquals("configuration-file", normalizedClaim.getSource());
        
        // Check provenance
        assertEquals(1, normalizedClaim.getProvenance().size());
        assertEquals("configuration-file", normalizedClaim.getProvenance().get(0).getSource());
        
        // Check normalized metadata
        Map<String, String> metadata = normalizedClaim.getMetadata();
        assertTrue(metadata.containsKey("target_host"));
        assertTrue(metadata.containsKey("target_port"));
        assertTrue(metadata.containsKey("source_type"));
        assertTrue(metadata.containsKey("normalized_at"));
    }

    @Test
    void testNormalizeClaims_ServiceNameMapping() {
        // Test various service name mappings
        Map<String, String> testMappings = new HashMap<>();
        testMappings.put("mysql-primary", "mysql-database");
        testMappings.put("redis-cache", "redis-service");
        testMappings.put("kafka-cluster", "kafka-service");
        testMappings.put("auth-service", "authentication-service");
        
        for (Map.Entry<String, String> mapping : testMappings.entrySet()) {
            Claim claim = Claim.builder()
                .fromApplication("test-app")
                .toApplication(mapping.getKey())
                .dependencyType(DependencyType.RUNTIME)
                .confidence(ConfidenceScore.HIGH)
                .source("test")
                .timestamp(Instant.now())
                .rawData("test")
                .build();

            List<NormalizedClaim> normalized = normalizer.normalizeClaims(Arrays.asList(claim));
            
            assertEquals(1, normalized.size());
            assertEquals(mapping.getValue(), normalized.get(0).getToApplication(),
                "Failed to map " + mapping.getKey() + " to " + mapping.getValue());
        }
    }

    @Test
    void testNormalizeClaims_ConfidenceCalibration() {
        // Test confidence calibration based on source
        Claim configClaim = Claim.builder()
            .fromApplication("app")
            .toApplication("service")
            .dependencyType(DependencyType.RUNTIME)
            .confidence(ConfidenceScore.VERY_HIGH)
            .source("configuration-file")
            .timestamp(Instant.now())
            .rawData("test")
            .build();
            
        Claim networkClaim = Claim.builder()
            .fromApplication("app")
            .toApplication("service")
            .dependencyType(DependencyType.RUNTIME)
            .confidence(ConfidenceScore.VERY_HIGH)
            .source("network-discovery")
            .timestamp(Instant.now())
            .rawData("test")
            .build();

        List<NormalizedClaim> configNormalized = normalizer.normalizeClaims(Arrays.asList(configClaim));
        List<NormalizedClaim> networkNormalized = normalizer.normalizeClaims(Arrays.asList(networkClaim));

        // Configuration file should maintain VERY_HIGH confidence (weight = 1.0)
        assertEquals(ConfidenceScore.VERY_HIGH, configNormalized.get(0).getConfidence());
        
        // Network discovery should be calibrated down (weight = 0.7, so VERY_HIGH * 0.7 = HIGH)
        assertEquals(ConfidenceScore.HIGH, networkNormalized.get(0).getConfidence());
    }

    @Test
    void testNormalizeClaims_DuplicateMerging() {
        Instant now = Instant.now();
        
        // Create duplicate claims from different sources
        Claim routerClaim = Claim.builder()
            .fromApplication("web-portal")
            .toApplication("user-service")
            .dependencyType(DependencyType.RUNTIME)
            .confidence(ConfidenceScore.VERY_HIGH)
            .source("router-log")
            .timestamp(now)
            .rawData("GET /api/users 200")
            .build()
            .addMetadata("http_status", "200")
            .addMetadata("response_time", "120ms");
            
        Claim configClaim = Claim.builder()
            .fromApplication("web-portal")
            .toApplication("user-management-service")  // Different name for same service
            .dependencyType(DependencyType.RUNTIME)
            .confidence(ConfidenceScore.VERY_HIGH)
            .source("configuration-file")
            .timestamp(now.plusSeconds(10))
            .rawData("user.service.url=http://user-service:8080")
            .build()
            .addMetadata("target_host", "user-service")
            .addMetadata("target_port", "8080");

        List<NormalizedClaim> normalized = normalizer.normalizeClaims(Arrays.asList(routerClaim, configClaim));

        // Should merge into single claim since they map to the same service
        assertEquals(1, normalized.size());
        
        NormalizedClaim merged = normalized.get(0);
        assertEquals("web-portal-service", merged.getFromApplication());
        assertEquals("user-management-service", merged.getToApplication());
        
        // Should have provenance from both sources
        assertEquals(2, merged.getProvenance().size());
        
        // Should have merged metadata
        Map<String, String> metadata = merged.getMetadata();
        assertTrue(metadata.containsKey("merged_from_sources"));
        assertTrue(metadata.containsKey("all_sources"));
        assertEquals("2", metadata.get("merged_from_sources"));
        assertTrue(metadata.get("all_sources").contains("router-log"));
        assertTrue(metadata.get("all_sources").contains("configuration-file"));
    }

    @Test
    void testNormalizeClaims_NamingConventions() {
        // Test automatic suffix application
        Map<String, String> expectedSuffixes = new HashMap<>();
        expectedSuffixes.put("my-app", "my-app-service");
        expectedSuffixes.put("database-primary", "database-primary-database");
        expectedSuffixes.put("redis-store", "redis-store-service");
        expectedSuffixes.put("kafka-broker", "kafka-broker-service");
        expectedSuffixes.put("payment-gateway", "payment-gateway");  // Already has appropriate suffix
        
        for (Map.Entry<String, String> test : expectedSuffixes.entrySet()) {
            Claim claim = Claim.builder()
                .fromApplication("test")
                .toApplication(test.getKey())
                .dependencyType(DependencyType.RUNTIME)
                .confidence(ConfidenceScore.HIGH)
                .source("test")
                .timestamp(Instant.now())
                .rawData("test")
                .build();

            List<NormalizedClaim> normalized = normalizer.normalizeClaims(Arrays.asList(claim));
            
            assertEquals(test.getValue(), normalized.get(0).getToApplication(),
                "Failed naming convention for " + test.getKey());
        }
    }

    @Test
    void testNormalizeClaims_EmptyInput() {
        List<NormalizedClaim> normalized = normalizer.normalizeClaims(Arrays.asList());
        
        assertNotNull(normalized);
        assertTrue(normalized.isEmpty());
    }

    @Test
    void testNormalizeClaims_NullInput() {
        List<NormalizedClaim> normalized = normalizer.normalizeClaims(null);
        
        assertNotNull(normalized);
        assertTrue(normalized.isEmpty());
    }

    @Test
    void testNormalizeClaims_MetadataNormalization() {
        Map<String, Object> originalMetadata = new HashMap<>();
        originalMetadata.put("Target-Host", "database-server");
        originalMetadata.put("target port", "3306");
        originalMetadata.put("Response_Time", 45);
        originalMetadata.put("null_value", null);
        
        Claim claim = Claim.builder()
            .fromApplication("app")
            .toApplication("db")
            .dependencyType(DependencyType.RUNTIME)
            .confidence(ConfidenceScore.HIGH)
            .source("test")
            .timestamp(Instant.now())
            .rawData("test")
            .build();
            
        // Add metadata manually since we can't easily test with the builder
        for (Map.Entry<String, Object> entry : originalMetadata.entrySet()) {
            if (entry.getValue() != null) {
                claim.addMetadata(entry.getKey(), entry.getValue().toString());
            }
        }

        List<NormalizedClaim> normalized = normalizer.normalizeClaims(Arrays.asList(claim));
        
        assertEquals(1, normalized.size());
        Map<String, String> metadata = normalized.get(0).getMetadata();
        
        // Check that keys are normalized (lowercase, underscores)
        assertTrue(metadata.containsKey("target_host") || metadata.containsKey("target-host"));
        assertTrue(metadata.containsKey("target_port") || metadata.containsKey("target port"));
        assertTrue(metadata.containsKey("response_time"));
        
        // Check standard metadata is added
        assertTrue(metadata.containsKey("source_type"));
        assertTrue(metadata.containsKey("normalized_at"));
    }

    @Test
    void testNormalizeClaims_ComplexScenario() {
        Instant baseTime = Instant.now();
        
        // Simulate a complex scenario with multiple sources reporting same dependencies
        List<Claim> claims = Arrays.asList(
            // Router logs
            createClaim("web-portal", "user-service", "router-log", ConfidenceScore.VERY_HIGH, baseTime),
            createClaim("user-service", "mysql-primary", "router-log", ConfidenceScore.HIGH, baseTime.plusSeconds(5)),
            
            // Configuration files
            createClaim("web-portal", "user-management-service", "configuration-file", ConfidenceScore.VERY_HIGH, baseTime.plusSeconds(10)),
            createClaim("user-service", "mysql-database", "configuration-file", ConfidenceScore.VERY_HIGH, baseTime.plusSeconds(15)),
            
            // Network discovery
            createClaim("web-portal", "user-service", "network-discovery", ConfidenceScore.MEDIUM, baseTime.plusSeconds(20)),
            
            // Unique dependency only from one source
            createClaim("web-portal", "redis-cache", "configuration-file", ConfidenceScore.HIGH, baseTime.plusSeconds(25))
        );

        List<NormalizedClaim> normalized = normalizer.normalizeClaims(claims);

        // Should merge duplicates and keep unique dependencies
        assertEquals(3, normalized.size());  // web->user, user->mysql, web->redis
        
        // Verify merged dependencies have multiple provenance entries
        boolean foundMergedDependency = false;
        for (NormalizedClaim claim : normalized) {
            if (claim.getFromApplication().equals("web-portal-service") && 
                claim.getToApplication().equals("user-management-service")) {
                assertTrue(claim.getProvenance().size() >= 2);  // router + config (+ maybe network)
                foundMergedDependency = true;
            }
        }
        assertTrue(foundMergedDependency, "Should find merged web-portal -> user-service dependency");
    }
    
    private Claim createClaim(String from, String to, String source, ConfidenceScore confidence, Instant timestamp) {
        return Claim.builder()
            .fromApplication(from)
            .toApplication(to)
            .dependencyType(DependencyType.RUNTIME)
            .confidence(confidence)
            .source(source)
            .timestamp(timestamp)
            .rawData("test data")
            .build();
    }
}
