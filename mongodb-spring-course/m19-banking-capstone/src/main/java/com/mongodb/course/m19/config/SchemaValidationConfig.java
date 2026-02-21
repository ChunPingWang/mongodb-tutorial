package com.mongodb.course.m19.config;

import jakarta.annotation.PostConstruct;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.stereotype.Component;

@Component
public class SchemaValidationConfig {

    private static final String COLLECTION = "m19_loan_applications";

    private final MongoTemplate mongoTemplate;

    public SchemaValidationConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    void init() {
        if (mongoTemplate.collectionExists(COLLECTION)) {
            mongoTemplate.dropCollection(COLLECTION);
        }

        var schema = MongoJsonSchema.builder()
                .required("applicantName", "requestedAmount", "termMonths", "status", "annualIncome")
                .property(JsonSchemaProperty.string("applicantName"))
                .property(JsonSchemaProperty.number("requestedAmount"))
                .property(JsonSchemaProperty.int32("termMonths"))
                .property(JsonSchemaProperty.string("status"))
                .property(JsonSchemaProperty.number("annualIncome"))
                .build();

        var options = CollectionOptions.empty()
                .schema(schema)
                .strictValidation()
                .failOnValidationError();

        mongoTemplate.createCollection(COLLECTION, options);
    }
}
