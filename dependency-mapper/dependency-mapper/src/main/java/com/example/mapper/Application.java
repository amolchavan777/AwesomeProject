package com.example.mapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the dependency mapper Spring Boot application.
 *
 * <pre>{@code
 * java -jar dependency-mapper.jar
 * }</pre>
 */
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
