package com.example.mapper.web;

import com.example.mapper.service.DependencyResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/dependencies")
public class DependencyController {
    private final DependencyResolver resolver;

    public DependencyController(DependencyResolver resolver) {
        this.resolver = resolver;
    }

    @GetMapping
    public List<String> list() {
        return resolver.toList();
    }
}
