package com.mongodb.course.m13.insurance.projection;

import com.mongodb.course.m13.SharedContainersConfig;
import com.mongodb.course.m13.insurance.event.*;
import com.mongodb.course.m13.insurance.readmodel.ClaimDashboardDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class ClaimDashboardProjectorTest {

    @Autowired
    private ClaimDashboardProjector projector;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanup() {
        mongoTemplate.remove(new Query(), "m13_claim_dashboards");
    }

    @Test
    void projectClaimFiled_createsInitialDashboard() {
        var event = new ClaimFiled(UUID.randomUUID().toString(), "CLM-001", 1,
                Instant.now(), "POL-001", "Alice", new BigDecimal("200000"), "Medical");

        projector.project(event);

        var doc = mongoTemplate.findById("CLM-001", ClaimDashboardDocument.class, "m13_claim_dashboards");
        assertThat(doc).isNotNull();
        assertThat(doc.currentStatus()).isEqualTo("FILED");
        assertThat(doc.claimantName()).isEqualTo("Alice");
        assertThat(doc.category()).isEqualTo("Medical");
        assertThat(doc.eventCount()).isEqualTo(1);
        assertThat(doc.timeline()).hasSize(1);
    }

    @Test
    void projectFullLifecycle_updatesAllFields() {
        var now = Instant.now();
        var filed = new ClaimFiled(UUID.randomUUID().toString(), "CLM-002", 1,
                now, "POL-002", "Bob", new BigDecimal("300000"), "Accident");
        var investigated = new ClaimInvestigated(UUID.randomUUID().toString(), "CLM-002", 2,
                now.plusMillis(100), "Inspector", "Valid");
        var assessed = new ClaimAssessed(UUID.randomUUID().toString(), "CLM-002", 3,
                now.plusMillis(200), "Assessor", new BigDecimal("250000"), "Partial");
        var approved = new ClaimApproved(UUID.randomUUID().toString(), "CLM-002", 4,
                now.plusMillis(300), new BigDecimal("250000"), "Manager");

        projector.project(filed);
        projector.project(investigated);
        projector.project(assessed);
        projector.project(approved);

        var doc = mongoTemplate.findById("CLM-002", ClaimDashboardDocument.class, "m13_claim_dashboards");
        assertThat(doc).isNotNull();
        assertThat(doc.currentStatus()).isEqualTo("APPROVED");
        assertThat(doc.investigatorName()).isEqualTo("Inspector");
        assertThat(doc.assessorName()).isEqualTo("Assessor");
        assertThat(doc.approverName()).isEqualTo("Manager");
        assertThat(doc.approvedAmount()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(doc.eventCount()).isEqualTo(4);
    }

    @Test
    void timeline_containsChronologicalEntries() {
        var now = Instant.now();
        var filed = new ClaimFiled(UUID.randomUUID().toString(), "CLM-003", 1,
                now, "POL-003", "Carol", new BigDecimal("100000"), "Medical");
        var investigated = new ClaimInvestigated(UUID.randomUUID().toString(), "CLM-003", 2,
                now.plusMillis(100), "Inspector", "Findings");
        var rejected = new ClaimRejected(UUID.randomUUID().toString(), "CLM-003", 3,
                now.plusMillis(200), "Insufficient evidence", "Reviewer");

        projector.project(filed);
        projector.project(investigated);
        projector.project(rejected);

        var doc = mongoTemplate.findById("CLM-003", ClaimDashboardDocument.class, "m13_claim_dashboards");
        assertThat(doc).isNotNull();
        assertThat(doc.timeline()).hasSize(3);
        assertThat(doc.timeline().get(0).eventType()).isEqualTo("ClaimFiled");
        assertThat(doc.timeline().get(1).eventType()).isEqualTo("ClaimInvestigated");
        assertThat(doc.timeline().get(2).eventType()).isEqualTo("ClaimRejected");
    }
}
