package com.mongodb.course.m18.ecommerce;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class VersionCoexistenceService {

    private static final String COLLECTION = "m18_customers";

    private final MongoTemplate mongoTemplate;

    public VersionCoexistenceService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void insertRawV1Customer(String name, String email, String phone,
                                     String street, String city, String zipCode, String country) {
        var doc = new Document()
                .append("name", name)
                .append("email", email)
                .append("phone", phone)
                .append("street", street)
                .append("city", city)
                .append("zipCode", zipCode)
                .append("country", country)
                .append("schemaVersion", 1);
        mongoTemplate.getCollection(COLLECTION).insertOne(doc);
    }

    public void insertRawV2Customer(String name, String email, String phone, Address address) {
        var addrDoc = new Document()
                .append("street", address.street())
                .append("city", address.city())
                .append("zipCode", address.zipCode())
                .append("country", address.country());
        var doc = new Document()
                .append("name", name)
                .append("email", email)
                .append("phone", phone)
                .append("address", addrDoc)
                .append("schemaVersion", 2);
        mongoTemplate.getCollection(COLLECTION).insertOne(doc);
    }

    public void insertRawV3Customer(String name, String email, String phone, Address address,
                                     String loyaltyTier) {
        var addrDoc = new Document()
                .append("street", address.street())
                .append("city", address.city())
                .append("zipCode", address.zipCode())
                .append("country", address.country());
        var doc = new Document()
                .append("name", name)
                .append("email", email)
                .append("phone", phone)
                .append("address", addrDoc)
                .append("loyaltyTier", loyaltyTier)
                .append("registeredAt", Date.from(Instant.now()))
                .append("schemaVersion", 3);
        mongoTemplate.getCollection(COLLECTION).insertOne(doc);
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
}
