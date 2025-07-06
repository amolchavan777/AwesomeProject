package com.example.mapper;

import com.enterprise.dependency.adapter.ApiGatewayAdapter;
import com.enterprise.dependency.adapter.CiCdPipelineAdapter;
import com.example.mapper.model.DependencyClaim;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Test for the new data source adapters to verify they process mock data correctly
 */
public class DataSourceAdapterTest {

    @Test
    public void testCiCdPipelineAdapter() throws Exception {
        CiCdPipelineAdapter adapter = new CiCdPipelineAdapter();
        String sampleData = CiCdPipelineAdapter.createSampleCiCdLog();
        
        assertTrue(adapter.canProcess(sampleData), "CI/CD adapter should be able to process sample data");
        
        List<DependencyClaim> claims = adapter.processData(sampleData);
        assertNotNull(claims, "Claims should not be null");
        assertFalse(claims.isEmpty(), "Claims should not be empty");
        
        // Verify some expected dependencies are found
        boolean foundUserServiceDependency = claims.stream()
            .anyMatch(claim -> 
                claim.getFromService().getName().equals("user-service") &&
                claim.getToService().getName().equals("auth-service"));
        
        assertTrue(foundUserServiceDependency, "Should find user-service depends on auth-service");
        
        // Verify all claims have proper confidence and source
        for (DependencyClaim claim : claims) {
            assertEquals("CICD_PIPELINE", claim.getSource());
            assertEquals(0.8, claim.getConfidence(), 0.01);
            assertNotNull(claim.getFromService());
            assertNotNull(claim.getToService());
            assertNotNull(claim.getTimestamp());
        }
        
        System.out.println("CI/CD Adapter found " + claims.size() + " dependency claims");
    }
    
    @Test
    public void testApiGatewayAdapter() throws Exception {
        ApiGatewayAdapter adapter = new ApiGatewayAdapter();
        String sampleData = ApiGatewayAdapter.createSampleApiGatewayLog();
        
        assertTrue(adapter.canProcess(sampleData), "API Gateway adapter should be able to process sample data");
        
        List<DependencyClaim> claims = adapter.processData(sampleData);
        assertNotNull(claims, "Claims should not be null");
        assertFalse(claims.isEmpty(), "Claims should not be empty");
        
        // Verify some expected dependencies are found
        boolean foundWebFrontendDependency = claims.stream()
            .anyMatch(claim -> 
                claim.getFromService().getName().equals("web-frontend") &&
                claim.getToService().getName().equals("user-service"));
        
        assertTrue(foundWebFrontendDependency, "Should find web-frontend depends on user-service");
        
        // Verify all claims have proper confidence and source
        for (DependencyClaim claim : claims) {
            assertEquals("API_GATEWAY", claim.getSource());
            assertEquals(0.95, claim.getConfidence(), 0.01);
            assertNotNull(claim.getFromService());
            assertNotNull(claim.getToService());
            assertNotNull(claim.getTimestamp());
        }
        
        System.out.println("API Gateway Adapter found " + claims.size() + " dependency claims");
    }
    
    @Test
    public void testAdapterNames() {
        CiCdPipelineAdapter cicdAdapter = new CiCdPipelineAdapter();
        ApiGatewayAdapter gatewayAdapter = new ApiGatewayAdapter();
        
        assertEquals("CI/CD Pipeline Adapter", cicdAdapter.getAdapterName());
        assertEquals("API Gateway Adapter", gatewayAdapter.getAdapterName());
    }
    
    @Test
    public void testAdapterConfidenceLevels() {
        CiCdPipelineAdapter cicdAdapter = new CiCdPipelineAdapter();
        ApiGatewayAdapter gatewayAdapter = new ApiGatewayAdapter();
        
        assertEquals(0.8, cicdAdapter.getDefaultConfidence(), 0.01);
        assertEquals(0.95, gatewayAdapter.getDefaultConfidence(), 0.01);
    }
}
