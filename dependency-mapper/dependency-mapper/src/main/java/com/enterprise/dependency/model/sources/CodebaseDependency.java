package com.enterprise.dependency.model.sources;

import lombok.Data;

/**
 * Represents a dependency discovered in the codebase.
 */
@Data
public class CodebaseDependency {
    private String groupId;
    private String artifactId;
    private String version;
}
