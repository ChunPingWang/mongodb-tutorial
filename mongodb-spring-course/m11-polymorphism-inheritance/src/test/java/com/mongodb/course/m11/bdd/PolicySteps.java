package com.mongodb.course.m11.bdd;

import com.mongodb.course.m11.insurance.model.AutoPolicy;
import com.mongodb.course.m11.insurance.model.HealthPolicy;
import com.mongodb.course.m11.insurance.model.LifePolicy;
import com.mongodb.course.m11.insurance.model.Policy;
import com.mongodb.course.m11.insurance.service.PolicyService;
import com.mongodb.course.m11.insurance.service.PremiumCalculator;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicySteps {

    @Autowired
    private PolicyService policyService;

    @Autowired
    private PremiumCalculator premiumCalculator;

    @Autowired
    private MongoTemplate mongoTemplate;

    private List<Policy> policyResults;
    private Policy currentPolicy;
    private BigDecimal calculatedPremium;

    @Before
    public void setUp() {
        mongoTemplate.dropCollection("m11_policies");
        policyResults = null;
        currentPolicy = null;
        calculatedPremium = null;
    }

    @When("建立車險保單 {string} 持有人 {string} 基本保費 {int}")
    public void createAutoPolicy(String policyNumber, String holder, int premium) {
        policyService.save(new AutoPolicy(null, policyNumber, holder,
                BigDecimal.valueOf(premium), "sedan", 30));
    }

    @When("建立壽險保單 {string} 持有人 {string} 基本保費 {int}")
    public void createLifePolicy(String policyNumber, String holder, int premium) {
        policyService.save(new LifePolicy(null, policyNumber, holder,
                BigDecimal.valueOf(premium), 40, 20, BigDecimal.valueOf(3000000)));
    }

    @When("建立健康險保單 {string} 持有人 {string} 基本保費 {int}")
    public void createHealthPolicy(String policyNumber, String holder, int premium) {
        policyService.save(new HealthPolicy(null, policyNumber, holder,
                BigDecimal.valueOf(premium), false, false, 5000));
    }

    @Then("統一查詢返回 {int} 張保單")
    public void verifyPolicyCount(int expected) {
        policyResults = policyService.findAll();
        assertThat(policyResults).hasSize(expected);
    }

    @Then("篩選車險返回 {int} 張")
    public void verifyAutoCount(int expected) {
        List<AutoPolicy> auto = policyService.findByType(AutoPolicy.class);
        assertThat(auto).hasSize(expected);
    }

    @Given("車險保單基本保費 {int} 駕駛年齡 {int} 車型 {string}")
    public void createAutoPolicyForCalculation(int premium, int age, String vehicleType) {
        currentPolicy = new AutoPolicy(null, "CALC-A", "測試",
                BigDecimal.valueOf(premium), vehicleType, age);
    }

    @Given("壽險保單基本保費 {int} 被保人年齡 {int} 保期 {int} 年")
    public void createLifePolicyForCalculation(int premium, int age, int termYears) {
        currentPolicy = new LifePolicy(null, "CALC-L", "測試",
                BigDecimal.valueOf(premium), age, termYears, BigDecimal.valueOf(3000000));
    }

    @Given("健康險保單基本保費 {int} 含牙科 含眼科")
    public void createHealthPolicyWithOptions(int premium) {
        currentPolicy = new HealthPolicy(null, "CALC-H", "測試",
                BigDecimal.valueOf(premium), true, true, 5000);
    }

    @When("計算保費")
    public void calculatePremium() {
        calculatedPremium = premiumCalculator.calculatePremium(currentPolicy);
    }

    @Then("保費為 {int}")
    public void verifyPremium(int expected) {
        assertThat(calculatedPremium).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }
}
