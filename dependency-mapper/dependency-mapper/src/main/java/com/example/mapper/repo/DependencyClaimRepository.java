package com.example.mapper.repo;

import com.example.mapper.model.DependencyClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.mapper.model.ApplicationService;

/**
 * Repository for {@link DependencyClaim} entities.
 */
public interface DependencyClaimRepository extends JpaRepository<DependencyClaim, Long> {
    /**
     * Check if a claim already exists for the specified edge and source.
     */
    boolean existsByFromServiceAndToServiceAndSource(ApplicationService fromService,
                                                    ApplicationService toService,
                                                    String source);
}
