package com.mongodb.course.m07;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;

@TestConfiguration(proxyBeanMethods = false)
public class SharedContainersConfig {

    private static final MongoDBContainer mongodb = new MongoDBContainer("mongo:8.0");

    static {
        mongodb.start();
    }

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDBContainer() {
        return mongodb;
    }
}
