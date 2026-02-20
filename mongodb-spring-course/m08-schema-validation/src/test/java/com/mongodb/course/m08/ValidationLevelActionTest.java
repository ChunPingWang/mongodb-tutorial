package com.mongodb.course.m08;

import com.mongodb.MongoWriteException;
import com.mongodb.course.m08.service.SchemaValidationService;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class ValidationLevelActionTest {

    private static final String TEST_COLLECTION = "m08_validation_level_test";

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    SchemaValidationService schemaValidationService;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(TEST_COLLECTION);
    }

    private Document validDocument() {
        return new Document()
                .append("accountNumber", "ACC-12345")
                .append("holderName", "Alice")
                .append("type", "SAVINGS")
                .append("balance", new Decimal128(new BigDecimal("1000")))
                .append("status", "ACTIVE");
    }

    private Document invalidDocument() {
        return new Document()
                .append("accountNumber", "ACC-12345")
                .append("type", "SAVINGS")
                .append("balance", new Decimal128(new BigDecimal("1000")))
                .append("status", "ACTIVE");
        // missing holderName
    }

    @Test
    void strictLevel_rejectsInvalidInsert() {
        MongoJsonSchema schema = schemaValidationService.buildBankAccountSchema();
        schemaValidationService.createCollectionStrict(TEST_COLLECTION, schema);

        assertThatThrownBy(() -> mongoTemplate.getCollection(TEST_COLLECTION).insertOne(invalidDocument()))
                .isInstanceOf(MongoWriteException.class);
    }

    @Test
    void strictLevel_rejectsInvalidUpdate() {
        MongoJsonSchema schema = schemaValidationService.buildBankAccountSchema();
        schemaValidationService.createCollectionStrict(TEST_COLLECTION, schema);

        mongoTemplate.getCollection(TEST_COLLECTION).insertOne(validDocument());

        assertThatThrownBy(() -> mongoTemplate.getCollection(TEST_COLLECTION).updateOne(
                new Document("accountNumber", "ACC-12345"),
                new Document("$unset", new Document("holderName", ""))
        )).isInstanceOf(MongoWriteException.class);
    }

    @Test
    void moderateLevel_allowsPreExistingInvalidDocs() {
        // Step 1: Create collection WITHOUT schema
        mongoTemplate.createCollection(TEST_COLLECTION);

        // Step 2: Insert an invalid document (missing holderName)
        mongoTemplate.getCollection(TEST_COLLECTION).insertOne(invalidDocument());

        // Step 3: Apply schema with moderate validation
        MongoJsonSchema schema = schemaValidationService.buildBankAccountSchema();
        schemaValidationService.applySchemaToExistingCollection(TEST_COLLECTION, schema, "moderate");

        // Step 4: Update the pre-existing invalid doc — moderate allows this
        mongoTemplate.getCollection(TEST_COLLECTION).updateOne(
                new Document("accountNumber", "ACC-12345"),
                new Document("$set", new Document("holderName", "Updated"))
        );

        Document found = mongoTemplate.getCollection(TEST_COLLECTION)
                .find(new Document("accountNumber", "ACC-12345")).first();
        assertThat(found).isNotNull();
        assertThat(found.getString("holderName")).isEqualTo("Updated");
    }

    @Test
    void moderateLevel_rejectsNewInvalidInsert() {
        MongoJsonSchema schema = schemaValidationService.buildBankAccountSchema();
        schemaValidationService.createCollectionModerate(TEST_COLLECTION, schema);

        assertThatThrownBy(() -> mongoTemplate.getCollection(TEST_COLLECTION).insertOne(invalidDocument()))
                .isInstanceOf(MongoWriteException.class);
    }

    @Test
    void warnAction_allowsInvalidInsert() {
        MongoJsonSchema schema = schemaValidationService.buildBankAccountSchema();
        schemaValidationService.createCollectionWarn(TEST_COLLECTION, schema);

        // Warn action: invalid insert succeeds (no exception)
        mongoTemplate.getCollection(TEST_COLLECTION).insertOne(invalidDocument());

        assertThat(mongoTemplate.getCollection(TEST_COLLECTION).countDocuments()).isEqualTo(1);
    }

    @Test
    void warnAction_documentActuallyPersisted() {
        MongoJsonSchema schema = schemaValidationService.buildBankAccountSchema();
        schemaValidationService.createCollectionWarn(TEST_COLLECTION, schema);

        mongoTemplate.getCollection(TEST_COLLECTION).insertOne(invalidDocument());

        Document found = mongoTemplate.getCollection(TEST_COLLECTION)
                .find(new Document("accountNumber", "ACC-12345")).first();
        assertThat(found).isNotNull();
        assertThat(found.getString("accountNumber")).isEqualTo("ACC-12345");
        assertThat(found.getString("holderName")).isNull();
    }
}
