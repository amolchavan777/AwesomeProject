package com.example.mapper.service;

import com.example.mapper.model.DependencyClaim;
import com.example.mapper.repo.DependencyClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Resolves dependencies by choosing the most credible claim for each edge.
 *
 * <pre>{@code
 * DependencyResolver resolver = new DependencyResolver(repository);
 * Map<String, Map<String, DependencyClaim>> graph = resolver.resolve();
 * }</pre>
 */
@Service
public class DependencyResolver {
    private static final Logger log = LoggerFactory.getLogger(DependencyResolver.class);
    private final DependencyClaimRepository claimRepo;
    private final WeightedConflictResolver weightedResolver;
    private final MajorityVoteConflictResolver majorityResolver;
    private final LcaConflictResolver lcaResolver;
    private final Map<String, Double> sourceCredibility = new HashMap<>();
    private String algorithm = "weighted";

    /**
     * Create a resolver using the given repository.
     */
    public DependencyResolver(DependencyClaimRepository claimRepo, WeightedConflictResolver weightedResolver, MajorityVoteConflictResolver majorityResolver, LcaConflictResolver lcaResolver) {
        this.claimRepo = claimRepo;
        this.weightedResolver = weightedResolver;
        this.majorityResolver = majorityResolver;
        this.lcaResolver = lcaResolver;
    }

    /**
     * Set the conflict resolution algorithm ("weighted" or others in future).
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Get the current conflict resolution algorithm.
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Resolve all dependency claims using the selected algorithm.
     */
    public Map<String, Map<String, DependencyClaim>> resolve() {
        if ("weighted".equalsIgnoreCase(algorithm)) {
            return weightedResolver.resolve();
        } else if ("majority".equalsIgnoreCase(algorithm)) {
            return majorityResolver.resolve();
        } else if ("lca".equalsIgnoreCase(algorithm)) {
            return lcaResolver.resolve();
        }
        return weightedResolver.resolve(); // fallback
    }

    /**
     * Convert the resolved graph into a list of edges.
     */
    public List<String> toList() {
        return resolve().entrySet().stream()
            .flatMap(e -> e.getValue().values().stream())
            .map(c -> c.getFromService().getName() + "->" + c.getToService().getName())
            .collect(Collectors.toList());
    }
}
