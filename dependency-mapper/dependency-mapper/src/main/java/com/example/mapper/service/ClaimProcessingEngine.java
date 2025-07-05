package com.example.mapper.service;

import com.enterprise.dependency.model.core.Application;
import com.enterprise.dependency.model.core.Claim;
import com.enterprise.dependency.model.sources.ApiGatewayCall;
import com.enterprise.dependency.model.sources.CodebaseDependency;
import com.enterprise.dependency.model.sources.RouterLogEntry;
import com.example.mapper.repo.ApplicationRepository;
import com.example.mapper.repo.ClaimRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes raw data models from various sources into {@link Claim} entities.
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * RouterLogEntry entry = new RouterLogEntry();
 * entry.setSourceIp("10.0.0.1");
 * entry.setTargetIp("10.0.0.2");
 * entry.setTimestamp(Instant.now());
 * Claim claim = engine.processRouterLog(entry);
 * }
 * </pre>
 */
@Service
public class ClaimProcessingEngine {

    private static final Logger log = LoggerFactory.getLogger(ClaimProcessingEngine.class);

    private final ApplicationRepository appRepo;
    private final ClaimRepository claimRepo;

    @Value("${confidence.routerLog:0.9}")
    private double routerLogConfidence;

    @Value("${confidence.codebase:0.95}")
    private double codebaseConfidence;

    @Value("${confidence.apiGateway:0.85}")
    private double apiGatewayConfidence;

    public ClaimProcessingEngine(ApplicationRepository appRepo, ClaimRepository claimRepo) {
        this.appRepo = appRepo;
        this.claimRepo = claimRepo;
    }

    /**
     * Convert a router log entry into a {@link Claim} and persist it.
     *
     * @param entry router log data
     * @return persisted claim
     */
    @Transactional
    public Claim processRouterLog(RouterLogEntry entry) {
        long start = System.nanoTime();
        validateRouterLog(entry);
        Claim claim = buildClaim(entry.getSourceIp(), entry.getTargetIp(), "routerLog", routerLogConfidence, entry.getTimestamp());
        claimRepo.save(claim);
        log.info("Processed router log in {} ms", (System.nanoTime() - start) / 1_000_000);
        return claim;
    }

    /**
     * Convert a codebase dependency into a {@link Claim} and persist it.
     *
     * @param dep code dependency
     * @return persisted claim
     */
    @Transactional
    public Claim processCodebaseDependency(CodebaseDependency dep) {
        long start = System.nanoTime();
        validateCodebase(dep);
        String from = dep.getGroupId();
        String to = dep.getArtifactId();
        Claim claim = buildClaim(from, to, "codebase", codebaseConfidence, Instant.now());
        claimRepo.save(claim);
        log.info("Processed codebase dep in {} ms", (System.nanoTime() - start) / 1_000_000);
        return claim;
    }

    /**
     * Convert an API gateway call into a {@link Claim} and persist it.
     *
     * @param call gateway call
     * @return persisted claim
     */
    @Transactional
    public Claim processApiGatewayCall(ApiGatewayCall call) {
        long start = System.nanoTime();
        validateGateway(call);
        Claim claim = buildClaim(call.getSourceService(), call.getTargetService(), "apiGateway", apiGatewayConfidence, call.getTimestamp());
        claimRepo.save(claim);
        log.info("Processed gateway call in {} ms", (System.nanoTime() - start) / 1_000_000);
        return claim;
    }

    private Claim buildClaim(String fromName, String toName, String source, double confidence, Instant ts) {
        Application from = findOrCreate(fromName);
        Application to = findOrCreate(toName);
        Claim claim = new Claim();
        claim.setFromApplication(from);
        claim.setToApplication(to);
        claim.setSource(source);
        claim.setConfidence(confidence);
        claim.setTimestamp(ts != null ? ts : Instant.now());
        return claim;
    }

    private Application findOrCreate(String name) {
        Application app = appRepo.findByName(name);
        if (app == null) {
            app = new Application();
            app.setName(name);
            app = appRepo.save(app);
        }
        return app;
    }

    private void validateRouterLog(RouterLogEntry entry) {
        if (entry == null || entry.getSourceIp() == null || entry.getTargetIp() == null) {
            throw new IllegalArgumentException("Invalid router log entry");
        }
        // TODO enhance validation using regex
    }

    private void validateCodebase(CodebaseDependency dep) {
        if (dep == null || dep.getGroupId() == null || dep.getArtifactId() == null) {
            throw new IllegalArgumentException("Invalid codebase dependency");
        }
    }

    private void validateGateway(ApiGatewayCall call) {
        if (call == null || call.getSourceService() == null || call.getTargetService() == null) {
            throw new IllegalArgumentException("Invalid API gateway call");
        }
    }
}
