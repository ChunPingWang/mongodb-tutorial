package com.mongodb.course.m20.config;

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
        mongoTemplate.indexOps("m20_claim_dashboard")
                .ensureIndex(new CompoundIndexDefinition(
                        new Document("category", 1).append("status", 1)));

        mongoTemplate.indexOps("m20_claim_dashboard")
                .ensureIndex(new Index().on("lastUpdatedAt", Sort.Direction.DESC));
    }
}
