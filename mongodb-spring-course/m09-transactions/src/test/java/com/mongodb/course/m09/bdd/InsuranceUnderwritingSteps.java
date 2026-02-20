package com.mongodb.course.m09.bdd;

import com.mongodb.course.m09.insurance.BillingSchedule;
import com.mongodb.course.m09.insurance.CustomerStatus;
import com.mongodb.course.m09.insurance.InsuranceCustomer;
import com.mongodb.course.m09.insurance.InsurancePolicy;
import com.mongodb.course.m09.insurance.PolicyType;
import com.mongodb.course.m09.insurance.UnderwritingException;
import com.mongodb.course.m09.service.UnderwritingService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class InsuranceUnderwritingSteps {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UnderwritingService underwritingService;

    private String currentCustomerNumber;
    private Exception lastException;

    @Before
    public void setUp() {
        mongoTemplate.remove(new Query(), InsuranceCustomer.class);
        mongoTemplate.remove(new Query(), InsurancePolicy.class);
        mongoTemplate.remove(new Query(), BillingSchedule.class);
        lastException = null;
        currentCustomerNumber = null;
    }

    @Given("保險客戶 {string} 名稱 {string} 狀態為 {string}")
    public void createCustomer(String customerNumber, String name, String statusStr) {
        CustomerStatus status = CustomerStatus.valueOf(statusStr);
        mongoTemplate.insert(new InsuranceCustomer(customerNumber, name, name.toLowerCase() + "@example.com", status));
        currentCustomerNumber = customerNumber;
    }

    @When("進行核保建立保單類型 {string} 保費 {int} 元")
    public void underwrite(String policyTypeStr, int premium) {
        PolicyType policyType = PolicyType.valueOf(policyTypeStr);
        String policyNumber = "POL-BDD-001";

        InsurancePolicy policy = new InsurancePolicy(policyNumber, currentCustomerNumber, "BDD-Holder",
                policyType, new BigDecimal(premium), new BigDecimal("1000000"),
                LocalDate.now(), LocalDate.now().plusYears(1));
        BillingSchedule billing = new BillingSchedule(policyNumber, currentCustomerNumber, new BigDecimal(premium),
                LocalDate.now(), LocalDate.now().plusYears(1));

        try {
            underwritingService.underwrite(currentCustomerNumber, policy, billing);
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Then("保單成功建立狀態為 {string}")
    public void policyCreatedWithStatus(String expectedStatus) {
        long count = mongoTemplate.count(new Query(), InsurancePolicy.class);
        assertThat(count).isEqualTo(1);
        InsurancePolicy policy = mongoTemplate.findOne(new Query(), InsurancePolicy.class);
        assertThat(policy).isNotNull();
        assertThat(policy.getStatus().name()).isEqualTo(expectedStatus);
    }

    @Then("收費排程成功建立每月 {int} 元")
    public void billingScheduleCreated(int expectedAmount) {
        long count = mongoTemplate.count(new Query(), BillingSchedule.class);
        assertThat(count).isEqualTo(1);
        BillingSchedule billing = mongoTemplate.findOne(new Query(), BillingSchedule.class);
        assertThat(billing).isNotNull();
        assertThat(billing.getMonthlyAmount()).isEqualByComparingTo(new BigDecimal(expectedAmount));
    }

    @Then("客戶 {string} 狀態更新為 {string}")
    public void customerStatusUpdated(String customerNumber, String expectedStatus) {
        InsuranceCustomer customer = mongoTemplate.findOne(
                Query.query(Criteria.where("customerNumber").is(customerNumber)),
                InsuranceCustomer.class
        );
        assertThat(customer).isNotNull();
        assertThat(customer.getStatus().name()).isEqualTo(expectedStatus);
    }

    @Then("核保失敗並回傳客戶停權錯誤")
    public void underwritingFailedSuspended() {
        assertThat(lastException).isInstanceOf(UnderwritingException.class);
        assertThat(lastException.getMessage()).contains("suspended");
    }

    @Then("沒有新保單被建立")
    public void noPolicyCreated() {
        long count = mongoTemplate.count(new Query(), InsurancePolicy.class);
        assertThat(count).isZero();
    }

    @Then("沒有新收費排程被建立")
    public void noBillingScheduleCreated() {
        long count = mongoTemplate.count(new Query(), BillingSchedule.class);
        assertThat(count).isZero();
    }
}
