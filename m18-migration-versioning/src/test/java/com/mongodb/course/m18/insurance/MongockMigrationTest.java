package com.mongodb.course.m18.insurance;

import com.mongodb.course.m18.SharedContainersConfig;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
@TestPropertySource(properties = "mongock.enabled=true")
class MongockMigrationTest {

    private static final String COLLECTION = "m18_policies";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void mongockSeedsOneThousandV1Policies() {
        long count = mongoTemplate.getCollection(COLLECTION).countDocuments();
        assertThat(count).isEqualTo(1000);
    }

    @Test
    void mongockMigratesAllPoliciesToV2() {
        long v2Count = mongoTemplate.getCollection(COLLECTION)
                .countDocuments(new Document("schemaVersion", 2));
        assertThat(v2Count).isEqualTo(1000);

        long withRiskScore = mongoTemplate.getCollection(COLLECTION)
                .countDocuments(new Document("riskScore", new Document("$exists", true)));
        assertThat(withRiskScore).isEqualTo(1000);
    }

    @Test
    void mongockSetsRiskScoreByType() {
        var collection = mongoTemplate.getCollection(COLLECTION);

        var autoDoc = collection.find(new Document("type", "AUTO")).first();
        assertThat(autoDoc).isNotNull();
        assertThat(autoDoc.getInteger("riskScore")).isEqualTo(50);

        var homeDoc = collection.find(new Document("type", "HOME")).first();
        assertThat(homeDoc).isNotNull();
        assertThat(homeDoc.getInteger("riskScore")).isEqualTo(30);

        var lifeDoc = collection.find(new Document("type", "LIFE")).first();
        assertThat(lifeDoc).isNotNull();
        assertThat(lifeDoc.getInteger("riskScore")).isEqualTo(20);
    }
}
