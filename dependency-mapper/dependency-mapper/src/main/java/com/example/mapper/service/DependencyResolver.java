package com.example.mapper.service;

import com.example.mapper.model.DependencyClaim;
import com.example.mapper.repo.DependencyClaimRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DependencyResolver {
    private final DependencyClaimRepository claimRepo;
    private final Map<String, Double> sourceCredibility = new HashMap<>();

    public DependencyResolver(DependencyClaimRepository claimRepo) {
        this.claimRepo = claimRepo;
    }

    public Map<String, Map<String, DependencyClaim>> resolve() {
        List<DependencyClaim> claims = claimRepo.findAll();
        Map<String, Map<String, DependencyClaim>> result = new HashMap<>();

        Map<String, Map<String, List<DependencyClaim>>> grouped = claims.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getFromService().getName(),
                        Collectors.groupingBy(c -> c.getToService().getName())));

        for (var fromEntry : grouped.entrySet()) {
            String from = fromEntry.getKey();
            result.computeIfAbsent(from, k -> new HashMap<>());
            for (var toEntry : fromEntry.getValue().entrySet()) {
                String to = toEntry.getKey();
                List<DependencyClaim> options = toEntry.getValue();
                DependencyClaim best = null;
                double bestScore = -1.0;

                for (DependencyClaim claim : options) {
                    double cred = sourceCredibility.getOrDefault(claim.getSource(), 0.8);
                    double score = cred * claim.getConfidence();
                    if (score > bestScore) {
                        bestScore = score;
                        best = claim;
                    }
                }

                for (DependencyClaim claim : options) {
                    double cred = sourceCredibility.getOrDefault(claim.getSource(), 0.8);
                    if (claim == best) {
                        cred = Math.min(1.0, cred + 0.05);
                    } else {
                        cred = Math.max(0.0, cred - 0.05);
                    }
                    sourceCredibility.put(claim.getSource(), cred);
                }

                result.get(from).put(to, best);
            }
        }

        return result;
    }

    public List<String> toList() {
        return resolve().entrySet().stream()
            .flatMap(e -> e.getValue().values().stream())
            .map(c -> c.getFromService().getName() + "->" + c.getToService().getName())
            .collect(Collectors.toList());
    }
}
