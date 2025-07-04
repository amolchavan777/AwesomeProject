package com.example.mapper.web;

import com.example.mapper.service.DependencyResolver;
import com.example.mapper.service.GraphSnapshotService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing dependency information.
 *
 * Example usage:
 * <pre>{@code
 * GET /api/dependencies
 * }</pre>
 */
@RestController
@RequestMapping("/api/dependencies")
public class DependencyController {
    private final DependencyResolver resolver;
    private final GraphSnapshotService snapshotService;

    /**
     * Create the controller with required services.
     */
    public DependencyController(DependencyResolver resolver, GraphSnapshotService snapshotService) {
        this.resolver = resolver;
        this.snapshotService = snapshotService;
    }

    /**
     * List all resolved dependencies.
     */
    @GetMapping
    public List<String> list() {
        return resolver.toList();
    }

    /**
     * Export the dependency graph to a file.
     */
    @GetMapping("/export")
    public Path export() throws IOException {
        return snapshotService.exportSnapshot();
    }
}
