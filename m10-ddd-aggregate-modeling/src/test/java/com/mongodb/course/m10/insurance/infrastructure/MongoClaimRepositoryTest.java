package com.mongodb.course.m10.insurance.infrastructure;

import com.mongodb.course.m10.SharedContainersConfig;
import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.insurance.domain.model.*;
import com.mongodb.course.m10.insurance.domain.port.ClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class MongoClaimRepositoryTest {

    @Autowired
    private ClaimRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection("m10_claims");
    }

    @Test
    void save_persistsClaimWithEmbeddedItems() {
        var items = List.of(
                new ClaimItem("Hospital", Money.twd(100_000), "Medical"),
                new ClaimItem("Surgery", Money.twd(50_000), "Medical")
        );
        Claim claim = Claim.file(
                new PolicyReference("POL-001"),
                new ClaimantReference("CLM-001"),
                items, Money.twd(500_000), Money.twd(10_000));

        Claim saved = repository.save(claim);
        assertThat(saved.getId()).isNotNull();

        Optional<Claim> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(2);
        assertThat(found.get().getTotalClaimedAmount().amount())
                .isEqualByComparingTo("150000");
    }

    @Test
    void save_persistsClaimWithAssessment() {
        var items = List.of(
                new ClaimItem("Treatment", Money.twd(200_000), "Medical")
        );
        Claim claim = Claim.file(
                new PolicyReference("POL-002"),
                new ClaimantReference("CLM-002"),
                items, Money.twd(500_000), Money.twd(10_000));
        repository.save(claim);

        claim.assess("Dr. Smith", Money.twd(190_000), "Approved");
        repository.save(claim);

        Optional<Claim> found = repository.findById(claim.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(found.get().getAssessment().approvedAmount().amount())
                .isEqualByComparingTo("190000");
    }

    @Test
    void findById_returnsFullAggregate() {
        var items = List.of(
                new ClaimItem("Consultation", Money.twd(5_000), "Medical"),
                new ClaimItem("Medicine", Money.twd(3_000), "Pharmacy")
        );
        Claim claim = Claim.file(
                new PolicyReference("POL-003"),
                new ClaimantReference("CLM-003"),
                items, Money.twd(500_000), Money.twd(10_000));
        repository.save(claim);

        Optional<Claim> found = repository.findById(claim.getId());
        assertThat(found).isPresent();
        Claim loaded = found.get();
        assertThat(loaded.getPolicyRef().policyId()).isEqualTo("POL-003");
        assertThat(loaded.getClaimantRef().claimantId()).isEqualTo("CLM-003");
        assertThat(loaded.getDeductible().amount()).isEqualByComparingTo("10000");
        assertThat(loaded.getPolicyCoverage().amount()).isEqualByComparingTo("500000");
    }
}
