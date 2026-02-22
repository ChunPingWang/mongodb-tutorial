package com.mongodb.course.m20.underwriting;

import com.mongodb.course.m20.SharedContainersConfig;
import com.mongodb.course.m20.policy.AutoPolicy;
import com.mongodb.course.m20.policy.PolicyService;
import com.mongodb.course.m20.underwriting.model.PolicyApplicant;
import com.mongodb.course.m20.underwriting.model.UnderwritingStatus;
import com.mongodb.course.m20.underwriting.service.UnderwritingService;
import com.mongodb.WriteConcernException;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.bson.types.Decimal128;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class UnderwritingServiceTest {

    @Autowired private UnderwritingService underwritingService;
    @Autowired private PolicyService policyService;
    @Autowired private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), "m20_claim_events");
        mongoTemplate.remove(new Query(), "m20_snapshots");
        mongoTemplate.remove(new Query(), "m20_claim_dashboard");
        mongoTemplate.remove(new Query(), "m20_claim_statistics");
        mongoTemplate.remove(new Query(), "m20_settlement_saga_logs");
        mongoTemplate.remove(new Query(), "m20_claim_notifications");
        if (mongoTemplate.collectionExists("m20_policies")) {
            mongoTemplate.dropCollection("m20_policies");
        }
        new com.mongodb.course.m20.config.SchemaValidationConfig(mongoTemplate).init();
    }

    @Test
    void approvedWhenAllSpecsMet() {
        var applicant = new PolicyApplicant("Wang", 35, "Engineer");
        var app = underwritingService.submit(applicant, "AUTO", new BigDecimal("500000"));
        app = underwritingService.review(app);

        assertThat(app.getStatus()).isEqualTo(UnderwritingStatus.APPROVED);

        var policies = policyService.findByType(AutoPolicy.class);
        assertThat(policies).hasSize(1);
        assertThat(policies.getFirst().getHolderName()).isEqualTo("Wang");
    }

    @Test
    void rejectedOnAgeIneligibility() {
        var applicant = new PolicyApplicant("Elder Li", 70, "Retired");
        var app = underwritingService.submit(applicant, "LIFE", new BigDecimal("1000000"));
        app = underwritingService.review(app);

        assertThat(app.getStatus()).isEqualTo(UnderwritingStatus.REJECTED);
        assertThat(app.getRejectionReason()).contains("Age ineligible");
    }

    @Test
    void schemaValidationRejectsInvalidPolicy() {
        // Insert raw document missing required fields to trigger schema validation
        var rawDoc = new Document("_id", "bad-policy")
                .append("holderName", "Test");
        // Missing policyNumber, basePremium, coverageAmount

        assertThatThrownBy(() -> mongoTemplate.insert(rawDoc, "m20_policies"))
                .hasMessageContaining("Document failed validation");
    }
}
