package com.example.mapper.service;

import com.example.mapper.model.DependencyClaim;
import com.example.mapper.repo.DependencyClaimRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WeightedConflictResolver {
    private final DependencyClaimRepository claimRepo;
    private final Map<String, Double> priorities;
    private final Map<String, String> overrides;

    public WeightedConflictResolver(DependencyClaimRepository claimRepo,
                                    @Value("#{${source.priorities:{}}}") Map<String, Double> priorities,
                                    @Value("#{${overrides:{}}}") Map<String, String> overrides) {
        this.claimRepo = claimRepo;
        this.priorities = priorities;
        this.overrides = overrides;
    }

    private double score(DependencyClaim claim, int frequency) {
        double priority = priorities.getOrDefault(claim.getSource(), 1.0);
        double recency = 0.0;
        Instant ts = claim.getTimestamp();
        if (ts != null) {
            recency = ts.toEpochMilli() / 1_000_000_000.0;
        }
        return claim.getConfidence() * priority + frequency + recency;
    }

    public Map<String, Map<String, DependencyClaim>> resolve() {
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
                int freq = options.size();
                DependencyClaim best = options.get(0);
                double bestScore = score(best, freq);
                for (DependencyClaim c : options) {
                    double s = score(c, freq);
                    if (s > bestScore) {
                        bestScore = s;
                        best = c;
                    }
                }

                String override = overrides.get(from + "->" + to);
                if (override != null) {
                    for (DependencyClaim c : options) {
                        if (c.getSource().equals(override)) {
                            best = c;
                            break;
                        }
                    }
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
