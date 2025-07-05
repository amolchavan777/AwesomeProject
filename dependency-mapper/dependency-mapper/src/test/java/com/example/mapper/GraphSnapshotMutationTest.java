package com.example.mapper;

import com.enterprise.dependency.model.core.Claim;
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
        private final Map<String, Map<String, Claim>> graph;

        StubResolver(Map<String, Map<String, Claim>> graph) {
            super(null);
            this.graph = graph;
        }

        @Override
        public Map<String, Map<String, Claim>> resolve() {
            return graph;
        }
    }

    @Test
    void exportDoesNotMutateGraph() throws IOException {
        Map<String, Map<String, Claim>> graph = new HashMap<>();
        Map<String, Claim> edges = new HashMap<>();
        edges.put("B", new Claim());
        graph.put("A", edges);

        StubResolver resolver = new StubResolver(graph);
        Path dir = Files.createTempDirectory("snap");
        GraphSnapshotService service = new GraphSnapshotService(resolver, dir.toString());

        service.exportSnapshot();

        assertEquals(1, graph.size(), "graph map should not gain entries");
    }
}
