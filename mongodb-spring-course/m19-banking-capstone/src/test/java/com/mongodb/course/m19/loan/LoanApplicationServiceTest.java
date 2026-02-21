package com.mongodb.course.m19.loan;

import com.mongodb.MongoWriteException;
import com.mongodb.course.m19.SharedContainersConfig;
import com.mongodb.course.m19.account.service.AccountCommandService;
import com.mongodb.course.m19.loan.model.Applicant;
import com.mongodb.course.m19.loan.model.LoanStatus;
import com.mongodb.course.m19.loan.service.LoanApplicationService;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class LoanApplicationServiceTest {

    @Autowired private LoanApplicationService loanApplicationService;
    @Autowired private AccountCommandService accountCommandService;
    @Autowired private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.remove(new Query(), "m19_account_events");
        mongoTemplate.remove(new Query(), "m19_snapshots");
        mongoTemplate.remove(new Query(), "m19_account_summaries");
        mongoTemplate.remove(new Query(), "m19_transaction_ledger");
        // Drop and recreate loan_applications to reset while preserving schema
        mongoTemplate.remove(new Query(), "m19_loan_applications");
    }

    @Test
    void approvedWhenSpecsMet() {
        accountCommandService.openAccount("LOAN-T01", "王小明", new BigDecimal("200000"), "TWD");

        var applicant = new Applicant("王小明", "LOAN-T01", new BigDecimal("1800000"));
        var app = loanApplicationService.submit(applicant, new BigDecimal("1000000"), 240);
        var reviewed = loanApplicationService.review(app.getId());

        assertThat(reviewed.getStatus()).isEqualTo(LoanStatus.APPROVED);
    }

    @Test
    void rejectedOnInsufficientBalance() {
        accountCommandService.openAccount("LOAN-T02", "李小華", new BigDecimal("10000"), "TWD");

        var applicant = new Applicant("李小華", "LOAN-T02", new BigDecimal("2000000"));
        var app = loanApplicationService.submit(applicant, new BigDecimal("500000"), 120);
        var reviewed = loanApplicationService.review(app.getId());

        assertThat(reviewed.getStatus()).isEqualTo(LoanStatus.REJECTED);
        assertThat(reviewed.getRejectionReason()).contains("balance");
    }

    @Test
    void schemaValidationRejectsInvalidDoc() {
        var invalidDoc = new Document("_id", "invalid-001")
                .append("status", "SUBMITTED");
        // Missing required fields: applicantName, requestedAmount, termMonths, annualIncome

        assertThatThrownBy(() ->
                mongoTemplate.getCollection("m19_loan_applications").insertOne(invalidDoc))
                .isInstanceOf(MongoWriteException.class);
    }
}
