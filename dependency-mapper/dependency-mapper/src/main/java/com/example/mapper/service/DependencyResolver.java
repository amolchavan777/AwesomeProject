package com.example.mapper.service;

import com.example.mapper.model.ApplicationService;
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

    public DependencyResolver(DependencyClaimRepository claimRepo) {
        this.claimRepo = claimRepo;
    }

    public Map<String, Map<String, DependencyClaim>> resolve() {
        List<DependencyClaim> claims = claimRepo.findAll();
        Map<String, Map<String, DependencyClaim>> result = new HashMap<>();
        for (DependencyClaim claim : claims) {
            String from = claim.getFromService().getName();
            String to = claim.getToService().getName();
            result.computeIfAbsent(from, k -> new HashMap<>());
            DependencyClaim existing = result.get(from).get(to);
            if (existing == null || claim.getConfidence() > existing.getConfidence()) {
                result.get(from).put(to, claim);
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
