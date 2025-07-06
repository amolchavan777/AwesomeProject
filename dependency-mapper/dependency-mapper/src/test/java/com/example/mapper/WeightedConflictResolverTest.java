package com.example.mapper;

import com.example.mapper.model.ApplicationService;
import com.example.mapper.model.DependencyClaim;
import com.example.mapper.repo.ApplicationServiceRepository;
import com.example.mapper.repo.DependencyClaimRepository;
import com.example.mapper.service.WeightedConflictResolver;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "source.priorities.manual=5",
        "source.priorities.auto=1",
        "overrides.ServiceA->ServiceC=manual"
})
public class WeightedConflictResolverTest {

    @Autowired
    private ApplicationServiceRepository serviceRepo;

    @Autowired
    private DependencyClaimRepository claimRepo;

    @Autowired
    private WeightedConflictResolver resolver;

    @Test
    void testConflictResolution() {
        ApplicationService a = new ApplicationService();
        a.setName("ServiceA");
        a = serviceRepo.save(a);
        ApplicationService b = new ApplicationService();
        b.setName("ServiceB");
        b = serviceRepo.save(b);
        ApplicationService c = new ApplicationService();
        c.setName("ServiceC");
        c = serviceRepo.save(c);

        DependencyClaim oldClaim = new DependencyClaim();
        oldClaim.setFromService(a);
        oldClaim.setToService(c);
        oldClaim.setSource("auto");
        oldClaim.setConfidence(0.9);
        oldClaim.setTimestamp(Instant.now().minusSeconds(3600));
        claimRepo.save(oldClaim);

        DependencyClaim newClaim = new DependencyClaim();
        newClaim.setFromService(a);
        newClaim.setToService(c);
        newClaim.setSource("manual");
        newClaim.setConfidence(0.6);
        newClaim.setTimestamp(Instant.now());
        claimRepo.save(newClaim);

        DependencyClaim freqClaim1 = new DependencyClaim();
        freqClaim1.setFromService(a);
        freqClaim1.setToService(b);
        freqClaim1.setSource("auto");
        freqClaim1.setConfidence(0.7);
        freqClaim1.setTimestamp(Instant.now());
        claimRepo.save(freqClaim1);

        DependencyClaim freqClaim2 = new DependencyClaim();
        freqClaim2.setFromService(a);
        freqClaim2.setToService(b);
        freqClaim2.setSource("auto");
        freqClaim2.setConfidence(0.7);
        freqClaim2.setTimestamp(Instant.now());
        claimRepo.save(freqClaim2);

        var resolved = resolver.resolve();
        // Check that the override source wins for ServiceA->ServiceC
        assertEquals("manual", resolved.get("ServiceA").get("ServiceC").getSource(), "manual override should win");
        // Check that frequency keeps one of the auto claims for ServiceA->ServiceB
        assertEquals("auto", resolved.get("ServiceA").get("ServiceB").getSource(), "frequency should keep auto claim");
    }
}
