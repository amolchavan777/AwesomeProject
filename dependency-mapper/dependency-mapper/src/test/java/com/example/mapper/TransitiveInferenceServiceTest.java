package com.example.mapper;

import com.enterprise.dependency.model.core.Claim;
import com.example.mapper.service.TransitiveInferenceService;
import com.example.mapper.service.WeightedConflictResolver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransitiveInferenceServiceTest {

    private static class StubResolver extends WeightedConflictResolver {
        private final Map<String, Map<String, Claim>> graph;
        StubResolver(Map<String, Map<String, Claim>> graph) {
            super(null, Map.of(), Map.of(), 1.0, 1.0);
            this.graph = graph;
        }
        @Override
        public Map<String, Map<String, Claim>> resolve() {
            return graph;
        }
    }

    @Test
    void infersTransitiveEdges() {
        Map<String, Map<String, Claim>> g = new HashMap<>();
        g.put("A", new HashMap<>());
        g.put("B", new HashMap<>());
        g.get("A").put("B", new Claim());
        g.get("B").put("C", new Claim());

        StubResolver resolver = new StubResolver(g);
        TransitiveInferenceService service = new TransitiveInferenceService(resolver);
        Map<String, Set<String>> map = service.inferTransitive();

        assertTrue(map.get("A").contains("B"));
        assertTrue(map.get("A").contains("C"), "A should depend transitively on C");
        assertTrue(map.get("B").contains("C"));
        List<String> list = service.toList();
        assertTrue(list.contains("A->C"));
    }
}
