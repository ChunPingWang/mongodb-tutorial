package com.mongodb.course.m12.insurance;

import com.mongodb.course.m12.insurance.event.*;
import com.mongodb.course.m12.insurance.model.ClaimProcess;
import com.mongodb.course.m12.insurance.model.ClaimStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaimProcessTest {

    @Test
    void file_createsWithStatusFiled() {
        var claim = ClaimProcess.file("CLM-001", "POL-001", "李小花",
                new BigDecimal("200000"), "Medical");

        assertThat(claim.getClaimId()).isEqualTo("CLM-001");
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.FILED);
        assertThat(claim.getVersion()).isEqualTo(1);
        assertThat(claim.getUncommittedEvents()).hasSize(1);
        assertThat(claim.getUncommittedEvents().getFirst()).isInstanceOf(ClaimFiled.class);
    }

    @Test
    void fullLifecycle_fileToPayment() {
        var claim = ClaimProcess.file("CLM-001", "POL-001", "李小花",
                new BigDecimal("200000"), "Medical");
        claim.investigate("張調查", "事故屬實");
        claim.assess("王評估", new BigDecimal("180000"), "合理範圍");
        claim.approve(new BigDecimal("180000"), "陳主管");
        claim.pay(new BigDecimal("180000"), "PAY-001");

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PAID);
        assertThat(claim.getVersion()).isEqualTo(5);
        assertThat(claim.getUncommittedEvents()).hasSize(5);
    }

    @Test
    void assess_nonInvestigatedClaim_throwsException() {
        var claim = ClaimProcess.file("CLM-001", "POL-001", "李小花",
                new BigDecimal("200000"), "Medical");

        assertThatThrownBy(() -> claim.assess("王評估", new BigDecimal("180000"), "notes"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FILED");
    }

    @Test
    void reject_setsRejectedStatus() {
        var claim = ClaimProcess.file("CLM-001", "POL-001", "李小花",
                new BigDecimal("200000"), "Medical");
        claim.investigate("張調查", "證據不足");
        claim.reject("證據不足", "陳主管");

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(claim.getVersion()).isEqualTo(3);
    }

    @Test
    void replayFrom_rebuildsCorrectState() {
        String id = "CLM-001";
        var events = List.<ClaimEvent>of(
                new ClaimFiled(UUID.randomUUID().toString(), id, 1, Instant.now(),
                        "POL-001", "李小花", new BigDecimal("200000"), "Medical"),
                new ClaimInvestigated(UUID.randomUUID().toString(), id, 2, Instant.now(),
                        "張調查", "事故屬實"),
                new ClaimAssessed(UUID.randomUUID().toString(), id, 3, Instant.now(),
                        "王評估", new BigDecimal("180000"), "合理"),
                new ClaimApproved(UUID.randomUUID().toString(), id, 4, Instant.now(),
                        new BigDecimal("180000"), "陳主管"));

        var claim = ClaimProcess.replayFrom(events);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(claim.getVersion()).isEqualTo(4);
        assertThat(claim.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("180000"));
        assertThat(claim.getUncommittedEvents()).isEmpty();
    }
}
