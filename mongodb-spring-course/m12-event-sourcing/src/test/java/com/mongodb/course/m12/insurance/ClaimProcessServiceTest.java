package com.mongodb.course.m12.insurance;

import com.mongodb.course.m12.SharedContainersConfig;
import com.mongodb.course.m12.infrastructure.EventStore;
import com.mongodb.course.m12.insurance.event.*;
import com.mongodb.course.m12.insurance.model.ClaimStatus;
import com.mongodb.course.m12.insurance.service.ClaimProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class ClaimProcessServiceTest {

    @Autowired
    ClaimProcessService claimProcessService;

    @Autowired
    EventStore eventStore;

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.remove(new Query(), "m12_claim_events");
        mongoTemplate.remove(new Query(), "m12_snapshots");
    }

    @Test
    void fileClaim_persistsEvent() {
        claimProcessService.fileClaim("CLM-S01", "POL-001", "李小花",
                new BigDecimal("200000"), "Medical");

        var claim = claimProcessService.loadClaim("CLM-S01");
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.FILED);
        assertThat(claim.getClaimedAmount()).isEqualByComparingTo(new BigDecimal("200000"));

        var events = claimProcessService.getEventHistory("CLM-S01");
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(ClaimFiled.class);
    }

    @Test
    void fullLifecycle_allEventsStored() {
        claimProcessService.fileClaim("CLM-S02", "POL-001", "李小花",
                new BigDecimal("300000"), "Medical");
        claimProcessService.investigate("CLM-S02", "張調查", "事故屬實");
        claimProcessService.assess("CLM-S02", "王評估", new BigDecimal("250000"), "合理範圍");
        claimProcessService.approve("CLM-S02", new BigDecimal("250000"), "陳主管");
        claimProcessService.pay("CLM-S02", new BigDecimal("250000"), "PAY-001");

        var events = claimProcessService.getEventHistory("CLM-S02");
        assertThat(events).hasSize(5);

        var claim = claimProcessService.loadClaim("CLM-S02");
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PAID);
    }

    @Test
    void loadClaim_fullReplay() {
        claimProcessService.fileClaim("CLM-S03", "POL-001", "李小花",
                new BigDecimal("200000"), "Medical");
        claimProcessService.investigate("CLM-S03", "張調查", "事故屬實");
        claimProcessService.assess("CLM-S03", "王評估", new BigDecimal("180000"), "合理");

        var claim = claimProcessService.loadClaim("CLM-S03");
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.ASSESSED);
        assertThat(claim.getAssessedAmount()).isEqualByComparingTo(new BigDecimal("180000"));
        assertThat(claim.getVersion()).isEqualTo(3);
    }

    @Test
    void snapshotCreatedAfterThreshold() {
        claimProcessService.fileClaim("CLM-S04", "POL-001", "李小花",
                new BigDecimal("200000"), "Medical");
        claimProcessService.investigate("CLM-S04", "張調查", "事故屬實");
        claimProcessService.assess("CLM-S04", "王評估", new BigDecimal("180000"), "合理");
        claimProcessService.approve("CLM-S04", new BigDecimal("180000"), "陳主管");
        claimProcessService.pay("CLM-S04", new BigDecimal("180000"), "PAY-001");
        // 5 events total → triggers snapshot

        var snapshot = eventStore.loadLatestSnapshot("CLM-S04", "ClaimProcess");
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().version()).isEqualTo(5);
    }

    @Test
    void auditTrail_preservesCompleteHistory() {
        claimProcessService.fileClaim("CLM-S05", "POL-001", "李小花",
                new BigDecimal("100000"), "Accident");
        claimProcessService.investigate("CLM-S05", "張調查", "事故屬實");
        claimProcessService.assess("CLM-S05", "王評估", new BigDecimal("80000"), "合理");
        claimProcessService.approve("CLM-S05", new BigDecimal("80000"), "陳主管");

        var events = claimProcessService.getEventHistory("CLM-S05");
        assertThat(events).hasSize(4);

        var eventTypes = events.stream()
                .map(e -> e.getClass().getSimpleName())
                .toList();
        assertThat(eventTypes).containsExactly(
                "ClaimFiled", "ClaimInvestigated", "ClaimAssessed", "ClaimApproved");
    }
}
