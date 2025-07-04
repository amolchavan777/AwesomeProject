package com.example.mapper.repo;

import com.enterprise.dependency.model.core.Application;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Application findByName(String name);
}
