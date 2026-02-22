package com.mongodb.course.m20.bdd;

import com.mongodb.course.m20.claim.service.ClaimCommandService;
import com.mongodb.course.m20.infrastructure.EventStore;
import com.mongodb.course.m20.policy.AutoPolicy;
import com.mongodb.course.m20.policy.HealthPolicy;
import com.mongodb.course.m20.policy.LifePolicy;
import com.mongodb.course.m20.policy.PolicyService;
import com.mongodb.course.m20.projection.ClaimQueryService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ClaimLifecycleSteps {

    @Autowired private ClaimCommandService claimCommandService;
    @Autowired private PolicyService policyService;
    @Autowired private EventStore eventStore;
    @Autowired private ClaimQueryService claimQueryService;

    // Track policy number → policy id mapping
    private final Map<String, String> policyIdMap = new HashMap<>();
    private String lastClaimId;

    @Given("已建立汽車保單 {string} 持有人 {string} 保額 {int} 元")
    public void createAutoPolicy(String policyNumber, String holderName, int coverage) {
        String policyId = "pid-" + policyNumber;
        var policy = new AutoPolicy(policyId, policyNumber, holderName,
                new BigDecimal(coverage).multiply(new BigDecimal("0.05")),
                new BigDecimal(coverage), "Sedan");
        policyService.save(policy);
        policyIdMap.put(policyNumber, policyId);
    }

    @Given("已建立健康保單 {string} 持有人 {string} 保額 {int} 元")
    public void createHealthPolicy(String policyNumber, String holderName, int coverage) {
        String policyId = "pid-" + policyNumber;
        var policy = new HealthPolicy(policyId, policyNumber, holderName,
                new BigDecimal(coverage).multiply(new BigDecimal("0.05")),
                new BigDecimal(coverage), "STANDARD");
        policyService.save(policy);
        policyIdMap.put(policyNumber, policyId);
    }

    @Given("已建立壽險保單 {string} 持有人 {string} 保額 {int} 元")
    public void createLifePolicy(String policyNumber, String holderName, int coverage) {
        String policyId = "pid-" + policyNumber;
        var policy = new LifePolicy(policyId, policyNumber, holderName,
                new BigDecimal(coverage).multiply(new BigDecimal("0.05")),
                new BigDecimal(coverage), "Legal Heir", 20);
        policyService.save(policy);
        policyIdMap.put(policyNumber, policyId);
    }

    @When("以保單 {string} 提出理賠 {string} 金額 {int} 元類別 {string}")
    public void fileClaim(String policyNumber, String claimId, int amount, String category) {
        String policyId = policyIdMap.get(policyNumber);
        claimCommandService.fileClaim(claimId, policyId, "Claimant",
                category, new BigDecimal(amount), "BDD claim");
        lastClaimId = claimId;
    }

    @Given("已提出理賠 {string} 保單 {string} 金額 {int} 元")
    public void fileClaimGiven(String claimId, String policyNumber, int amount) {
        String policyId = policyIdMap.get(policyNumber);
        // Determine category from policy type
        var policy = policyService.findById(policyId).orElseThrow();
        String category = switch (policy) {
            case AutoPolicy a -> "AUTO";
            case HealthPolicy h -> "HEALTH";
            case LifePolicy l -> "LIFE";
        };
        claimCommandService.fileClaim(claimId, policyId, policy.getHolderName(),
                category, new BigDecimal(amount), "BDD claim");
        lastClaimId = claimId;
    }

    @When("理賠 {string} 完成調查結果為 {string} 風險")
    public void investigate(String claimId, String fraudRisk) {
        claimCommandService.investigate(claimId, "BDD Inspector", "Investigation findings", fraudRisk);
        lastClaimId = claimId;
    }

    @When("理賠 {string} 評估金額為 {int} 元")
    public void assess(String claimId, int amount) {
        claimCommandService.assess(claimId, new BigDecimal(amount), "BDD assessment");
        lastClaimId = claimId;
    }

    @When("核准理賠 {string} 金額 {int} 元")
    public void approve(String claimId, int amount) {
        claimCommandService.approve(claimId, new BigDecimal(amount));
        lastClaimId = claimId;
    }

    @When("支付理賠 {string} 金額 {int} 元參考號 {string}")
    public void pay(String claimId, int amount, String paymentRef) {
        claimCommandService.pay(claimId, new BigDecimal(amount), paymentRef);
        lastClaimId = claimId;
    }

    @When("拒絕理賠 {string} 原因 {string}")
    public void reject(String claimId, String reason) {
        claimCommandService.reject(claimId, reason);
        lastClaimId = claimId;
    }

    @Then("理賠 {string} 狀態為 {string}")
    public void verifyClaimStatus(String claimId, String expectedStatus) {
        var claim = claimCommandService.loadClaim(claimId);
        assertThat(claim.getStatus().name()).isEqualTo(expectedStatus);
    }

    @Then("理賠事件數量為 {int}")
    public void verifyEventCount(int expectedCount) {
        long count = eventStore.countEvents(lastClaimId, "m20_claim_events");
        assertThat(count).isEqualTo(expectedCount);
    }

    @Then("理賠儀表板 {string} 評估金額為 {int} 元")
    public void verifyDashboardAssessedAmount(String claimId, int expectedAmount) {
        var dashboard = claimQueryService.findDashboardByClaimId(claimId);
        assertThat(dashboard).isPresent();
        assertThat(dashboard.get().assessedAmount())
                .isEqualByComparingTo(new BigDecimal(expectedAmount));
    }
}
