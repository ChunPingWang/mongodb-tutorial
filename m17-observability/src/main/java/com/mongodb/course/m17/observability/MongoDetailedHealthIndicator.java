package com.mongodb.course.m17.observability;

import org.bson.Document;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoDetailedHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;

    public MongoDetailedHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Health health() {
        try {
            var result = mongoTemplate.getDb().runCommand(new Document("ping", 1));

            var buildInfo = mongoTemplate.getDb().runCommand(new Document("buildInfo", 1));
            var version = buildInfo.getString("version");

            var databaseName = mongoTemplate.getDb().getName();

            var collectionNames = mongoTemplate.getDb().listCollectionNames();
            int collectionsCount = 0;
            for (var ignored : collectionNames) {
                collectionsCount++;
            }

            return Health.up()
                    .withDetail("version", version)
                    .withDetail("databaseName", databaseName)
                    .withDetail("collections", collectionsCount)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
