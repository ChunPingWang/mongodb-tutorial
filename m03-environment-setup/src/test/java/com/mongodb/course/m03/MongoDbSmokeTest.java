package com.mongodb.course.m03;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M03-LAB-01: Smoke test — start MongoDB container, write, and read.
 */
@Testcontainers
@SpringBootTest
class MongoDbSmokeTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0");

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void shouldConnectToMongoDB() {
        Document result = mongoTemplate.getDb().runCommand(new Document("ping", 1));
        assertThat(result.getDouble("ok")).isEqualTo(1.0);
    }

    @Test
    void shouldWriteAndReadDocument() {
        Document doc = new Document("name", "smoke-test")
                .append("status", "passed")
                .append("module", "m03");

        mongoTemplate.insert(doc, "smoke_tests");

        Document found = mongoTemplate.findById(doc.getObjectId("_id"), Document.class, "smoke_tests");
        assertThat(found).isNotNull();
        assertThat(found.getString("name")).isEqualTo("smoke-test");
        assertThat(found.getString("status")).isEqualTo("passed");
    }

    @Test
    void shouldPerformCrudOperations() {
        // Create
        Document doc = new Document("item", "widget").append("qty", 25);
        mongoTemplate.insert(doc, "inventory");

        // Read
        Document found = mongoTemplate.findById(doc.getObjectId("_id"), Document.class, "inventory");
        assertThat(found).isNotNull();
        assertThat(found.getInteger("qty")).isEqualTo(25);

        // Update
        found.put("qty", 50);
        mongoTemplate.save(found, "inventory");
        Document updated = mongoTemplate.findById(doc.getObjectId("_id"), Document.class, "inventory");
        assertThat(updated.getInteger("qty")).isEqualTo(50);

        // Delete
        mongoTemplate.remove(updated, "inventory");
        Document deleted = mongoTemplate.findById(doc.getObjectId("_id"), Document.class, "inventory");
        assertThat(deleted).isNull();
    }
}
