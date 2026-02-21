package com.mongodb.course.m18.ecommerce;

import com.mongodb.course.m18.SharedContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class CustomerVersionCoexistenceTest {

    private static final String COLLECTION = "m18_customers";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private VersionCoexistenceService versionCoexistenceService;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.getCollection(COLLECTION).drop();
    }

    @Test
    void mixedVersionsAllReadableAsLatest() {
        versionCoexistenceService.insertRawV1Customer("V1User", "v1@test.com", "0900000001",
                "街道一", "城市A", "100", "TW");
        versionCoexistenceService.insertRawV2Customer("V2User", "v2@test.com", "0900000002",
                new Address("街道二", "城市B", "200", "TW"));
        versionCoexistenceService.insertRawV3Customer("V3User", "v3@test.com", "0900000003",
                new Address("街道三", "城市C", "300", "TW"), "GOLD");

        var customers = customerService.findAll();
        assertThat(customers).hasSize(3);
        assertThat(customers).allSatisfy(c -> {
            assertThat(c.address()).isNotNull();
            assertThat(c.loyaltyTier()).isNotNull();
        });
    }

    @Test
    void countPerVersionReflectsRawStorage() {
        versionCoexistenceService.insertRawV1Customer("A", "a@test.com", "0900000001",
                "s", "c", "100", "TW");
        versionCoexistenceService.insertRawV2Customer("B", "b@test.com", "0900000002",
                new Address("s", "c", "200", "TW"));
        versionCoexistenceService.insertRawV3Customer("C", "c@test.com", "0900000003",
                new Address("s", "c", "300", "TW"), "SILVER");

        var counts = versionCoexistenceService.countPerVersion();
        assertThat(counts).containsEntry(1, 1L);
        assertThat(counts).containsEntry(2, 1L);
        assertThat(counts).containsEntry(3, 1L);
    }
}
