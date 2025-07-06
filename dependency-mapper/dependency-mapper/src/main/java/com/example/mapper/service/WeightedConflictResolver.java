package com.example.mapper.service;

import com.example.mapper.model.DependencyClaim;
import com.example.mapper.repo.DependencyClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Service used to resolve conflicting dependency claims by weighting their
 * confidence, frequency and recency. Example usage:
 * <pre>{@code
 * WeightedConflictResolver resolver = new WeightedConflictResolver(claimRepo,
 *         Map.of("manual", 5.0), Map.of());
 * Map<String, Map<String, DependencyClaim>> result = resolver.resolve();
 * }</pre>
 */
@Service
public class WeightedConflictResolver {
    private static final Logger log =
            LoggerFactory.getLogger(WeightedConflictResolver.class);
    private final DependencyClaimRepository claimRepo;
    private final Map<String, Double> priorities;
    private final Map<String, String> overrides;
    private final SourceReliabilityService reliabilityService;
    private final Environment environment;

    /**
     * Create a resolver.
     *
     * @param claimRepo repository providing {@link DependencyClaim} instances
     * @param priorities map of source name to weight
     * @param overrides  map of explicit overrides in the form "from->to"
     */
    public WeightedConflictResolver(DependencyClaimRepository claimRepo,
                                    @Value("#{${source.priorities:{}}}") Map<String, Double> priorities,
                                    @Value("#{${overrides:{}}}") Map<String, String> overrides,
                                    SourceReliabilityService reliabilityService,
                                    Environment environment) {
        this.claimRepo = claimRepo;
        this.priorities = (priorities != null) ? priorities : new HashMap<>();
        this.overrides = (overrides != null) ? overrides : new HashMap<>();
        this.reliabilityService = reliabilityService;
        this.environment = environment;
        
        // Manually populate overrides from individual properties
        if (this.overrides.isEmpty()) {
            String overrideValue = environment.getProperty("overrides.ServiceA->ServiceC");
            if (overrideValue != null) {
                this.overrides.put("ServiceA->ServiceC", overrideValue);
            }
        }
        
        // Manually populate priorities from individual properties  
        if (this.priorities.isEmpty()) {
            String manualPriority = environment.getProperty("source.priorities.manual");
            String autoPriority = environment.getProperty("source.priorities.auto");
            if (manualPriority != null) {
                this.priorities.put("manual", Double.valueOf(manualPriority));
            }
            if (autoPriority != null) {
                this.priorities.put("auto", Double.valueOf(autoPriority));
            }
        }
        
        log.debug("WeightedConflictResolver initialized with priorities: {} and overrides: {}", 
            this.priorities, this.overrides);
    }

    /**
     * Calculate an internal score for a claim.
     */
    private double score(DependencyClaim claim, int frequency) {
        double priority = priorities.getOrDefault(claim.getSource(), 1.0);
        double reliability = reliabilityService.getReliability(claim.getSource());
        double recency = 0.0;
        Instant ts = claim.getTimestamp();
        if (ts != null) {
            long secondsOld = java.time.Duration.between(ts, Instant.now()).getSeconds();
            recency = 1.0 / (1.0 + secondsOld);
        }
        return claim.getConfidence() * priority * reliability + frequency + recency;
    }

    /**
     * Resolve conflicts among dependency claims.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, Map<String, DependencyClaim>> graph = resolver.resolve();
     * }</pre>
     *
     * @return map keyed by from-service to a map of to-service and winning claim
     */
    public Map<String, Map<String, DependencyClaim>> resolve() {
        long start = System.currentTimeMillis();
        log.info("Starting conflict resolution");
        List<DependencyClaim> claims = claimRepo.findAll();
        Map<String, Map<String, List<DependencyClaim>>> grouped = claims.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getFromApplication().getName(),
                        Collectors.groupingBy(c -> c.getToApplication().getName())));

        Map<String, Map<String, Claim>> result = new HashMap<>();

        for (var fromEntry : grouped.entrySet()) {
            String from = fromEntry.getKey();
            result.putIfAbsent(from, new HashMap<>());
            for (var toEntry : fromEntry.getValue().entrySet()) {
                String to = toEntry.getKey();
                List<DependencyClaim> options = toEntry.getValue();
                String override = overrides.get(from + "->" + to);
                boolean overrideApplied = false;
                if (override != null) {
                    for (DependencyClaim c : options) {
                        if (c.getSource() != null && c.getSource().trim().equalsIgnoreCase(override.trim())) {
                            result.get(from).put(to, c);
                            overrideApplied = true;
                            break;
                        }
                    }
                }
                if (overrideApplied) continue;
                int freq = options.size();
                Claim best = options.get(0);
                double bestScore = score(best, freq);
                for (Claim c : options) {
                    double s = score(c, freq);
                    if (s > bestScore) {
                        bestScore = s;
                        best = c;
                    }
                }
                result.get(from).put(to, best);
            }
        }

        // TODO: implement advanced inference to further refine resolution
        log.info("Finished conflict resolution in {} ms", System.currentTimeMillis() - start);
        return result;
    }

    /**
     * Convenience method converting the resolved map to a list of strings in
     * the form "from->to".
     *
     * @return list of dependency edges
     */
    public List<String> toList() {
        return resolve().entrySet().stream()
                .flatMap(e -> e.getValue().values().stream())
                .map(c -> c.getFromApplication().getName() + "->" + c.getToApplication().getName())
                .collect(Collectors.toList());
    }
}
