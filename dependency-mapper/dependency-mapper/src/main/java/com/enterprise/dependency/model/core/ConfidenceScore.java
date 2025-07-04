package com.enterprise.dependency.model.core;

import javax.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class ConfidenceScore {
    private double value;
}
