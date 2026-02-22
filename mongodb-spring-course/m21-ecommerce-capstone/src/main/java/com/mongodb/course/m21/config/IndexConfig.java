package com.mongodb.course.m21.config;

import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
public class IndexConfig {

    private final MongoTemplate mongoTemplate;

    public IndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    void init() {
        mongoTemplate.indexOps("m21_order_events")
                .ensureIndex(new CompoundIndexDefinition(
                        new Document("aggregateId", 1).append("version", 1))
                        .unique());

        mongoTemplate.indexOps("m21_order_dashboard")
                .ensureIndex(new CompoundIndexDefinition(
                        new Document("customerId", 1).append("status", 1)));

        mongoTemplate.indexOps("m21_order_dashboard")
                .ensureIndex(new Index().on("lastUpdatedAt", Sort.Direction.DESC));
    }
}
