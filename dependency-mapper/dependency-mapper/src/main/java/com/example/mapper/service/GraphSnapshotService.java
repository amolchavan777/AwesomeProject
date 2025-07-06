package com.example.mapper.service;

import com.enterprise.dependency.model.core.Claim;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Exports the resolved dependency graph to GraphML files.
 * <p>
 * Usage example:
 * <pre>{@code
 * GraphSnapshotService svc = new GraphSnapshotService(resolver, "snapshots");
 * Path file = svc.exportSnapshot();
 * }</pre>
 */
@Service
public class GraphSnapshotService {
    private static final Logger log = LoggerFactory.getLogger(GraphSnapshotService.class);
    private final DependencyResolver resolver;
    private final Path snapshotDir;

    /**
     * Create the service specifying a snapshot directory.
     */
    public GraphSnapshotService(DependencyResolver resolver,
                                @Value("${snapshot.dir:snapshots}") String dir) throws IOException {
        this.resolver = resolver;
        this.snapshotDir = Path.of(dir);
        Files.createDirectories(snapshotDir);
    }

    /**
     * Export the current dependency graph to a GraphML file.
     *
     * @return path to the exported file
     */
    public Path exportSnapshot() throws IOException {
        long start = System.currentTimeMillis();
        log.info("Exporting dependency snapshot");
        Map<String, Map<String, DependencyClaim>> graph = resolver.resolve();
        Set<String> nodes = graph.keySet();
        nodes.addAll(graph.values().stream()
                .flatMap(m -> m.keySet().stream())
                .collect(Collectors.toSet()));

        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        Path file = snapshotDir.resolve("dependencies-" + timestamp + ".graphml");

        try (FileWriter writer = new FileWriter(file.toFile())) {
            writer.write("<graphml>\n");
            writer.write("  <graph edgedefault=\"directed\">\n");
            for (String node : nodes) {
                writer.write("    <node id=\"" + node + "\"/>\n");
            }
            for (var fromEntry : graph.entrySet()) {
                String from = fromEntry.getKey();
                for (var toEntry : fromEntry.getValue().entrySet()) {
                    String to = toEntry.getKey();
                    DependencyClaim claim = toEntry.getValue();
                    writer.write("    <edge source=\"" + from + "\" target=\"" + to + "\">\n");
                    writer.write("      <data key=\"confidence\">" + claim.getConfidence() + "</data>\n");
                    writer.write("      <data key=\"source\">" + claim.getSource() + "</data>\n");
                    writer.write("      <data key=\"provenance\">" + (claim.getProvenance() != null ? claim.getProvenance() : "") + "</data>\n");
                    writer.write("      <data key=\"auditTrail\">" + (claim.getAuditTrail() != null ? claim.getAuditTrail() : "") + "</data>\n");
                    writer.write("    </edge>\n");
                }
            }
            writer.write("  </graph>\n</graphml>\n");
        }

        cleanupOldSnapshots();
        log.info("Snapshot exported in {} ms", System.currentTimeMillis() - start);
        return file;
    }

    private void cleanupOldSnapshots() throws IOException {
        var files = Files.list(snapshotDir)
                .filter(p -> p.getFileName().toString().endsWith(".graphml"))
                .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                .collect(Collectors.toList());
        for (int i = 3; i < files.size(); i++) {
            Files.deleteIfExists(files.get(i));
        }
    }
}
