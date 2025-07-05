package com.example.mapper.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Represents an application/service participating in the dependency graph.
 *
 * <pre>{@code
 * ApplicationService svc = new ApplicationService();
 * svc.setName("ServiceA");
 * }</pre>
 */
@Entity
public class ApplicationService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    // Constructors
    public ApplicationService() {
        // Default constructor for JPA
    }

    public ApplicationService(String name) {
        this.name = name;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
