package com.mongodb.course.m04;

import com.mongodb.course.m04.bson.BsonTypeDemoDocument;
import com.mongodb.course.m04.bson.BsonTypeDemoDocument.Address;
import com.mongodb.course.m04.bson.BsonTypeDemoRepository;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M04: BSON type mapping — verify Java types map to correct BSON types.
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
class BsonTypeMappingTest {

    @Autowired
    private BsonTypeDemoRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("BigDecimal maps to BSON Decimal128 with @Field annotation")
    void bigDecimalToDecimal128() {
        var doc = new BsonTypeDemoDocument(
                new BigDecimal("99999.99"), Instant.now(), List.of(), new Address("Taipei", "100"));
        repository.save(doc);

        // Read raw BSON to verify type
        Document raw = mongoTemplate.getCollection("bson_type_demo")
                .find(new Document("_id", new ObjectId(doc.getId()))).first();
        assertThat(raw).isNotNull();
        assertThat(raw.get("amount")).isInstanceOf(Decimal128.class);
        assertThat(((Decimal128) raw.get("amount")).bigDecimalValue())
                .isEqualByComparingTo(new BigDecimal("99999.99"));
    }

    @Test
    @DisplayName("Instant maps to BSON ISODate (Date)")
    void instantToISODate() {
        Instant now = Instant.now();
        var doc = new BsonTypeDemoDocument(
                new BigDecimal("100"), now, List.of(), new Address("Taipei", "100"));
        repository.save(doc);

        Document raw = mongoTemplate.getCollection("bson_type_demo")
                .find(new Document("_id", new ObjectId(doc.getId()))).first();
        assertThat(raw).isNotNull();
        assertThat(raw.get("createdAt")).isInstanceOf(Date.class);

        // Verify round-trip: read back as Java object
        BsonTypeDemoDocument found = repository.findById(doc.getId()).orElseThrow();
        assertThat(found.getCreatedAt()).isEqualTo(now.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    }

    @Test
    @DisplayName("List maps to BSON Array")
    void listToBsonArray() {
        var doc = new BsonTypeDemoDocument(
                new BigDecimal("100"), Instant.now(),
                List.of("java", "mongodb", "spring"), new Address("Taipei", "100"));
        repository.save(doc);

        Document raw = mongoTemplate.getCollection("bson_type_demo")
                .find(new Document("_id", new ObjectId(doc.getId()))).first();
        assertThat(raw).isNotNull();
        assertThat(raw.get("tags")).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) raw.get("tags");
        assertThat(tags).containsExactly("java", "mongodb", "spring");
    }

    @Test
    @DisplayName("Nested object maps to BSON embedded document")
    void nestedObjectToEmbeddedDocument() {
        var doc = new BsonTypeDemoDocument(
                new BigDecimal("100"), Instant.now(),
                List.of(), new Address("Taipei", "100"));
        repository.save(doc);

        Document raw = mongoTemplate.getCollection("bson_type_demo")
                .find(new Document("_id", new ObjectId(doc.getId()))).first();
        assertThat(raw).isNotNull();
        assertThat(raw.get("address")).isInstanceOf(Document.class);

        Document address = (Document) raw.get("address");
        assertThat(address.getString("city")).isEqualTo("Taipei");
        assertThat(address.getString("zipCode")).isEqualTo("100");
    }
}
