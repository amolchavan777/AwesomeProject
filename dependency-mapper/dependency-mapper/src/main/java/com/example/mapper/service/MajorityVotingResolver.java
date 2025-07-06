package com.example.mapper.service;

import com.example.mapper.model.DependencyClaim;
import com.example.mapper.repo.DependencyClaimRepository;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple majority voting resolver: picks the most frequent claim for each dependency edge.
 */
@Service
public class MajorityVotingResolver {
    private final DependencyClaimRepository claimRepo;
    public MajorityVotingResolver(DependencyClaimRepository claimRepo) {
        this.claimRepo = claimRepo;
    }
    public Map<String, Map<String, DependencyClaim>> resolve() {
        List<DependencyClaim> claims = claimRepo.findAll();
        Map<String, Map<String, List<DependencyClaim>>> grouped = claims.stream()
            .collect(Collectors.groupingBy(
                c -> c.getFromService().getName(),
                Collectors.groupingBy(c -> c.getToService().getName())
            ));
        Map<String, Map<String, DependencyClaim>> result = new HashMap<>();
        for (var fromEntry : grouped.entrySet()) {
            String from = fromEntry.getKey();
            result.putIfAbsent(from, new HashMap<>());
            for (var toEntry : fromEntry.getValue().entrySet()) {
                String to = toEntry.getKey();
                List<DependencyClaim> options = toEntry.getValue();
                DependencyClaim best = options.stream()
                    .collect(Collectors.groupingBy(
                        c -> c.getSource() + ":" + c.getConfidence(), Collectors.counting()))
                    .entrySet().stream().max(Map.Entry.comparingByValue())
                    .map(e -> options.get(0)).orElse(options.get(0)); // fallback
                result.get(from).put(to, best);
            }
        }
        return result;
    }
}
