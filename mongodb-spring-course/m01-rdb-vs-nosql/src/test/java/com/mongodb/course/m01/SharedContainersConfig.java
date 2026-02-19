package com.mongodb.course.m01;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Testcontainers configuration for M01 tests.
 * Containers are created once and reused across all test classes via Spring context caching.
 */
@TestConfiguration(proxyBeanMethods = false)
public class SharedContainersConfig {

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final MongoDBContainer mongodb = new MongoDBContainer("mongo:8.0");

    static {
        postgres.start();
        mongodb.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return postgres;
    }

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDBContainer() {
        return mongodb;
    }
}
