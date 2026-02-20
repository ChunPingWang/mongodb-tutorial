package com.mongodb.course.m09;

import com.mongodb.course.m09.insurance.BillingSchedule;
import com.mongodb.course.m09.insurance.CustomerStatus;
import com.mongodb.course.m09.insurance.InsuranceCustomer;
import com.mongodb.course.m09.insurance.InsurancePolicy;
import com.mongodb.course.m09.insurance.PolicyType;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class UnderwritingServiceTest {

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
    void successfulUnderwriting_createsPolicy() {
        mongoTemplate.insert(new InsuranceCustomer("C001", "Alice", "alice@example.com", CustomerStatus.PROSPECT));

        InsurancePolicy policy = new InsurancePolicy("POL-001", "C001", "Alice",
                PolicyType.LIFE, new BigDecimal("1200"), new BigDecimal("1000000"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        BillingSchedule billing = new BillingSchedule("POL-001", "C001", new BigDecimal("1200"),
                LocalDate.now(), LocalDate.now().plusYears(1));

        InsurancePolicy saved = underwritingService.underwrite("C001", policy, billing);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus().name()).isEqualTo("ACTIVE");
        assertThat(saved.getPolicyNumber()).isEqualTo("POL-001");
    }

    @Test
    void successfulUnderwriting_createsBillingSchedule() {
        mongoTemplate.insert(new InsuranceCustomer("C001", "Alice", "alice@example.com", CustomerStatus.PROSPECT));

        InsurancePolicy policy = new InsurancePolicy("POL-001", "C001", "Alice",
                PolicyType.LIFE, new BigDecimal("1200"), new BigDecimal("1000000"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        BillingSchedule billing = new BillingSchedule("POL-001", "C001", new BigDecimal("1200"),
                LocalDate.now(), LocalDate.now().plusYears(1));

        underwritingService.underwrite("C001", policy, billing);

        List<BillingSchedule> schedules = mongoTemplate.find(
                Query.query(Criteria.where("policyNumber").is("POL-001")),
                BillingSchedule.class
        );
        assertThat(schedules).hasSize(1);
        assertThat(schedules.getFirst().getMonthlyAmount()).isEqualByComparingTo(new BigDecimal("1200"));
    }

    @Test
    void successfulUnderwriting_updatesCustomer() {
        mongoTemplate.insert(new InsuranceCustomer("C001", "Alice", "alice@example.com", CustomerStatus.PROSPECT));

        InsurancePolicy policy = new InsurancePolicy("POL-001", "C001", "Alice",
                PolicyType.LIFE, new BigDecimal("1200"), new BigDecimal("1000000"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        BillingSchedule billing = new BillingSchedule("POL-001", "C001", new BigDecimal("1200"),
                LocalDate.now(), LocalDate.now().plusYears(1));

        underwritingService.underwrite("C001", policy, billing);

        InsuranceCustomer customer = mongoTemplate.findOne(
                Query.query(Criteria.where("customerNumber").is("C001")),
                InsuranceCustomer.class
        );
        assertThat(customer).isNotNull();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.getPolicyCount()).isEqualTo(1);
    }

    @Test
    void successfulUnderwriting_allThreeCollectionsConsistent() {
        mongoTemplate.insert(new InsuranceCustomer("C001", "Alice", "alice@example.com", CustomerStatus.PROSPECT));

        InsurancePolicy policy = new InsurancePolicy("POL-001", "C001", "Alice",
                PolicyType.HEALTH, new BigDecimal("800"), new BigDecimal("500000"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        BillingSchedule billing = new BillingSchedule("POL-001", "C001", new BigDecimal("800"),
                LocalDate.now(), LocalDate.now().plusYears(1));

        underwritingService.underwrite("C001", policy, billing);

        long policyCount = mongoTemplate.count(new Query(), InsurancePolicy.class);
        long billingCount = mongoTemplate.count(new Query(), BillingSchedule.class);
        InsuranceCustomer customer = mongoTemplate.findOne(
                Query.query(Criteria.where("customerNumber").is("C001")),
                InsuranceCustomer.class
        );

        assertThat(policyCount).isEqualTo(1);
        assertThat(billingCount).isEqualTo(1);
        assertThat(customer).isNotNull();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void multipleUnderwritings_incrementsPolicyCount() {
        mongoTemplate.insert(new InsuranceCustomer("C001", "Alice", "alice@example.com", CustomerStatus.PROSPECT));

        InsurancePolicy policy1 = new InsurancePolicy("POL-001", "C001", "Alice",
                PolicyType.LIFE, new BigDecimal("1200"), new BigDecimal("1000000"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        BillingSchedule billing1 = new BillingSchedule("POL-001", "C001", new BigDecimal("1200"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        underwritingService.underwrite("C001", policy1, billing1);

        InsurancePolicy policy2 = new InsurancePolicy("POL-002", "C001", "Alice",
                PolicyType.AUTO, new BigDecimal("600"), new BigDecimal("200000"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        BillingSchedule billing2 = new BillingSchedule("POL-002", "C001", new BigDecimal("600"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        underwritingService.underwrite("C001", policy2, billing2);

        InsuranceCustomer customer = mongoTemplate.findOne(
                Query.query(Criteria.where("customerNumber").is("C001")),
                InsuranceCustomer.class
        );
        assertThat(customer).isNotNull();
        assertThat(customer.getPolicyCount()).isEqualTo(2);
    }

    @Test
    void underwritingWithDifferentPolicyTypes_works() {
        mongoTemplate.insert(new InsuranceCustomer("C001", "Alice", "alice@example.com", CustomerStatus.PROSPECT));

        InsurancePolicy lifePolicy = new InsurancePolicy("POL-001", "C001", "Alice",
                PolicyType.LIFE, new BigDecimal("1500"), new BigDecimal("2000000"),
                LocalDate.now(), LocalDate.now().plusYears(20));
        BillingSchedule lifeBilling = new BillingSchedule("POL-001", "C001", new BigDecimal("1500"),
                LocalDate.now(), LocalDate.now().plusYears(20));
        underwritingService.underwrite("C001", lifePolicy, lifeBilling);

        InsurancePolicy autoPolicy = new InsurancePolicy("POL-002", "C001", "Alice",
                PolicyType.AUTO, new BigDecimal("500"), new BigDecimal("300000"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        BillingSchedule autoBilling = new BillingSchedule("POL-002", "C001", new BigDecimal("500"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        underwritingService.underwrite("C001", autoPolicy, autoBilling);

        List<InsurancePolicy> policies = mongoTemplate.find(
                Query.query(Criteria.where("customerNumber").is("C001")),
                InsurancePolicy.class
        );
        assertThat(policies).hasSize(2);
        assertThat(policies).extracting(InsurancePolicy::getPolicyType)
                .containsExactlyInAnyOrder(PolicyType.LIFE, PolicyType.AUTO);
    }
}
