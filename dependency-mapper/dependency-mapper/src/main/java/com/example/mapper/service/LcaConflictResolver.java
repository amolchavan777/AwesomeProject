package com.example.mapper.service;

import com.example.mapper.model.DependencyClaim;
import com.example.mapper.repo.DependencyClaimRepository;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Stub for Latent Credibility Analysis (LCA) conflict resolver.
 * (Future: implement EM-style iterative credibility estimation)
 */
@Service
public class LcaConflictResolver {
    private final DependencyClaimRepository claimRepo;
    public LcaConflictResolver(DependencyClaimRepository claimRepo) {
        this.claimRepo = claimRepo;
    }
    public Map<String, Map<String, DependencyClaim>> resolve() {
        // TODO: Implement EM-style LCA
        // For now, fallback to majority vote
        List<DependencyClaim> claims = claimRepo.findAll();
        Map<String, Map<String, List<DependencyClaim>>> grouped = claims.stream()
            .collect(Collectors.groupingBy(
                c -> c.getFromService().getName(),
                Collectors.groupingBy(c -> c.getToService().getName())));
        Map<String, Map<String, DependencyClaim>> result = new HashMap<>();
        for (var fromEntry : grouped.entrySet()) {
            String from = fromEntry.getKey();
            result.putIfAbsent(from, new HashMap<>());
            for (var toEntry : fromEntry.getValue().entrySet()) {
                String to = toEntry.getKey();
                List<DependencyClaim> options = toEntry.getValue();
                DependencyClaim best = options.get(0); // placeholder
                result.get(from).put(to, best);
            }
        }
        return result;
    }
}
