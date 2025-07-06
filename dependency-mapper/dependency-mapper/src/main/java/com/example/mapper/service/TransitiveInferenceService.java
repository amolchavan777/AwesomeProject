package com.example.mapper.service;

import com.enterprise.dependency.model.core.Claim;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * Infers transitive dependencies from the resolved dependency graph.
 * If ServiceA depends on ServiceB and ServiceB depends on ServiceC,
 * this service will infer ServiceA -> ServiceC.
 */
@Service
public class TransitiveInferenceService {
    private final WeightedConflictResolver resolver;

    public TransitiveInferenceService(WeightedConflictResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Returns a map of each application to all applications it depends on
     * directly or transitively.
     */
    public Map<String, Set<String>> inferTransitive() {
        Map<String, Map<String, Claim>> graph = resolver.resolve();
        Map<String, Set<String>> result = new HashMap<>();
        for (String start : graph.keySet()) {
            Set<String> visited = new LinkedHashSet<>();
            Deque<String> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                String current = queue.poll();
                Map<String, Claim> edges = graph.get(current);
                if (edges == null) continue;
                for (String next : edges.keySet()) {
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
            visited.remove(start);
            result.put(start, visited);
        }
        return result;
    }

    /**
     * Convenience method returning dependencies as list of "A->B" strings.
     */
    public List<String> toList() {
        Map<String, Set<String>> map = inferTransitive();
        List<String> list = new ArrayList<>();
        for (var e : map.entrySet()) {
            String from = e.getKey();
            for (String to : e.getValue()) {
                list.add(from + "->" + to);
            }
        }
        return list;
    }
}
