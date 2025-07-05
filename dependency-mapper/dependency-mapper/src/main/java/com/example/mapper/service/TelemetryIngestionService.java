package com.example.mapper.service;

import com.enterprise.dependency.model.core.Application;
import com.enterprise.dependency.model.core.Claim;
import com.example.mapper.repo.ApplicationRepository;
import com.example.mapper.repo.ClaimRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingests telemetry CSV files formatted as
 * {@code timestamp,sourceService,targetService,count}. Lines not matching
 * the expected format are ignored. Confidence is derived from the count and
 * capped between 0.7 and 1.0.
 */
@Service
public class TelemetryIngestionService {
    private static final Logger log = LoggerFactory.getLogger(TelemetryIngestionService.class);
    private static final Pattern LINE = Pattern.compile("^(\\S+),(\\S+),(\\S+),(\\d+)$");

    private final ApplicationRepository appRepo;
    private final ClaimRepository claimRepo;

    public TelemetryIngestionService(ApplicationRepository appRepo, ClaimRepository claimRepo) {
        this.appRepo = appRepo;
        this.claimRepo = claimRepo;
    }

    /**
     * Parse a telemetry CSV file and persist dependency claims.
     *
     * @param path path to the telemetry file
     */
    @Transactional
    public void ingestTelemetry(String path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(Path.of(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                Claim claim = parseLine(line);
                if (claim == null) {
                    log.warn("Skipping malformed telemetry line: {}", line);
                    continue;
                }
                claimRepo.save(claim);
            }
        }
    }

    private Claim parseLine(String line) {
        Matcher m = LINE.matcher(line.trim());
        if (!m.matches()) {
            return null;
        }
        Instant ts;
        try {
            ts = Instant.parse(m.group(1));
        } catch (Exception e) {
            return null;
        }
        String from = m.group(2);
        String to = m.group(3);
        int count = Integer.parseInt(m.group(4));

        if (from.isEmpty() || to.isEmpty()) {
            return null;
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

        Claim claim = new Claim();
        claim.setFromApplication(fromApp);
        claim.setToApplication(toApp);
        claim.setSource("telemetry");
        double confidence = 0.7 + Math.min(count, 50) / 200.0; // cap at 0.95
        claim.setConfidence(Math.min(1.0, confidence));
        claim.setTimestamp(ts);
        return claim;
    }
}
