package com.example.mapper;

import com.example.mapper.model.DependencyClaim;
import com.example.mapper.service.DependencyResolver;
import com.example.mapper.service.GraphSnapshotService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GraphSnapshotMutationTest {

    private static class StubResolver extends DependencyResolver {
        private final Map<String, Map<String, DependencyClaim>> graph;

        StubResolver(Map<String, Map<String, DependencyClaim>> graph) {
            super(null);
            this.graph = graph;
        }

        @Override
        public Map<String, Map<String, DependencyClaim>> resolve() {
            return graph;
        }
    }

    @Test
    void exportDoesNotMutateGraph() throws IOException {
        Map<String, Map<String, DependencyClaim>> graph = new HashMap<>();
        Map<String, DependencyClaim> edges = new HashMap<>();
        edges.put("B", new DependencyClaim());
        graph.put("A", edges);

        StubResolver resolver = new StubResolver(graph);
        Path dir = Files.createTempDirectory("snap");
        GraphSnapshotService service = new GraphSnapshotService(resolver, dir.toString());

        service.exportSnapshot();

        assertEquals(1, graph.size(), "graph map should not gain entries");
    }
}
