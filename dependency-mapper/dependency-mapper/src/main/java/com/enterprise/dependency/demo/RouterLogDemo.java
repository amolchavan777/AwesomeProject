package com.enterprise.dependency.demo;

import com.enterprise.dependency.adapter.RouterLogAdapter;
import com.enterprise.dependency.model.core.Claim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Demo runner to showcase the RouterLogAdapter functionality.
 */
@Component
public class RouterLogDemo implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(RouterLogDemo.class);
    
    private final RouterLogAdapter adapter;
    
    public RouterLogDemo(RouterLogAdapter adapter) {
        this.adapter = adapter;
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("=== Router Log Adapter Demo ===");
        
        // Parse the sample log file
        String logFile = "/tmp/sample_router.log";
        List<Claim> claims = adapter.parseLogFile(logFile);
        
        log.info("Parsed {} dependency claims from log file", claims.size());
        
        for (Claim claim : claims) {
            log.info("Dependency: {} -> {} (confidence: {}, source: {})", 
                claim.getFromApplication(), 
                claim.getToApplication(), 
                claim.getConfidence(), 
                claim.getSource());
            
            claim.getMetadata().forEach((key, value) -> 
                log.info("  Metadata: {} = {}", key, value));
        }
        
        log.info("=== Demo Complete ===");
    }
}
