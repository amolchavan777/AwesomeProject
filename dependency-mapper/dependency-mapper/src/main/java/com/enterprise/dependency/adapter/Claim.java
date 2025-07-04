package com.enterprise.dependency.adapter;

import java.time.Instant;
import lombok.Data;

/**
 * Represents a parsed dependency claim from a router log entry.
 */
@Data
public class Claim {
    private Instant timestamp;
    private String sourceIp;
    private String targetIp;
    private double confidence;
}
