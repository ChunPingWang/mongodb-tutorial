package com.mongodb.course.m10.insurance.domain;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.insurance.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaimDomainTest {

    @Test
    void file_validItems_statusFiled() {
        var items = List.of(
                new ClaimItem("Hospital stay", Money.twd(100_000), "Medical"),
                new ClaimItem("Surgery", Money.twd(100_000), "Medical")
        );

        Claim claim = Claim.file(
                new PolicyReference("POL-001"),
                new ClaimantReference("CLM-001"),
                items, Money.twd(500_000), Money.twd(10_000));

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.FILED);
        assertThat(claim.getTotalClaimedAmount().amount())
                .isEqualByComparingTo("200000");
        assertThat(claim.getDomainEvents()).hasSize(1);
    }

    @Test
    void file_exceedingCoverage_throwsException() {
        var items = List.of(
                new ClaimItem("Major surgery", Money.twd(600_000), "Medical")
        );

        assertThatThrownBy(() -> Claim.file(
                new PolicyReference("POL-002"),
                new ClaimantReference("CLM-002"),
                items, Money.twd(500_000), Money.twd(10_000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds policy coverage");
    }

    @Test
    void assess_withinLimit_approved() {
        var items = List.of(
                new ClaimItem("Treatment", Money.twd(200_000), "Medical")
        );
        Claim claim = Claim.file(
                new PolicyReference("POL-003"),
                new ClaimantReference("CLM-003"),
                items, Money.twd(500_000), Money.twd(10_000));

        // maxApprovable = 200,000 - 10,000 = 190,000
        claim.assess("Dr. Smith", Money.twd(190_000), "Approved after review");

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(claim.getAssessment().approvedAmount().amount())
                .isEqualByComparingTo("190000");
    }

    @Test
    void assess_exceedsClaimedMinusDeductible_throwsException() {
        var items = List.of(
                new ClaimItem("Treatment", Money.twd(200_000), "Medical")
        );
        Claim claim = Claim.file(
                new PolicyReference("POL-004"),
                new ClaimantReference("CLM-004"),
                items, Money.twd(500_000), Money.twd(10_000));

        // maxApprovable = 200,000 - 10,000 = 190,000; trying 250,000
        assertThatThrownBy(() ->
                claim.assess("Dr. Smith", Money.twd(250_000), "Over limit"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds");
    }
}
