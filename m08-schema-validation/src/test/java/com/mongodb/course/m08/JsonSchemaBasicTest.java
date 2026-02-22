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
class JsonSchemaBasicTest {

    private static final String BANK_COLLECTION = "m08_bank_accounts";
    private static final String INSURANCE_COLLECTION = "m08_insurance_policies";

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    SchemaValidationService schemaValidationService;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(BANK_COLLECTION);
        mongoTemplate.dropCollection(INSURANCE_COLLECTION);
    }

    @Test
    void createCollectionWithSchema_insertsValidDocument() {
        MongoJsonSchema schema = schemaValidationService.buildBankAccountSchema();
        schemaValidationService.createCollectionStrict(BANK_COLLECTION, schema);

        Document doc = new Document()
                .append("accountNumber", "ACC-12345")
                .append("holderName", "Alice")
                .append("type", "SAVINGS")
                .append("balance", new Decimal128(new BigDecimal("1000")))
                .append("status", "ACTIVE");
        mongoTemplate.getCollection(BANK_COLLECTION).insertOne(doc);

        assertThat(mongoTemplate.getCollection(BANK_COLLECTION).countDocuments()).isEqualTo(1);
    }

    @Test
    void schemaRejectsDocument_missingRequiredField() {
        MongoJsonSchema schema = schemaValidationService.buildBankAccountSchema();
        schemaValidationService.createCollectionStrict(BANK_COLLECTION, schema);

        Document doc = new Document()
                .append("accountNumber", "ACC-12345")
                .append("type", "SAVINGS")
                .append("balance", new Decimal128(new BigDecimal("1000")))
                .append("status", "ACTIVE");
        // missing holderName

        assertThatThrownBy(() -> mongoTemplate.getCollection(BANK_COLLECTION).insertOne(doc))
                .isInstanceOf(MongoWriteException.class);
    }

    @Test
    void schemaRejectsDocument_invalidEnumValue() {
        MongoJsonSchema schema = schemaValidationService.buildBankAccountSchema();
        schemaValidationService.createCollectionStrict(BANK_COLLECTION, schema);

        Document doc = new Document()
                .append("accountNumber", "ACC-12345")
                .append("holderName", "Alice")
                .append("type", "INVALID")
                .append("balance", new Decimal128(new BigDecimal("1000")))
                .append("status", "ACTIVE");

        assertThatThrownBy(() -> mongoTemplate.getCollection(BANK_COLLECTION).insertOne(doc))
                .isInstanceOf(MongoWriteException.class);
    }

    @Test
    void schemaRejectsDocument_stringTooShort() {
        MongoJsonSchema schema = schemaValidationService.buildBankAccountSchema();
        schemaValidationService.createCollectionStrict(BANK_COLLECTION, schema);

        Document doc = new Document()
                .append("accountNumber", "AB")
                .append("holderName", "Alice")
                .append("type", "SAVINGS")
                .append("balance", new Decimal128(new BigDecimal("1000")))
                .append("status", "ACTIVE");

        assertThatThrownBy(() -> mongoTemplate.getCollection(BANK_COLLECTION).insertOne(doc))
                .isInstanceOf(MongoWriteException.class);
    }

    @Test
    void schemaAllowsExtraFields() {
        MongoJsonSchema schema = schemaValidationService.buildBankAccountSchema();
        schemaValidationService.createCollectionStrict(BANK_COLLECTION, schema);

        Document doc = new Document()
                .append("accountNumber", "ACC-12345")
                .append("holderName", "Alice")
                .append("type", "SAVINGS")
                .append("balance", new Decimal128(new BigDecimal("1000")))
                .append("status", "ACTIVE")
                .append("nickname", "My Main Account");
        mongoTemplate.getCollection(BANK_COLLECTION).insertOne(doc);

        Document found = mongoTemplate.getCollection(BANK_COLLECTION).find().first();
        assertThat(found).isNotNull();
        assertThat(found.getString("nickname")).isEqualTo("My Main Account");
    }

    @Test
    void schemaEnforcesInsurancePolicyConstraints() {
        MongoJsonSchema schema = schemaValidationService.buildInsurancePolicySchema();
        schemaValidationService.createCollectionStrict(INSURANCE_COLLECTION, schema);

        Document doc = new Document()
                .append("holderName", "Bob")
                .append("policyType", "HEALTH")
                .append("premium", new Decimal128(new BigDecimal("500")))
                .append("coverageAmount", new Decimal128(new BigDecimal("1000000")))
                .append("status", "ACTIVE");
        // missing policyNumber

        assertThatThrownBy(() -> mongoTemplate.getCollection(INSURANCE_COLLECTION).insertOne(doc))
                .isInstanceOf(MongoWriteException.class);
    }
}
