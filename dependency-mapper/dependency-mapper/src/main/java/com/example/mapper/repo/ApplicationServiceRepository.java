package com.example.mapper.repo;

import com.example.mapper.model.ApplicationService;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link ApplicationService} entities.
 */
public interface ApplicationServiceRepository extends JpaRepository<ApplicationService, Long> {
    /**
     * Find a service by name.
     */
    ApplicationService findByName(String name);
}
