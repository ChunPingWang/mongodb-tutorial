package com.mongodb.course.m20.config;

import jakarta.annotation.PostConstruct;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.stereotype.Component;

@Component
public class SchemaValidationConfig {

    private static final String COLLECTION = "m20_policies";

    private final MongoTemplate mongoTemplate;

    public SchemaValidationConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void init() {
        if (mongoTemplate.collectionExists(COLLECTION)) {
            mongoTemplate.dropCollection(COLLECTION);
        }

        var schema = MongoJsonSchema.builder()
                .required("policyNumber", "holderName", "basePremium", "coverageAmount")
                .property(JsonSchemaProperty.string("policyNumber"))
                .property(JsonSchemaProperty.string("holderName"))
                .property(JsonSchemaProperty.number("basePremium"))
                .property(JsonSchemaProperty.number("coverageAmount"))
                .build();

        var options = CollectionOptions.empty()
                .schema(schema)
                .strictValidation()
                .failOnValidationError();

        mongoTemplate.createCollection(COLLECTION, options);
    }
}
