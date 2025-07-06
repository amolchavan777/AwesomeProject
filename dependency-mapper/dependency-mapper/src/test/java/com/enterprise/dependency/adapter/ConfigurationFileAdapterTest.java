package com.enterprise.dependency.adapter;

import com.enterprise.dependency.model.core.Claim;
import com.enterprise.dependency.model.core.DependencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ConfigurationFileAdapter.
 */
class ConfigurationFileAdapterTest {

    private ConfigurationFileAdapter adapter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        adapter = new ConfigurationFileAdapter();
    }

    @Test
    void testParseConfigurationFile_DatabaseConnections() throws IOException {
        // Create test configuration with various database types
        String configData = "# Database configuration\n" +
            "db.primary.url=jdbc:mysql://database-service:3306/maindb\n" +
            "db.cache.redis.host=redis-service\n" +
            "db.analytics.url=jdbc:postgresql://analytics-db:5432/warehouse\n" +
            "# Another comment\n" +
            "reporting.oracle.connection=jdbc:oracle://oracle-service:1521/reports\n";

        Path configFile = tempDir.resolve("database.properties");
        Files.writeString(configFile, configData);

        // Parse the file
        List<Claim> claims = adapter.parseConfigurationFile(configFile.toString(), "web-application");

        // Verify results
        assertNotNull(claims);
        assertTrue(claims.size() >= 3, "Should discover database dependencies");

        // Verify claim structure
        for (Claim claim : claims) {
            assertEquals("web-application", claim.getFromApplication());
            assertEquals(DependencyType.RUNTIME, claim.getDependencyType());
            assertEquals("configuration-file", claim.getSource());
            assertNotNull(claim.getTimestamp());
            assertNotNull(claim.getRawData());
            
            // Check metadata
            assertTrue(claim.getMetadata().containsKey("target_host"));
            assertTrue(claim.getMetadata().containsKey("target_port"));
            assertTrue(claim.getMetadata().containsKey("config_line"));
            assertTrue(claim.getMetadata().containsKey("dependency_evidence"));
        }
    }

    @Test
    void testParseConfigurationFile_ServiceEndpoints() throws IOException {
        String configData = "# Service endpoints\n" +
            "user.service.url=http://user-service:8080/api\n" +
            "payment.endpoint=https://payment-service:8443/payments\n" +
            "notification.service.endpoint=http://notification-service:9000/notify\n";

        Path configFile = tempDir.resolve("services.properties");
        Files.writeString(configFile, configData);

        List<Claim> claims = adapter.parseConfigurationFile(configFile.toString(), "api-gateway");

        assertNotNull(claims);
        assertTrue(claims.size() >= 3, "Should discover service dependencies");

        // Check specific service dependencies
        boolean foundUserService = claims.stream()
            .anyMatch(c -> c.getToApplication().equals("user-service"));
        boolean foundPaymentService = claims.stream()
            .anyMatch(c -> c.getToApplication().equals("payment-service"));
        
        assertTrue(foundUserService, "Should find user-service dependency");
        assertTrue(foundPaymentService, "Should find payment-service dependency");
    }

    @Test
    void testParseConfigurationFile_HostReferences() throws IOException {
        String configData = "# Host configurations\n" +
            "redis.host=redis-cluster\n" +
            "elasticsearch.server=search-service\n" +
            "mongodb.host=mongo-primary\n";

        Path configFile = tempDir.resolve("hosts.conf");
        Files.writeString(configFile, configData);

        List<Claim> claims = adapter.parseConfigurationFile(configFile.toString(), "data-processor");

        assertNotNull(claims);
        assertTrue(claims.size() >= 3, "Should discover host dependencies");
    }

    @Test
    void testParseConfigurationFile_KafkaBrokers() throws IOException {
        String configData = "# Kafka configuration\n" +
            "kafka.brokers=kafka-service:9092\n" +
            "messaging.bootstrap.servers=message-broker:9093\n";

        Path configFile = tempDir.resolve("kafka.properties");
        Files.writeString(configFile, configData);

        List<Claim> claims = adapter.parseConfigurationFile(configFile.toString(), "event-processor");

        assertNotNull(claims);
        assertTrue(claims.size() >= 2, "Should discover Kafka dependencies");

        // Verify Kafka-specific metadata
        boolean foundKafkaService = claims.stream()
            .anyMatch(c -> c.getToApplication().equals("kafka-service") &&
                          c.getMetadata().get("service_type").equals("kafka"));
        
        assertTrue(foundKafkaService, "Should find Kafka service dependency");
    }

    @Test
    void testParseConfigurationFile_EmptyFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.properties");
        Files.writeString(emptyFile, "");

        List<Claim> claims = adapter.parseConfigurationFile(emptyFile.toString(), "test-app");

        assertNotNull(claims);
        assertTrue(claims.isEmpty(), "Empty file should produce no dependencies");
    }

    @Test
    void testParseConfigurationFile_OnlyComments() throws IOException {
        String configData = "# This is a comment\n" +
            "# Another comment\n" +
            "// Java style comment\n" +
            "/* Multi-line comment */\n" +
            "* Another comment style\n";

        Path configFile = tempDir.resolve("comments.properties");
        Files.writeString(configFile, configData);

        List<Claim> claims = adapter.parseConfigurationFile(configFile.toString(), "test-app");

        assertNotNull(claims);
        assertTrue(claims.isEmpty(), "Comments-only file should produce no dependencies");
    }

    @Test
    void testParseConfigurationFile_InvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> 
            adapter.parseConfigurationFile(null, "app"));
        
        assertThrows(IllegalArgumentException.class, () -> 
            adapter.parseConfigurationFile("", "app"));
            
        assertThrows(IllegalArgumentException.class, () -> 
            adapter.parseConfigurationFile("file.txt", null));
            
        assertThrows(IllegalArgumentException.class, () -> 
            adapter.parseConfigurationFile("file.txt", ""));
    }

    @Test
    void testParseConfigurationFile_FileNotFound() {
        assertThrows(IOException.class, () -> 
            adapter.parseConfigurationFile("/non/existent/file.properties", "app"));
    }

    @Test
    void testParseConfigurationFile_MixedConfiguration() throws IOException {
        // Test a realistic configuration file with mixed dependency types
        String configData = "# Application Configuration\n" +
            "app.name=web-portal\n" +
            "app.version=1.0.0\n" +
            "\n" +
            "# Database connections\n" +
            "spring.datasource.url=jdbc:mysql://mysql-primary:3306/portal\n" +
            "spring.redis.host=redis-cache\n" +
            "\n" +
            "# External services\n" +
            "service.authentication.url=http://auth-service:8080/auth\n" +
            "service.payment.endpoint=https://payment-gateway:8443/api\n" +
            "\n" +
            "# Message queues\n" +
            "spring.kafka.bootstrap-servers=kafka-cluster:9092\n" +
            "\n" +
            "# Other configuration\n" +
            "logging.level.root=INFO\n" +
            "server.port=8080\n";

        Path configFile = tempDir.resolve("application.properties");
        Files.writeString(configFile, configData);

        List<Claim> claims = adapter.parseConfigurationFile(configFile.toString(), "web-portal");

        assertNotNull(claims);
        assertTrue(claims.size() >= 5, "Should discover multiple types of dependencies");

        // Verify different types of dependencies are found
        boolean foundDatabase = claims.stream()
            .anyMatch(c -> c.getToApplication().equals("mysql-primary"));
        boolean foundRedis = claims.stream()
            .anyMatch(c -> c.getToApplication().equals("redis-cache"));
        boolean foundAuth = claims.stream()
            .anyMatch(c -> c.getToApplication().equals("auth-service"));
        boolean foundKafka = claims.stream()
            .anyMatch(c -> c.getToApplication().contains("kafka"));

        assertTrue(foundDatabase, "Should find database dependency");
        assertTrue(foundRedis, "Should find Redis dependency");
        assertTrue(foundAuth, "Should find authentication service dependency");
        assertTrue(foundKafka, "Should find Kafka dependency");
    }
}
