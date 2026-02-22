package com.mongodb.course.m18.insurance.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ChangeUnit(id = "v001-seed-policies", order = "001", author = "m18")
public class V001_SeedPoliciesChangeUnit {

    private static final String COLLECTION = "m18_policies";
    private static final String[] TYPES = {"AUTO", "HOME", "LIFE"};

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        List<Document> docs = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            var doc = new Document()
                    .append("policyNumber", "POL-%05d".formatted(i + 1))
                    .append("holderName", "Holder-%d".formatted(i + 1))
                    .append("type", TYPES[i % 3])
                    .append("premium", 10000L + (i * 37L) % 50000L)
                    .append("status", i % 5 == 0 ? "EXPIRED" : "ACTIVE")
                    .append("effectiveDate", LocalDate.of(2024, 1, 1).plusDays(i % 365))
                    .append("expirationDate", LocalDate.of(2025, 1, 1).plusDays(i % 365))
                    .append("schemaVersion", 1);
            docs.add(doc);
        }
        mongoTemplate.getCollection(COLLECTION).insertMany(docs);
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection(COLLECTION).drop();
    }
}
