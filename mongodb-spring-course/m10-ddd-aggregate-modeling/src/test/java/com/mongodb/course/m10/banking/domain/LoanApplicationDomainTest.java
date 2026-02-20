package com.mongodb.course.m10.banking.domain;

import com.mongodb.course.m10.banking.domain.event.LoanApplicationSubmitted;
import com.mongodb.course.m10.banking.domain.event.LoanPreliminaryReviewPassed;
import com.mongodb.course.m10.banking.domain.event.LoanPreliminaryReviewRejected;
import com.mongodb.course.m10.banking.domain.model.*;
import com.mongodb.course.m10.banking.domain.specification.IncomeToPaymentRatioSpec;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanApplicationDomainTest {

    private static final LoanTerm STANDARD_TERM = new LoanTerm(20, new BigDecimal("2.5"));

    @Test
    void submit_createsWithStatusSubmitted() {
        var applicant = new Applicant("Alice", "A123", Money.twd(1_200_000), "TechCorp");

        LoanApplication app = LoanApplication.submit(applicant, Money.twd(1_000_000), STANDARD_TERM);

        assertThat(app.getStatus()).isEqualTo(LoanStatus.SUBMITTED);
        assertThat(app.getApplicant().name()).isEqualTo("Alice");
        assertThat(app.getRequestedAmount().amount())
                .isEqualByComparingTo(new BigDecimal("1000000"));
    }

    @Test
    void submit_registersSubmittedEvent() {
        var applicant = new Applicant("Alice", "A123", Money.twd(1_200_000), "TechCorp");

        LoanApplication app = LoanApplication.submit(applicant, Money.twd(1_000_000), STANDARD_TERM);

        assertThat(app.getDomainEvents()).hasSize(1);
        assertThat(app.getDomainEvents().getFirst()).isInstanceOf(LoanApplicationSubmitted.class);
    }

    @Test
    void preliminaryReview_passesWhenIncomeAboveThreshold() {
        // Income 1,800,000; annual payment ~64,147; ratio 3 → threshold ~192,441
        var applicant = new Applicant("Bob", "B123", Money.twd(1_800_000), "BigCorp");
        LoanApplication app = LoanApplication.submit(applicant, Money.twd(1_000_000), STANDARD_TERM);

        app.performPreliminaryReview(new IncomeToPaymentRatioSpec(3));

        assertThat(app.getStatus()).isEqualTo(LoanStatus.PRELIMINARY_PASSED);
        assertThat(app.getDomainEvents()).hasSize(2);
        assertThat(app.getDomainEvents().get(1)).isInstanceOf(LoanPreliminaryReviewPassed.class);
    }

    @Test
    void preliminaryReview_rejectsWhenIncomeBelowThreshold() {
        // Income 100,000; annual payment ~64,147; ratio 3 → threshold ~192,441
        var applicant = new Applicant("Charlie", "C123", Money.twd(100_000), "SmallShop");
        LoanApplication app = LoanApplication.submit(applicant, Money.twd(1_000_000), STANDARD_TERM);

        app.performPreliminaryReview(new IncomeToPaymentRatioSpec(3));

        assertThat(app.getStatus()).isEqualTo(LoanStatus.PRELIMINARY_REJECTED);
        assertThat(app.getDomainEvents()).hasSize(2);
        assertThat(app.getDomainEvents().get(1)).isInstanceOf(LoanPreliminaryReviewRejected.class);
    }

    @Test
    void preliminaryReview_failsWhenNotSubmitted() {
        var applicant = new Applicant("Diana", "D123", Money.twd(2_000_000), "MegaCorp");
        LoanApplication app = LoanApplication.submit(applicant, Money.twd(1_000_000), STANDARD_TERM);
        app.performPreliminaryReview(new IncomeToPaymentRatioSpec(3)); // now PASSED

        assertThatThrownBy(() -> app.performPreliminaryReview(new IncomeToPaymentRatioSpec(3)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUBMITTED");
    }
}
