package com.enterprise.dependency.adapter;

import com.enterprise.dependency.model.core.Claim;
import com.enterprise.dependency.model.core.ConfidenceScore;
import com.enterprise.dependency.model.core.DependencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for RouterLogAdapter functionality.
 * 
 * @author GitHub Copilot
 * @since 1.0.0
 */
class RouterLogAdapterTest {
    
    private RouterLogAdapter adapter;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        adapter = new RouterLogAdapter();
    }
    
    @Test
    void testParseSimpleLogFormat() throws IOException {
        // Given
        Path logFile = tempDir.resolve("simple.log");
        try (FileWriter writer = new FileWriter(logFile.toFile())) {
            writer.write("ServiceA->ServiceB\n");
            writer.write("web-portal->user-service\n");
            writer.write("user-service->database\n");
        }
        
        // When
        List<Claim> claims = adapter.parseLogFile(logFile.toString());
        
        // Then
        assertEquals(3, claims.size());
        
        Claim firstClaim = claims.get(0);
        assertEquals("ServiceA", firstClaim.getFromApplication());
        assertEquals("ServiceB", firstClaim.getToApplication());
        assertEquals(DependencyType.RUNTIME, firstClaim.getDependencyType());
        assertEquals(ConfidenceScore.HIGH, firstClaim.getConfidence());
        assertEquals("router-log-simple", firstClaim.getSource());
    }
    
    @Test
    void testParseStructuredLogFormat() throws IOException {
        // Given
        Path logFile = tempDir.resolve("structured.log");
        try (FileWriter writer = new FileWriter(logFile.toFile())) {
            writer.write("2024-07-04 10:30:45 [INFO] 192.168.1.100 -> 192.168.1.200:8080 GET /api/users 200 125ms\n");
            writer.write("2024-07-04 10:30:46 [INFO] 192.168.1.200 -> 192.168.1.150:3306 POST /data 201 250ms\n");
        }
        
        // When
        List<Claim> claims = adapter.parseLogFile(logFile.toString());
        
        // Then
        assertEquals(2, claims.size());
        
        Claim firstClaim = claims.get(0);
        assertEquals("web-portal", firstClaim.getFromApplication());
        assertEquals("user-service", firstClaim.getToApplication());
        assertEquals(DependencyType.RUNTIME, firstClaim.getDependencyType());
        assertEquals(ConfidenceScore.VERY_HIGH, firstClaim.getConfidence());
        assertEquals("router-log", firstClaim.getSource());
        assertEquals("8080", firstClaim.getMetadata("target_port"));
        assertEquals(200, firstClaim.getMetadata("http_status"));
        assertEquals(125, firstClaim.getMetadata("response_time_ms"));
    }
    
    @Test
    void testParseLogWithErrors() throws IOException {
        // Given
        Path logFile = tempDir.resolve("errors.log");
        try (FileWriter writer = new FileWriter(logFile.toFile())) {
            writer.write("ServiceA->ServiceB\n");
            writer.write("InvalidLine\n");
            writer.write("->NoSource\n");
            writer.write("NoTarget->\n");
            writer.write("ServiceC->ServiceD\n");
        }
        
        // When
        List<Claim> claims = adapter.parseLogFile(logFile.toString());
        
        // Then
        assertEquals(2, claims.size()); // Only valid lines should be parsed
        assertEquals("ServiceA", claims.get(0).getFromApplication());
        assertEquals("ServiceC", claims.get(1).getFromApplication());
    }
    
    @Test
    void testParseEmptyFile() throws IOException {
        // Given
        Path logFile = tempDir.resolve("empty.log");
        try (FileWriter writer = new FileWriter(logFile.toFile())) {
            // Empty file
        }
        
        // When
        List<Claim> claims = adapter.parseLogFile(logFile.toString());
        
        // Then
        assertTrue(claims.isEmpty());
    }
    
    @Test
    void testParseNonExistentFile() {
        // When & Then
        assertThrows(IOException.class, () -> {
            adapter.parseLogFile("/non/existent/file.log");
        });
    }
    
    @Test
    void testParseNullFilePath() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            adapter.parseLogFile(null);
        });
    }
    
    @Test
    void testParseEmptyFilePath() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            adapter.parseLogFile("");
        });
    }
    
    @Test
    void testConfidenceCalculation() throws IOException {
        // Given
        Path logFile = tempDir.resolve("confidence.log");
        try (FileWriter writer = new FileWriter(logFile.toFile())) {
            writer.write("2024-07-04 10:30:45 test 192.168.1.100 test 192.168.1.200:8080 test 200 50ms\n");   // VERY_HIGH
            writer.write("2024-07-04 10:30:45 test 192.168.1.100 test 192.168.1.200:8080 test 201 500ms\n");  // HIGH  
            writer.write("2024-07-04 10:30:45 test 192.168.1.100 test 192.168.1.200:8080 test 404 100ms\n");  // MEDIUM
            writer.write("2024-07-04 10:30:45 test 192.168.1.100 test 192.168.1.200:8080 test 500 100ms\n");  // LOW
        }
        
        // When
        List<Claim> claims = adapter.parseLogFile(logFile.toString());
        
        // Then
        assertEquals(4, claims.size());
        assertEquals(ConfidenceScore.VERY_HIGH, claims.get(0).getConfidence());
        assertEquals(ConfidenceScore.HIGH, claims.get(1).getConfidence());
        assertEquals(ConfidenceScore.MEDIUM, claims.get(2).getConfidence());
        assertEquals(ConfidenceScore.LOW, claims.get(3).getConfidence());
    }
}
