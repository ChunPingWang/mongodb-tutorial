package com.mongodb.course.m13.insurance.projection;

import com.mongodb.course.m13.SharedContainersConfig;
import com.mongodb.course.m13.insurance.event.ClaimApproved;
import com.mongodb.course.m13.insurance.event.ClaimFiled;
import com.mongodb.course.m13.insurance.readmodel.ClaimStatisticsDocument;
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
class ClaimStatisticsProjectorTest {

    @Autowired
    private ClaimDashboardProjector dashboardProjector;

    @Autowired
    private ClaimStatisticsProjector statisticsProjector;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanup() {
        mongoTemplate.remove(new Query(), "m13_claim_dashboards");
        mongoTemplate.remove(new Query(), "m13_claim_statistics");
    }

    @Test
    void projectMultipleClaimsFiled_upsertsByCategory() {
        var filed1 = new ClaimFiled(UUID.randomUUID().toString(), "CLM-S01", 1,
                Instant.now(), "POL-001", "Alice", new BigDecimal("100000"), "Medical");
        var filed2 = new ClaimFiled(UUID.randomUUID().toString(), "CLM-S02", 1,
                Instant.now(), "POL-002", "Bob", new BigDecimal("200000"), "Medical");
        var filed3 = new ClaimFiled(UUID.randomUUID().toString(), "CLM-S03", 1,
                Instant.now(), "POL-003", "Carol", new BigDecimal("300000"), "Accident");

        dashboardProjector.project(filed1);
        statisticsProjector.project(filed1);
        dashboardProjector.project(filed2);
        statisticsProjector.project(filed2);
        dashboardProjector.project(filed3);
        statisticsProjector.project(filed3);

        var medical = mongoTemplate.findById("Medical", ClaimStatisticsDocument.class, "m13_claim_statistics");
        assertThat(medical).isNotNull();
        assertThat(medical.totalClaims()).isEqualTo(2);
        assertThat(medical.totalClaimedAmount()).isEqualByComparingTo(new BigDecimal("300000"));

        var accident = mongoTemplate.findById("Accident", ClaimStatisticsDocument.class, "m13_claim_statistics");
        assertThat(accident).isNotNull();
        assertThat(accident.totalClaims()).isEqualTo(1);
    }

    @Test
    void projectApprovedClaim_updatesApprovedStats() {
        var filed = new ClaimFiled(UUID.randomUUID().toString(), "CLM-S04", 1,
                Instant.now(), "POL-004", "Dave", new BigDecimal("200000"), "Medical");
        var approved = new ClaimApproved(UUID.randomUUID().toString(), "CLM-S04", 4,
                Instant.now(), new BigDecimal("180000"), "Manager");

        dashboardProjector.project(filed);
        statisticsProjector.project(filed);
        statisticsProjector.project(approved);

        var stats = mongoTemplate.findById("Medical", ClaimStatisticsDocument.class, "m13_claim_statistics");
        assertThat(stats).isNotNull();
        assertThat(stats.approvedCount()).isEqualTo(1);
        assertThat(stats.totalApprovedAmount()).isEqualByComparingTo(new BigDecimal("180000"));
    }
}
