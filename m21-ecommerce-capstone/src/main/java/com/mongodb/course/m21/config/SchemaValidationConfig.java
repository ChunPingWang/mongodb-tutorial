package com.mongodb.course.m21.config;

import jakarta.annotation.PostConstruct;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.stereotype.Component;

@Component
public class SchemaValidationConfig {

    private static final String COLLECTION = "m21_products";

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
                .required("sku", "name", "category", "price", "stockQuantity")
                .property(JsonSchemaProperty.string("sku"))
                .property(JsonSchemaProperty.string("name"))
                .property(JsonSchemaProperty.string("category"))
                .property(JsonSchemaProperty.number("price"))
                .property(JsonSchemaProperty.int32("stockQuantity"))
                .build();

        var options = CollectionOptions.empty()
                .schema(schema)
                .strictValidation()
                .failOnValidationError();

        mongoTemplate.createCollection(COLLECTION, options);
    }
}
