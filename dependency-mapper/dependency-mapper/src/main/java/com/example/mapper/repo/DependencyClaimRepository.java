package com.example.mapper.repo;

import com.example.mapper.model.DependencyClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.mapper.model.ApplicationService;

public interface DependencyClaimRepository extends JpaRepository<DependencyClaim, Long> {
    boolean existsByFromServiceAndToServiceAndSource(ApplicationService fromService,
                                                    ApplicationService toService,
                                                    String source);
}
