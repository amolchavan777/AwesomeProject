package com.example.mapper.service;

import com.enterprise.dependency.model.core.Application;
import com.enterprise.dependency.model.core.Claim;
import com.example.mapper.repo.ApplicationRepository;
import com.example.mapper.repo.ClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingests dependency logs in the format {@code ServiceA->ServiceB}.
 * <p>
 * Example:
 * <pre>{@code
 * logIngestionService.ingestLog("/tmp/deps.log", "manual", 1.0);
 * }</pre>
 */
@Service
public class LogIngestionService {
    private static final Logger log = LoggerFactory.getLogger(LogIngestionService.class);
    private final ApplicationRepository appRepo;
    private final ClaimRepository claimRepo;

    /**
     * Create the service.
     */
    public LogIngestionService(ApplicationServiceRepository serviceRepo, DependencyClaimRepository claimRepo) {
        this.serviceRepo = serviceRepo;
        this.claimRepo = claimRepo;
    }

    /**
     * Ingest a log file and persist claims.
     */
    @Transactional
    public void ingestLog(String path, String source, double confidence) throws IOException {
        long start = System.currentTimeMillis();
        log.info("Ingesting log from {}", path);
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("->");
                if (parts.length != 2) {
                    log.warn("Skipping malformed line: {}", line);
                    continue;
                }
                String from = parts[0].trim();
                String to = parts[1].trim();
                if (from.isEmpty() || to.isEmpty()) {
                    log.warn("Skipping line with empty service name: {}", line);
                    continue;
                }
                Application fromApp = appRepo.findByName(from);
                if (fromApp == null) {
                    fromApp = new Application();
                    fromApp.setName(from);
                    fromApp = appRepo.save(fromApp);
                }
                Application toApp = appRepo.findByName(to);
                if (toApp == null) {
                    toApp = new Application();
                    toApp.setName(to);
                    toApp = appRepo.save(toApp);
                }
                if (claimRepo.existsByFromApplicationAndToApplicationAndSource(fromApp, toApp, source)) {
                    log.warn("Skipping duplicate dependency: {}->{}", from, to);
                    continue;
                }
                Claim claim = new Claim();
                claim.setFromApplication(fromApp);
                claim.setToApplication(toApp);
                claim.setSource(source);
                claim.setConfidence(confidence);
                claim.setTimestamp(Instant.now());
                claimRepo.save(claim);
            }
        }
        log.info("Finished log ingestion in {} ms", System.currentTimeMillis() - start);
    }
}
