package com.mongodb.course.m18.ecommerce;

import com.mongodb.course.m18.SharedContainersConfig;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class LazyMigrationConverterTest {

    private static final String COLLECTION = "m18_customers";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CustomerService customerService;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.getCollection(COLLECTION).drop();
    }

    @Test
    void readV1DocumentReturnsCustomerWithEmbeddedAddress() {
        mongoTemplate.getCollection(COLLECTION).insertOne(new Document()
                .append("name", "Alice")
                .append("email", "alice@test.com")
                .append("phone", "0912345678")
                .append("street", "信義路一段")
                .append("city", "台北市")
                .append("zipCode", "110")
                .append("country", "TW")
                .append("schemaVersion", 1));

        var customer = customerService.findByName("Alice");
        assertThat(customer).isNotNull();
        assertThat(customer.address()).isNotNull();
        assertThat(customer.address().street()).isEqualTo("信義路一段");
        assertThat(customer.address().city()).isEqualTo("台北市");
        assertThat(customer.loyaltyTier()).isEqualTo("BRONZE");
    }

    @Test
    void readV2DocumentAddsLoyaltyTierAndRegisteredAt() {
        mongoTemplate.getCollection(COLLECTION).insertOne(new Document()
                .append("name", "Bob")
                .append("email", "bob@test.com")
                .append("phone", "0987654321")
                .append("address", new Document()
                        .append("street", "中山路")
                        .append("city", "新北市")
                        .append("zipCode", "220")
                        .append("country", "TW"))
                .append("schemaVersion", 2));

        var customer = customerService.findByName("Bob");
        assertThat(customer).isNotNull();
        assertThat(customer.loyaltyTier()).isEqualTo("BRONZE");
        assertThat(customer.registeredAt()).isNotNull();
    }

    @Test
    void saveCustomerPersistsAsV3() {
        mongoTemplate.getCollection(COLLECTION).insertOne(new Document()
                .append("name", "Charlie")
                .append("email", "charlie@test.com")
                .append("phone", "0911111111")
                .append("street", "前鎮路")
                .append("city", "高雄市")
                .append("zipCode", "806")
                .append("country", "TW")
                .append("schemaVersion", 1));

        var customer = customerService.findByName("Charlie");
        customerService.save(customer);

        var rawDoc = mongoTemplate.getCollection(COLLECTION)
                .find(new Document("name", "Charlie")).first();
        assertThat(rawDoc).isNotNull();
        assertThat(rawDoc.getInteger("schemaVersion")).isEqualTo(3);
        assertThat(rawDoc.get("street")).isNull();
        assertThat(rawDoc.get("address", Document.class)).isNotNull();
    }
}
