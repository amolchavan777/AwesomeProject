package com.enterprise.dependency.model.sources;

import java.time.Instant;
import lombok.Data;

/**
 * Describes a call recorded by an API gateway.
 */
@Data
public class ApiGatewayCall {
    private Instant timestamp;
    private String sourceService;
    private String targetService;
    private String endpoint;
    private String method;
    private long responseTimeMs;
}
