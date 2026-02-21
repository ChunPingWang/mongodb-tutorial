package com.mongodb.course.m18.insurance;

import com.mongodb.course.m18.SharedContainersConfig;
import com.mongodb.course.m18.insurance.changeunit.V001_SeedPoliciesChangeUnit;
import com.mongodb.course.m18.insurance.changeunit.V002_AddRiskScoreChangeUnit;
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
class PolicyMigrationRollbackTest {

    private static final String COLLECTION = "m18_policies";

    @Autowired
    private MongoTemplate mongoTemplate;

    private final V001_SeedPoliciesChangeUnit v001 = new V001_SeedPoliciesChangeUnit();
    private final V002_AddRiskScoreChangeUnit v002 = new V002_AddRiskScoreChangeUnit();

    @BeforeEach
    void cleanUp() {
        mongoTemplate.getCollection(COLLECTION).drop();
    }

    @Test
    void rollbackV002RemovesRiskScoreAndRegion() {
        v001.execution(mongoTemplate);
        v002.execution(mongoTemplate);

        // Verify V2 state
        assertThat(mongoTemplate.getCollection(COLLECTION)
                .countDocuments(new Document("schemaVersion", 2))).isEqualTo(1000);

        // Rollback V002
        v002.rollback(mongoTemplate);

        // Verify rolled back to V1
        long v1Count = mongoTemplate.getCollection(COLLECTION)
                .countDocuments(new Document("schemaVersion", 1));
        assertThat(v1Count).isEqualTo(1000);

        long withRiskScore = mongoTemplate.getCollection(COLLECTION)
                .countDocuments(new Document("riskScore", new Document("$exists", true)));
        assertThat(withRiskScore).isZero();

        long withRegion = mongoTemplate.getCollection(COLLECTION)
                .countDocuments(new Document("region", new Document("$exists", true)));
        assertThat(withRegion).isZero();
    }

    @Test
    void rollbackV001DropsCollection() {
        v001.execution(mongoTemplate);
        assertThat(mongoTemplate.getCollection(COLLECTION).countDocuments()).isEqualTo(1000);

        v001.rollback(mongoTemplate);

        assertThat(mongoTemplate.getCollection(COLLECTION).countDocuments()).isZero();
    }
}
