package com.mongodb.course.m09;

import com.mongodb.course.m09.insurance.BillingSchedule;
import com.mongodb.course.m09.insurance.CustomerStatus;
import com.mongodb.course.m09.insurance.InsuranceCustomer;
import com.mongodb.course.m09.insurance.InsurancePolicy;
import com.mongodb.course.m09.insurance.PolicyType;
import com.mongodb.course.m09.insurance.UnderwritingException;
import com.mongodb.course.m09.service.UnderwritingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class UnderwritingRollbackTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    UnderwritingService underwritingService;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), InsuranceCustomer.class);
        mongoTemplate.remove(new Query(), InsurancePolicy.class);
        mongoTemplate.remove(new Query(), BillingSchedule.class);
    }

    @Test
    void suspendedCustomer_rollsBackAll() {
        mongoTemplate.insert(new InsuranceCustomer("C002", "Bob", "bob@example.com", CustomerStatus.SUSPENDED));

        InsurancePolicy policy = new InsurancePolicy("POL-001", "C002", "Bob",
                PolicyType.AUTO, new BigDecimal("800"), new BigDecimal("300000"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        BillingSchedule billing = new BillingSchedule("POL-001", "C002", new BigDecimal("800"),
                LocalDate.now(), LocalDate.now().plusYears(1));

        assertThatThrownBy(() -> underwritingService.underwrite("C002", policy, billing))
                .isInstanceOf(UnderwritingException.class)
                .hasMessageContaining("suspended");

        long policyCount = mongoTemplate.count(new Query(), InsurancePolicy.class);
        long billingCount = mongoTemplate.count(new Query(), BillingSchedule.class);
        assertThat(policyCount).isZero();
        assertThat(billingCount).isZero();
    }

    @Test
    void customerNotFound_rollsBackAll() {
        InsurancePolicy policy = new InsurancePolicy("POL-001", "C999", "Unknown",
                PolicyType.LIFE, new BigDecimal("1200"), new BigDecimal("1000000"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        BillingSchedule billing = new BillingSchedule("POL-001", "C999", new BigDecimal("1200"),
                LocalDate.now(), LocalDate.now().plusYears(1));

        assertThatThrownBy(() -> underwritingService.underwrite("C999", policy, billing))
                .isInstanceOf(UnderwritingException.class)
                .hasMessageContaining("not found");

        long policyCount = mongoTemplate.count(new Query(), InsurancePolicy.class);
        long billingCount = mongoTemplate.count(new Query(), BillingSchedule.class);
        assertThat(policyCount).isZero();
        assertThat(billingCount).isZero();
    }

    @Test
    void rollback_customerPolicyCountUnchanged() {
        InsuranceCustomer customer = new InsuranceCustomer("C002", "Bob", "bob@example.com", CustomerStatus.SUSPENDED);
        mongoTemplate.insert(customer);

        InsurancePolicy policy = new InsurancePolicy("POL-001", "C002", "Bob",
                PolicyType.HOME, new BigDecimal("900"), new BigDecimal("500000"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        BillingSchedule billing = new BillingSchedule("POL-001", "C002", new BigDecimal("900"),
                LocalDate.now(), LocalDate.now().plusYears(1));

        assertThatThrownBy(() -> underwritingService.underwrite("C002", policy, billing))
                .isInstanceOf(UnderwritingException.class);

        InsuranceCustomer reloaded = mongoTemplate.findOne(
                Query.query(Criteria.where("customerNumber").is("C002")),
                InsuranceCustomer.class
        );
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getPolicyCount()).isZero();
        assertThat(reloaded.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
    }

    @Test
    void partialFailure_noOrphanedDocuments() {
        mongoTemplate.insert(new InsuranceCustomer("C002", "Bob", "bob@example.com", CustomerStatus.SUSPENDED));

        InsurancePolicy policy = new InsurancePolicy("POL-001", "C002", "Bob",
                PolicyType.HEALTH, new BigDecimal("700"), new BigDecimal("400000"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        BillingSchedule billing = new BillingSchedule("POL-001", "C002", new BigDecimal("700"),
                LocalDate.now(), LocalDate.now().plusYears(1));

        assertThatThrownBy(() -> underwritingService.underwrite("C002", policy, billing))
                .isInstanceOf(UnderwritingException.class);

        // No orphaned policy documents
        long policyCount = mongoTemplate.count(
                Query.query(Criteria.where("policyNumber").is("POL-001")),
                InsurancePolicy.class
        );
        assertThat(policyCount).isZero();

        // No orphaned billing schedule documents
        long billingCount = mongoTemplate.count(
                Query.query(Criteria.where("policyNumber").is("POL-001")),
                BillingSchedule.class
        );
        assertThat(billingCount).isZero();
    }
}
