package com.example.mapper.repo;

import com.example.mapper.model.DependencyClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.mapper.model.ApplicationService;
import java.util.List;

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
    
    /**
     * Find all claims between two specific services by their names.
     */
    @Query("SELECT dc FROM DependencyClaim dc WHERE dc.fromService.name = :fromName AND dc.toService.name = :toName")
    List<DependencyClaim> findByFromServiceNameAndToServiceName(@Param("fromName") String fromServiceName, 
                                                               @Param("toName") String toServiceName);

    /**
     * Get all service dependencies for analytics (returns from and to service names)
     */
    @Query("SELECT dc.fromService.name, dc.toService.name FROM DependencyClaim dc")
    List<Object[]> findAllServiceDependencies();
}
