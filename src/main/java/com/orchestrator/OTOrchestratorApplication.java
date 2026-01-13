package com.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application for OT Orchestrator
 * 
 * This application uses Apache Camel for orchestration and integration
 * patterns.
 * Designed to run on OpenJDK 22 on RHEL.
 */
@SpringBootApplication
public class OTOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OTOrchestratorApplication.class, args);
    }
}
