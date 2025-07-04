package com.example.mapper.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Data;

/**
 * Represents an application/service participating in the dependency graph.
 *
 * <pre>{@code
 * ApplicationService svc = new ApplicationService();
 * svc.setName("ServiceA");
 * }</pre>
 */
@Data
@Entity
public class ApplicationService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
}
