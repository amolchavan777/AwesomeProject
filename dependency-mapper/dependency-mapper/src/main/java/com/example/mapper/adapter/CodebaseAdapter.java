package com.example.mapper.adapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class CodebaseAdapter implements DataSourceAdapter<List<String>> {
    private final Path path;

    public CodebaseAdapter(Path path) {
        this.path = path;
    }

    @Override
    public List<String> fetch() throws IOException {
        if (path == null || !Files.exists(path)) {
            return List.of();
        }
        return Files.list(path)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
    }
}
