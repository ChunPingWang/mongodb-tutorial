package com.mongodb.course.m08.service;

import org.bson.Document;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.schema.JsonSchemaProperty.number;
import static org.springframework.data.mongodb.core.schema.JsonSchemaProperty.required;
import static org.springframework.data.mongodb.core.schema.JsonSchemaProperty.string;

@Service
public class SchemaValidationService {

    private final MongoTemplate mongoTemplate;

    public SchemaValidationService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public MongoJsonSchema buildBankAccountSchema() {
        return MongoJsonSchema.builder()
                .properties(
                        required(string("accountNumber").minLength(5).maxLength(20)),
                        required(string("holderName")),
                        required(string("type").possibleValues("SAVINGS", "CHECKING")),
                        required(number("balance")),
                        required(string("status").possibleValues("ACTIVE", "CLOSED", "FROZEN"))
                )
                .build();
    }

    public MongoJsonSchema buildInsurancePolicySchema() {
        return MongoJsonSchema.builder()
                .properties(
                        required(string("policyNumber")),
                        required(string("holderName")),
                        required(string("policyType").possibleValues("TERM_LIFE", "WHOLE_LIFE", "HEALTH", "AUTO")),
                        required(number("premium")),
                        required(number("coverageAmount")),
                        required(string("status").possibleValues("ACTIVE", "EXPIRED", "CANCELLED"))
                )
                .build();
    }

    public MongoJsonSchema buildProductSchema() {
        return MongoJsonSchema.builder()
                .properties(
                        required(string("sku").minLength(1)),
                        required(string("name").minLength(1)),
                        required(string("category").minLength(1)),
                        required(number("price"))
                )
                .build();
    }

    public void createCollectionStrict(String name, MongoJsonSchema schema) {
        CollectionOptions options = CollectionOptions.empty()
                .schema(schema)
                .strictValidation()
                .failOnValidationError();
        mongoTemplate.createCollection(name, options);
    }

    public void createCollectionModerate(String name, MongoJsonSchema schema) {
        CollectionOptions options = CollectionOptions.empty()
                .schema(schema)
                .moderateValidation()
                .failOnValidationError();
        mongoTemplate.createCollection(name, options);
    }

    public void createCollectionWarn(String name, MongoJsonSchema schema) {
        CollectionOptions options = CollectionOptions.empty()
                .schema(schema)
                .strictValidation()
                .warnOnValidationError();
        mongoTemplate.createCollection(name, options);
    }

    public void applySchemaToExistingCollection(String name, MongoJsonSchema schema, String level) {
        applySchemaToExistingCollection(name, schema, level, "error");
    }

    public void applySchemaToExistingCollection(String name, MongoJsonSchema schema,
                                                 String level, String action) {
        Document command = new Document("collMod", name)
                .append("validator", schema.toDocument())
                .append("validationLevel", level)
                .append("validationAction", action);
        mongoTemplate.getDb().runCommand(command);
    }
}
