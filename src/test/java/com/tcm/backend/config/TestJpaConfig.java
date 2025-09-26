package com.tcm.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@TestConfiguration
@EnableJpaRepositories(basePackages = "com.tcm.backend.repository")
@EntityScan(basePackages = "com.tcm.backend.domain")
@ComponentScan(basePackages = "com.tcm.backend")
public class TestJpaConfig {
    // This configuration ensures all entities and repositories are properly scanned
}