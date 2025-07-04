package com.example.mapper.service;

import com.enterprise.dependency.model.core.Claim;
import com.example.mapper.repo.ClaimRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DependencyResolver {
    private final ClaimRepository claimRepo;
    private final Map<String, Double> sourceCredibility = new HashMap<>();

    public DependencyResolver(ClaimRepository claimRepo) {
        this.claimRepo = claimRepo;
    }

    public Map<String, Map<String, Claim>> resolve() {
        List<Claim> claims = claimRepo.findAll();
        Map<String, Map<String, Claim>> result = new HashMap<>();

        Map<String, Map<String, List<Claim>>> grouped = claims.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getFromApplication().getName(),
                        Collectors.groupingBy(c -> c.getToApplication().getName())));

        for (var fromEntry : grouped.entrySet()) {
            String from = fromEntry.getKey();
            result.computeIfAbsent(from, k -> new HashMap<>());
            for (var toEntry : fromEntry.getValue().entrySet()) {
                String to = toEntry.getKey();
                List<Claim> options = toEntry.getValue();
                Claim best = null;
                double bestScore = -1.0;

                for (Claim claim : options) {
                    double cred = sourceCredibility.getOrDefault(claim.getSource(), 0.8);
                    double score = cred * claim.getConfidence();
                    if (score > bestScore) {
                        bestScore = score;
                        best = claim;
                    }
                }

                for (Claim claim : options) {
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
            .map(c -> c.getFromApplication().getName() + "->" + c.getToApplication().getName())
            .collect(Collectors.toList());
    }
}
