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
class BulkBackgroundMigrationTest {

    private static final String COLLECTION = "m18_customers";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CustomerMigrationService migrationService;

    @Autowired
    private VersionCoexistenceService versionCoexistenceService;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.getCollection(COLLECTION).drop();
    }

    @Test
    void bulkMigrationUpgradesAllToV3() {
        for (int i = 0; i < 5; i++) {
            versionCoexistenceService.insertRawV1Customer(
                    "V1-%d".formatted(i), "v1_%d@test.com".formatted(i), "090000000%d".formatted(i),
                    "街道%d".formatted(i), "城市%d".formatted(i), "%03d".formatted(i), "TW");
        }
        for (int i = 0; i < 3; i++) {
            versionCoexistenceService.insertRawV2Customer(
                    "V2-%d".formatted(i), "v2_%d@test.com".formatted(i), "091000000%d".formatted(i),
                    new Address("街道%d".formatted(i), "城市%d".formatted(i), "%03d".formatted(i), "TW"));
        }

        int migrated = migrationService.migrateAllToLatest();
        assertThat(migrated).isEqualTo(8);

        long v3Count = mongoTemplate.getCollection(COLLECTION)
                .countDocuments(new Document("schemaVersion", 3));
        assertThat(v3Count).isEqualTo(8);
    }

    @Test
    void bulkMigrationPreservesData() {
        versionCoexistenceService.insertRawV1Customer(
                "PreserveTest", "preserve@test.com", "0912345678",
                "保存街道", "保存城市", "999", "TW");

        migrationService.migrateAllToLatest();

        var rawDoc = mongoTemplate.getCollection(COLLECTION)
                .find(new Document("name", "PreserveTest")).first();
        assertThat(rawDoc).isNotNull();
        assertThat(rawDoc.getString("name")).isEqualTo("PreserveTest");
        assertThat(rawDoc.getString("email")).isEqualTo("preserve@test.com");
        assertThat(rawDoc.getString("phone")).isEqualTo("0912345678");

        var address = rawDoc.get("address", Document.class);
        assertThat(address).isNotNull();
        assertThat(address.getString("street")).isEqualTo("保存街道");
    }
}
