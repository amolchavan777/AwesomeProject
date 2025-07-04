package com.example.mapper.repo;

import com.enterprise.dependency.model.core.Application;
import com.enterprise.dependency.model.core.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    boolean existsByFromApplicationAndToApplicationAndSource(Application fromApplication,
                                                            Application toApplication,
                                                            String source);
}
