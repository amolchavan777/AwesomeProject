package com.example.mapper.service;

import com.example.mapper.model.ApplicationService;
import com.example.mapper.model.DependencyClaim;
import com.example.mapper.repo.ApplicationServiceRepository;
import com.example.mapper.repo.DependencyClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogIngestionService {
    private static final Logger log = LoggerFactory.getLogger(LogIngestionService.class);
    private final ApplicationServiceRepository serviceRepo;
    private final DependencyClaimRepository claimRepo;

    public LogIngestionService(ApplicationServiceRepository serviceRepo, DependencyClaimRepository claimRepo) {
        this.serviceRepo = serviceRepo;
        this.claimRepo = claimRepo;
    }

    @Transactional
    public void ingestLog(String path, String source, double confidence) throws IOException {
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
                ApplicationService fromSvc = serviceRepo.findByName(from);
                if (fromSvc == null) {
                    fromSvc = new ApplicationService();
                    fromSvc.setName(from);
                    fromSvc = serviceRepo.save(fromSvc);
                }
                ApplicationService toSvc = serviceRepo.findByName(to);
                if (toSvc == null) {
                    toSvc = new ApplicationService();
                    toSvc.setName(to);
                    toSvc = serviceRepo.save(toSvc);
                }
                if (claimRepo.existsByFromServiceAndToServiceAndSource(fromSvc, toSvc, source)) {
                    log.warn("Skipping duplicate dependency: {}->{}", from, to);
                    continue;
                }
                DependencyClaim claim = new DependencyClaim();
                claim.setFromService(fromSvc);
                claim.setToService(toSvc);
                claim.setSource(source);
                claim.setConfidence(confidence);
                claim.setTimestamp(Instant.now());
                claimRepo.save(claim);
            }
        }
    }
}
