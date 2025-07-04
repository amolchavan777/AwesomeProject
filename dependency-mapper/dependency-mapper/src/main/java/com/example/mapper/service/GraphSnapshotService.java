package com.example.mapper.service;

import com.enterprise.dependency.model.core.Claim;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GraphSnapshotService {
    private final DependencyResolver resolver;
    private final Path snapshotDir;

    public GraphSnapshotService(DependencyResolver resolver,
                                @Value("${snapshot.dir:snapshots}") String dir) throws IOException {
        this.resolver = resolver;
        this.snapshotDir = Path.of(dir);
        Files.createDirectories(snapshotDir);
    }

    public Path exportSnapshot() throws IOException {
        Map<String, Map<String, Claim>> graph = resolver.resolve();
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
                    writer.write("    <edge source=\"" + from + "\" target=\"" + to + "\"/>\n");
                }
            }
            writer.write("  </graph>\n</graphml>\n");
        }

        cleanupOldSnapshots();
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
