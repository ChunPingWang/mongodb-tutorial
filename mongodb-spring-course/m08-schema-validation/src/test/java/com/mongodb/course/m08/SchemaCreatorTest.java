package com.mongodb.course.m08;

import com.mongodb.MongoWriteException;
import com.mongodb.course.m08.banking.AccountType;
import com.mongodb.course.m08.banking.BankAccount;
import com.mongodb.course.m08.ecommerce.Product;
import com.mongodb.course.m08.service.SchemaCreatorService;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class SchemaCreatorTest {

    private static final String AUTO_COLLECTION = "m08_auto_schema_test";

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    SchemaCreatorService schemaCreatorService;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(AUTO_COLLECTION);
    }

    @Test
    void generateSchemaFromBankAccount_containsExpectedFields() {
        Document schemaDoc = schemaCreatorService.getSchemaAsDocument(BankAccount.class);

        Document jsonSchema = schemaDoc.get("$jsonSchema", Document.class);
        assertThat(jsonSchema).isNotNull();
        Document properties = jsonSchema.get("properties", Document.class);
        assertThat(properties).isNotNull();
        assertThat(properties).containsKey("accountNumber");
        assertThat(properties).containsKey("balance");
        assertThat(properties).containsKey("type");
    }

    @Test
    void generateSchemaFromProduct_containsArrayAndObject() {
        Document schemaDoc = schemaCreatorService.getSchemaAsDocument(Product.class);

        Document jsonSchema = schemaDoc.get("$jsonSchema", Document.class);
        assertThat(jsonSchema).isNotNull();
        Document properties = jsonSchema.get("properties", Document.class);
        assertThat(properties).isNotNull();
        assertThat(properties).containsKey("tags");
        assertThat(properties).containsKey("specifications");
    }

    @Test
    void autoSchemaCollection_validDocInserts() {
        schemaCreatorService.createCollectionWithAutoSchema(BankAccount.class, AUTO_COLLECTION);

        // Use mongoTemplate.save() so Spring Data handles type conversion (including _id)
        BankAccount account = new BankAccount("ACC-12345", "Alice", AccountType.SAVINGS, new BigDecimal("1000"));
        mongoTemplate.save(account, AUTO_COLLECTION);

        assertThat(mongoTemplate.getCollection(AUTO_COLLECTION).countDocuments()).isEqualTo(1);
    }

    @Test
    void autoSchemaCollection_invalidDocRejected() {
        schemaCreatorService.createCollectionWithAutoSchema(BankAccount.class, AUTO_COLLECTION);

        // Insert document with wrong type for accountNumber (int instead of string)
        Document invalid = new Document()
                .append("accountNumber", 12345)
                .append("holderName", "Alice")
                .append("type", "SAVINGS")
                .append("balance", new Decimal128(new BigDecimal("1000")))
                .append("status", "ACTIVE")
                .append("_class", "com.mongodb.course.m08.banking.BankAccount");

        assertThatThrownBy(() -> mongoTemplate.getCollection(AUTO_COLLECTION).insertOne(invalid))
                .isInstanceOf(MongoWriteException.class);
    }
}
