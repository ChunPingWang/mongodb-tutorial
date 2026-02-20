package com.mongodb.course.m13.insurance;

import com.mongodb.course.m13.insurance.command.ClaimProcess;
import com.mongodb.course.m13.insurance.command.ClaimStatus;
import com.mongodb.course.m13.insurance.event.ClaimFiled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaimProcessTest {

    @Test
    void file_producesClaimFiledEvent() {
        var claim = ClaimProcess.file("CLM-001", "POL-001", "Alice",
                new BigDecimal("200000"), "Medical");

        assertThat(claim.getClaimId()).isEqualTo("CLM-001");
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.FILED);
        assertThat(claim.getUncommittedEvents()).hasSize(1);
        assertThat(claim.getUncommittedEvents().getFirst()).isInstanceOf(ClaimFiled.class);
    }

    @Test
    void fullLifecycle_producesAllEvents() {
        var claim = ClaimProcess.file("CLM-002", "POL-002", "Bob",
                new BigDecimal("300000"), "Accident");
        claim.investigate("Inspector", "Valid claim");
        claim.assess("Assessor", new BigDecimal("250000"), "Partial coverage");
        claim.approve(new BigDecimal("250000"), "Manager");
        claim.pay(new BigDecimal("250000"), "PAY-001");

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PAID);
        assertThat(claim.getVersion()).isEqualTo(5);
        assertThat(claim.getUncommittedEvents()).hasSize(5);
    }

    @Test
    void invalidStateTransition_throws() {
        var claim = ClaimProcess.file("CLM-003", "POL-003", "Carol",
                new BigDecimal("100000"), "Medical");

        assertThatThrownBy(() -> claim.assess("Assessor", new BigDecimal("80000"), "Notes"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot assess claim in status: FILED");
    }
}
