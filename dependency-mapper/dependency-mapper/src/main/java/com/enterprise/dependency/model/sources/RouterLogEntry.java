package com.enterprise.dependency.model.sources;

import java.time.Instant;
import lombok.Data;

/**
 * Represents a parsed router log line.
 */
@Data
public class RouterLogEntry {
    private Instant timestamp;
    private String sourceIp;
    private String targetIp;
    private String method;
    private String path;
    private int status;
    private long responseTimeMs;
}
