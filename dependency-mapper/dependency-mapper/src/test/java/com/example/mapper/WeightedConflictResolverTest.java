package com.example.mapper;

import com.enterprise.dependency.model.core.Application;
import com.enterprise.dependency.model.core.Claim;
import com.example.mapper.repo.ApplicationRepository;
import com.example.mapper.repo.ClaimRepository;
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
        "overrides.ServiceA->ServiceC=manual",
        "recency.weight=1",
        "frequency.weight=1"
})
public class WeightedConflictResolverTest {

    @Autowired
    private ApplicationRepository appRepo;

    @Autowired
    private ClaimRepository claimRepo;

    @Autowired
    private WeightedConflictResolver resolver;

    @Test
    void testConflictResolution() {
        Application a = new Application();
        a.setName("ServiceA");
        a = appRepo.save(a);
        Application b = new Application();
        b.setName("ServiceB");
        b = appRepo.save(b);
        Application c = new Application();
        c.setName("ServiceC");
        c = appRepo.save(c);

        Claim oldClaim = new Claim();
        oldClaim.setFromApplication(a);
        oldClaim.setToApplication(c);
        oldClaim.setSource("auto");
        oldClaim.setConfidence(0.9);
        oldClaim.setTimestamp(Instant.now().minusSeconds(3600));
        claimRepo.save(oldClaim);

        Claim newClaim = new Claim();
        newClaim.setFromApplication(a);
        newClaim.setToApplication(c);
        newClaim.setSource("manual");
        newClaim.setConfidence(0.6);
        newClaim.setTimestamp(Instant.now());
        claimRepo.save(newClaim);

        Claim freqClaim1 = new Claim();
        freqClaim1.setFromApplication(a);
        freqClaim1.setToApplication(b);
        freqClaim1.setSource("auto");
        freqClaim1.setConfidence(0.7);
        freqClaim1.setTimestamp(Instant.now());
        claimRepo.save(freqClaim1);

        Claim freqClaim2 = new Claim();
        freqClaim2.setFromApplication(a);
        freqClaim2.setToApplication(b);
        freqClaim2.setSource("auto");
        freqClaim2.setConfidence(0.7);
        freqClaim2.setTimestamp(Instant.now());
        claimRepo.save(freqClaim2);

        var resolved = resolver.resolve();
        assertEquals(newClaim.getId(), resolved.get("ServiceA").get("ServiceC").getId(), "manual override should win");
        assertEquals(freqClaim1.getId(), resolved.get("ServiceA").get("ServiceB").getId(), "frequency should keep auto claim");
    }

    @Test
    void testRecencyBias() {
        Application x = new Application();
        x.setName("ServiceX");
        x = appRepo.save(x);
        Application y = new Application();
        y.setName("ServiceY");
        y = appRepo.save(y);

        Claim oldC = new Claim();
        oldC.setFromApplication(x);
        oldC.setToApplication(y);
        oldC.setSource("auto");
        oldC.setConfidence(0.5);
        oldC.setTimestamp(Instant.now().minusSeconds(7200));
        claimRepo.save(oldC);

        Claim newC = new Claim();
        newC.setFromApplication(x);
        newC.setToApplication(y);
        newC.setSource("auto");
        newC.setConfidence(0.5);
        newC.setTimestamp(Instant.now());
        claimRepo.save(newC);

        var resolved = resolver.resolve();
        assertEquals(newC.getId(), resolved.get("ServiceX").get("ServiceY").getId(), "newer claim should win");
    }
}
