package com.mongodb.course.m10.banking.infrastructure;

import com.mongodb.course.m10.SharedContainersConfig;
import com.mongodb.course.m10.banking.domain.model.*;
import com.mongodb.course.m10.banking.domain.port.LoanApplicationRepository;
import com.mongodb.course.m10.banking.domain.specification.IncomeToPaymentRatioSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class MongoLoanApplicationRepositoryTest {

    @Autowired
    private LoanApplicationRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection("m10_loan_applications");
    }

    @Test
    void save_persistsAndRetrievesApplication() {
        var applicant = new Applicant("Alice", "A123", Money.twd(1_200_000), "TechCorp");
        LoanApplication app = LoanApplication.submit(applicant, Money.twd(1_000_000),
                new LoanTerm(20, new BigDecimal("2.5")));

        LoanApplication saved = repository.save(app);
        assertThat(saved.getId()).isNotNull();

        Optional<LoanApplication> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getApplicant().name()).isEqualTo("Alice");
        assertThat(found.get().getStatus()).isEqualTo(LoanStatus.SUBMITTED);
    }

    @Test
    void findByStatus_returnsMatchingApplications() {
        var applicant1 = new Applicant("Bob", "B123", Money.twd(1_800_000), "BigCorp");
        var applicant2 = new Applicant("Charlie", "C123", Money.twd(100_000), "SmallShop");
        var term = new LoanTerm(20, new BigDecimal("2.5"));

        LoanApplication app1 = LoanApplication.submit(applicant1, Money.twd(1_000_000), term);
        repository.save(app1);
        app1.performPreliminaryReview(new IncomeToPaymentRatioSpec(3));
        repository.save(app1);

        LoanApplication app2 = LoanApplication.submit(applicant2, Money.twd(1_000_000), term);
        repository.save(app2);

        List<LoanApplication> submitted = repository.findByStatus(LoanStatus.SUBMITTED);
        assertThat(submitted).hasSize(1);
        assertThat(submitted.getFirst().getApplicant().name()).isEqualTo("Charlie");

        List<LoanApplication> passed = repository.findByStatus(LoanStatus.PRELIMINARY_PASSED);
        assertThat(passed).hasSize(1);
    }

    @Test
    void findByApplicantName_returnsMatchingApplications() {
        var applicant = new Applicant("Diana", "D123", Money.twd(2_000_000), "MegaCorp");
        LoanApplication app = LoanApplication.submit(applicant, Money.twd(1_000_000),
                new LoanTerm(20, new BigDecimal("2.5")));
        repository.save(app);

        List<LoanApplication> found = repository.findByApplicantName("Diana");
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getApplicant().nationalId()).isEqualTo("D123");
    }
}
