package com.mongodb.course.m10.bdd;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.insurance.application.ClaimService;
import com.mongodb.course.m10.insurance.domain.model.Claim;
import com.mongodb.course.m10.insurance.domain.model.ClaimItem;
import com.mongodb.course.m10.insurance.domain.model.ClaimantReference;
import com.mongodb.course.m10.insurance.domain.model.PolicyReference;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ClaimSteps {

    @Autowired
    private ClaimService claimService;

    @Autowired
    private MongoTemplate mongoTemplate;

    private String policyId;
    private int coverage;
    private int deductible;
    private Claim currentClaim;
    private Exception lastException;

    @Before
    public void setUpClaims() {
        mongoTemplate.dropCollection("m10_claims");
        currentClaim = null;
        lastException = null;
    }

    @Given("保單 {string} 保額 {int} 元自負額 {int} 元")
    public void policySetup(String policyId, int coverage, int deductible) {
        this.policyId = policyId;
        this.coverage = coverage;
        this.deductible = deductible;
    }

    @When("提出理賠項目清單總額 {int} 元")
    public void fileClaim(int totalAmount) {
        try {
            var items = List.of(new ClaimItem("理賠項目", Money.twd(totalAmount), "一般"));
            currentClaim = claimService.fileClaim(
                    new PolicyReference(policyId),
                    new ClaimantReference("CLM-AUTO"),
                    items,
                    Money.twd(coverage),
                    Money.twd(deductible));
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Given("已提出理賠總額 {int} 元")
    public void alreadyFiledClaim(int totalAmount) {
        fileClaim(totalAmount);
    }

    @When("審核人員核定金額 {int} 元")
    public void assessClaim(int amount) {
        try {
            currentClaim = claimService.assessClaim(
                    currentClaim.getId(), "審核員", Money.twd(amount), "審核完成");
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Then("理賠狀態為 {string}")
    public void verifyClaimStatus(String status) {
        assertThat(currentClaim.getStatus().name()).isEqualTo(status);
    }

    @Then("理賠申請失敗並回傳超過保額錯誤")
    public void claimFilingFailedExceedsCoverage() {
        assertThat(lastException)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds policy coverage");
    }

    @And("核定金額為 {int} 元")
    public void verifyApprovedAmount(int amount) {
        assertThat(currentClaim.getAssessment().approvedAmount().amount())
                .isEqualByComparingTo(BigDecimal.valueOf(amount));
    }

    @Then("審核失敗並回傳金額超出限制錯誤")
    public void assessmentFailedExceedsLimit() {
        assertThat(lastException)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds");
    }
}
