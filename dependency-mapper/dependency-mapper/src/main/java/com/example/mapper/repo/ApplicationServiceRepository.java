package com.example.mapper.repo;

import com.example.mapper.model.ApplicationService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationServiceRepository extends JpaRepository<ApplicationService, Long> {
    ApplicationService findByName(String name);
}
