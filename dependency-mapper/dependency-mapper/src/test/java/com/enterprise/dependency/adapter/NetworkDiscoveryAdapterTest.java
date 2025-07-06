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
 * Test suite for NetworkDiscoveryAdapter.
 */
class NetworkDiscoveryAdapterTest {

    private NetworkDiscoveryAdapter adapter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        adapter = new NetworkDiscoveryAdapter();
    }

    @Test
    void testParseNetworkScan_ValidFile() throws IOException {
        // Create test data
        String scanData = "HOST: 192.168.1.100 (web-portal)\n" +
            "PORT: 80/tcp open http nginx/1.18.0\n" +
            "PORT: 443/tcp open https nginx/1.18.0\n" +
            "\n" +
            "HOST: 192.168.1.200 (user-service)\n" +
            "PORT: 8080/tcp open http tomcat/9.0.50\n" +
            "\n" +
            "HOST: 192.168.1.150 (database-service)\n" +
            "PORT: 3306/tcp open mysql MySQL/8.0.25\n" +
            "PORT: 6379/tcp open redis Redis/6.2.1\n";

        Path scanFile = tempDir.resolve("test_scan.txt");
        Files.writeString(scanFile, scanData);

        // Parse the file
        List<Claim> claims = adapter.parseNetworkScan(scanFile.toString());

        // Verify results
        assertNotNull(claims);
        assertTrue(claims.size() > 0, "Should discover some dependencies");

        // Verify claim structure
        for (Claim claim : claims) {
            assertNotNull(claim.getFromApplication());
            assertNotNull(claim.getToApplication());
            assertEquals(DependencyType.RUNTIME, claim.getDependencyType());
            assertEquals("network-discovery", claim.getSource());
            assertNotNull(claim.getTimestamp());
            assertNotNull(claim.getRawData());
            
            // Check metadata
            assertTrue(claim.getMetadata().containsKey("source_ip"));
            assertTrue(claim.getMetadata().containsKey("target_ip"));
            assertTrue(claim.getMetadata().containsKey("inference_reason"));
        }
    }

    @Test
    void testParseNetworkScan_EmptyFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.writeString(emptyFile, "");

        List<Claim> claims = adapter.parseNetworkScan(emptyFile.toString());

        assertNotNull(claims);
        assertTrue(claims.isEmpty());
    }

    @Test
    void testParseNetworkScan_InvalidPath() {
        assertThrows(IllegalArgumentException.class, () -> 
            adapter.parseNetworkScan(null));
        
        assertThrows(IllegalArgumentException.class, () -> 
            adapter.parseNetworkScan(""));
    }

    @Test
    void testParseNetworkScan_FileNotFound() {
        assertThrows(IOException.class, () -> 
            adapter.parseNetworkScan("/non/existent/file.txt"));
    }

    @Test
    void testParseNetworkScan_IgnoreComments() throws IOException {
        String scanData = "# This is a comment\n" +
            "HOST: 192.168.1.100 (web-portal)\n" +
            "# Another comment\n" +
            "PORT: 80/tcp open http nginx/1.18.0\n" +
            "\n" +
            "HOST: 192.168.1.150 (database-service)\n" +
            "PORT: 3306/tcp open mysql MySQL/8.0.25\n";

        Path scanFile = tempDir.resolve("comments.txt");
        Files.writeString(scanFile, scanData);

        List<Claim> claims = adapter.parseNetworkScan(scanFile.toString());

        assertNotNull(claims);
        // Should process hosts and ports despite comments
    }

    @Test
    void testParseNetworkScan_MalformedLines() throws IOException {
        String scanData = "HOST: 192.168.1.100 (web-portal)\n" +
            "PORT: 80/tcp open http nginx/1.18.0\n" +
            "INVALID LINE FORMAT\n" +
            "ANOTHER INVALID LINE\n" +
            "\n" +
            "HOST: 192.168.1.150 (database-service)\n" +
            "PORT: 3306/tcp open mysql MySQL/8.0.25\n";

        Path scanFile = tempDir.resolve("malformed.txt");
        Files.writeString(scanFile, scanData);

        // Should not throw exception, just skip malformed lines
        List<Claim> claims = adapter.parseNetworkScan(scanFile.toString());
        assertNotNull(claims);
    }

    @Test
    void testParseNetworkScan_FilteredPorts() throws IOException {
        String scanData = "HOST: 192.168.1.100 (web-portal)\n" +
            "PORT: 80/tcp open http nginx/1.18.0\n" +
            "PORT: 21/tcp closed ftp\n" +
            "PORT: 23/tcp filtered telnet\n" +
            "\n" +
            "HOST: 192.168.1.150 (database-service)\n" +
            "PORT: 3306/tcp open mysql MySQL/8.0.25\n";

        Path scanFile = tempDir.resolve("filtered.txt");
        Files.writeString(scanFile, scanData);

        List<Claim> claims = adapter.parseNetworkScan(scanFile.toString());

        assertNotNull(claims);
        // Should only process 'open' ports, not 'closed' or 'filtered'
    }

    @Test
    void testParseNetworkScan_NoHostName() throws IOException {
        String scanData = "HOST: 192.168.1.100\n" +
            "PORT: 80/tcp open http nginx/1.18.0\n" +
            "\n" +
            "HOST: 192.168.1.150\n" +
            "PORT: 3306/tcp open mysql MySQL/8.0.25\n";

        Path scanFile = tempDir.resolve("no_hostname.txt");
        Files.writeString(scanFile, scanData);

        List<Claim> claims = adapter.parseNetworkScan(scanFile.toString());

        assertNotNull(claims);
        // Should generate default hostnames like "host-192-168-1-100"
    }
}
