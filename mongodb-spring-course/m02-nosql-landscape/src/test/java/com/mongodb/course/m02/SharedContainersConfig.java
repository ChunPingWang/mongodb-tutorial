package com.mongodb.course.m02;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;

import java.net.InetSocketAddress;

@TestConfiguration(proxyBeanMethods = false)
public class SharedContainersConfig {

    private static final MongoDBContainer mongodb = new MongoDBContainer("mongo:8.0");

    @SuppressWarnings("resource")
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private static final CassandraContainer<?> cassandra = new CassandraContainer<>("cassandra:4.1");

    static {
        mongodb.start();
        redis.start();
        cassandra.start();
        initCassandraSchema();
    }

    private static void initCassandraSchema() {
        try (CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(cassandra.getHost(), cassandra.getMappedPort(9042)))
                .withLocalDatacenter("datacenter1")
                .build()) {
            session.execute(
                    "CREATE KEYSPACE IF NOT EXISTS testks " +
                    "WITH replication = {'class':'SimpleStrategy','replication_factor':'1'}");
            session.execute(
                    "CREATE TABLE IF NOT EXISTS testks.products (" +
                    "category text, id text, name text, price decimal, " +
                    "PRIMARY KEY (category, id))");
        }
    }

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDBContainer() {
        return mongodb;
    }

    @Bean
    LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
    }

    @Bean
    CqlSession cqlSession() {
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress(cassandra.getHost(), cassandra.getMappedPort(9042)))
                .withLocalDatacenter("datacenter1")
                .withKeyspace("testks")
                .build();
    }
}
