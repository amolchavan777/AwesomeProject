package com.example.mapper;

import com.enterprise.dependency.model.sources.ApiGatewayCall;
import com.enterprise.dependency.model.sources.CodebaseDependency;
import com.enterprise.dependency.model.sources.RouterLogEntry;
import com.example.mapper.repo.ApplicationRepository;
import com.example.mapper.repo.ClaimRepository;
import com.example.mapper.service.ClaimProcessingEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ClaimProcessingEngineTest {

    @Autowired
    private ClaimProcessingEngine engine;
    @Autowired
    private ApplicationRepository appRepo;
    @Autowired
    private ClaimRepository claimRepo;

    @Test
    void processRouterLog() {
        RouterLogEntry entry = new RouterLogEntry();
        entry.setSourceIp("10.0.0.1");
        entry.setTargetIp("10.0.0.2");
        entry.setTimestamp(Instant.now());
        var claim = engine.processRouterLog(entry);
        assertNotNull(claim.getId());
        assertEquals(0.9, claim.getConfidence(), 0.0001);
    }

    @Test
    void processCodebaseDependency() {
        CodebaseDependency dep = new CodebaseDependency();
        dep.setGroupId("group");
        dep.setArtifactId("artifact");
        dep.setVersion("1.0");
        var claim = engine.processCodebaseDependency(dep);
        assertNotNull(claim.getId());
        assertEquals(0.95, claim.getConfidence(), 0.0001);
    }

    @Test
    void processApiGatewayCall() {
        ApiGatewayCall call = new ApiGatewayCall();
        call.setSourceService("svcA");
        call.setTargetService("svcB");
        call.setTimestamp(Instant.now());
        var claim = engine.processApiGatewayCall(call);
        assertNotNull(claim.getId());
        assertEquals(0.85, claim.getConfidence(), 0.0001);
    }
}
