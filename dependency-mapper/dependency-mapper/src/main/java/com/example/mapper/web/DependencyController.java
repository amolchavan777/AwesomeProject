package com.example.mapper.web;

import com.enterprise.dependency.adapter.ApiGatewayAdapter;
import com.enterprise.dependency.adapter.CiCdPipelineAdapter;
import com.example.mapper.model.DependencyClaim;
import com.example.mapper.model.SourceReliability;
import com.example.mapper.service.DependencyResolver;
import com.example.mapper.service.GraphSnapshotService;
import com.example.mapper.service.SourceReliabilityService;
import com.example.mapper.service.metrics.EvaluationMetricsService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing dependency information.
 *
 * Example usage:
 * <pre>{@code
 * GET /api/dependencies
 * GET /api/dependencies/test-cicd
 * GET /api/dependencies/test-api-gateway
 * }</pre>
 */
@RestController
@RequestMapping("/api/dependencies")
public class DependencyController {
    private final DependencyResolver resolver;
    private final GraphSnapshotService snapshotService;
    private final CiCdPipelineAdapter cicdAdapter;
    private final ApiGatewayAdapter apiGatewayAdapter;
    private final SourceReliabilityService reliabilityService;
    private final com.example.mapper.repo.DependencyClaimRepository claimRepo;
    private final EvaluationMetricsService metricsService;

    /**
     * Create the controller with required services.
     */
    @Autowired
    public DependencyController(DependencyResolver resolver, GraphSnapshotService snapshotService,
                               CiCdPipelineAdapter cicdAdapter, ApiGatewayAdapter apiGatewayAdapter,
                               SourceReliabilityService reliabilityService,
                               com.example.mapper.repo.DependencyClaimRepository claimRepo,
                               EvaluationMetricsService metricsService) {
        this.resolver = resolver;
        this.snapshotService = snapshotService;
        this.cicdAdapter = cicdAdapter;
        this.apiGatewayAdapter = apiGatewayAdapter;
        this.reliabilityService = reliabilityService;
        this.claimRepo = claimRepo;
        this.metricsService = metricsService;
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
    
    /**
     * Test CI/CD Pipeline adapter with sample data.
     */
    @GetMapping("/test-cicd")
    public List<DependencyClaim> testCiCdAdapter() throws Exception {
        String sampleData = CiCdPipelineAdapter.createSampleCiCdLog();
        return cicdAdapter.processData(sampleData);
    }
    
    /**
     * Test API Gateway adapter with sample data.
     */
    @GetMapping("/test-api-gateway")
    public List<DependencyClaim> testApiGatewayAdapter() throws Exception {
        String sampleData = ApiGatewayAdapter.createSampleApiGatewayLog();
        return apiGatewayAdapter.processData(sampleData);
    }

    /**
     * Export source reliability stats for dashboard/reporting.
     */
    @GetMapping("/source-reliability")
    public List<SourceReliability> getSourceReliability() {
        return reliabilityService.getAllReliability();
    }

    /**
     * Export all dependency claims (raw, with provenance and audit trail).
     */
    @GetMapping("/claims/all")
    public List<DependencyClaim> exportAllClaims() {
        return claimRepo.findAll();
    }

    /**
     * Export all resolved/active dependency claims (with provenance and audit trail).
     */
    @GetMapping("/claims/resolved")
    public List<DependencyClaim> exportResolvedClaims() {
        return resolver.resolve().values().stream()
                .flatMap(m -> m.values().stream())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Compute evaluation metrics (precision, recall, F1) for resolved claims vs. ground truth.
     * Example: /api/dependencies/metrics?groundTruthSource=manual
     */
    @GetMapping("/metrics")
    public EvaluationMetricsService.Metrics getMetrics(@RequestParam(value = "groundTruthSource", defaultValue = "manual") String groundTruthSource) {
        List<DependencyClaim> groundTruth = claimRepo.findAll().stream()
            .filter(c -> groundTruthSource.equalsIgnoreCase(c.getSource()))
            .collect(java.util.stream.Collectors.toList());
        List<DependencyClaim> resolved = resolver.resolve().values().stream()
            .flatMap(m -> m.values().stream())
            .collect(java.util.stream.Collectors.toList());
        return metricsService.compute(groundTruth, resolved);
    }

    /**
     * Set the conflict resolution algorithm (e.g., weighted, majority, etc).
     * Example: /api/dependencies/algorithm?name=weighted
     */
    @GetMapping("/algorithm")
    public String setAlgorithm(@RequestParam("name") String name) {
        resolver.setAlgorithm(name);
        return "Algorithm set to: " + resolver.getAlgorithm();
    }
    /**
     * Get the current conflict resolution algorithm.
     */
    @GetMapping("/algorithm/current")
    public String getAlgorithm() {
        return resolver.getAlgorithm();
    }
}
