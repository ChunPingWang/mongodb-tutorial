package com.mongodb.course.m19.config;

import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import org.springframework.data.domain.Sort;

@Component
public class IndexConfig {

    private final MongoTemplate mongoTemplate;

    public IndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    void init() {
        // Transaction ledger: ESR rule for account query by time
        mongoTemplate.indexOps("m19_transaction_ledger")
                .ensureIndex(new CompoundIndexDefinition(
                        new Document("accountId", 1).append("occurredAt", -1)));

        // Account summaries: balance ranking
        mongoTemplate.indexOps("m19_account_summaries")
                .ensureIndex(new Index().on("currentBalance", Sort.Direction.DESC));
    }
}
