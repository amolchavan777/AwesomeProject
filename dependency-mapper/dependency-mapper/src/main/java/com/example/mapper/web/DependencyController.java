package com.example.mapper.web;

import com.example.mapper.service.WeightedConflictResolver;
import com.example.mapper.service.GraphSnapshotService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dependencies")
public class DependencyController {
    private final WeightedConflictResolver resolver;
    private final GraphSnapshotService snapshotService;

    public DependencyController(WeightedConflictResolver resolver, GraphSnapshotService snapshotService) {
        this.resolver = resolver;
        this.snapshotService = snapshotService;
    }

    @GetMapping
    public List<String> list() {
        return resolver.toList();
    }

    @GetMapping("/export")
    public Path export() throws IOException {
        return snapshotService.exportSnapshot();
    }
}
