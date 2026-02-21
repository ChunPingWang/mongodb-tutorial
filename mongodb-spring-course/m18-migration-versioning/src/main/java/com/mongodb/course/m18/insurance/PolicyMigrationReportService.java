package com.mongodb.course.m18.insurance;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PolicyMigrationReportService {

    private static final String COLLECTION = "m18_policies";

    private final MongoTemplate mongoTemplate;

    public PolicyMigrationReportService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Map<Integer, Long> countPerVersion() {
        var aggregation = Aggregation.newAggregation(
                Aggregation.group("schemaVersion").count().as("count")
        );
        AggregationResults<Document> results =
                mongoTemplate.aggregate(aggregation, COLLECTION, Document.class);

        Map<Integer, Long> versionCounts = new HashMap<>();
        for (var doc : results.getMappedResults()) {
            int version = doc.getInteger("_id");
            long count = doc.get("count", Number.class).longValue();
            versionCounts.put(version, count);
        }
        return versionCounts;
    }

    public boolean verifyAllMigrated(int expectedVersion) {
        long belowExpected = mongoTemplate.getCollection(COLLECTION)
                .countDocuments(new Document("schemaVersion", new Document("$lt", expectedVersion)));
        return belowExpected == 0;
    }
}
