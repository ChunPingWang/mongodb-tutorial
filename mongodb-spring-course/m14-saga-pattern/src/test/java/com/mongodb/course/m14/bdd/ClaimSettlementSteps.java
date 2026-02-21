package com.mongodb.course.m14.bdd;

import com.mongodb.course.m14.insurance.model.Claim;
import com.mongodb.course.m14.insurance.model.ClaimSettlementStatus;
import com.mongodb.course.m14.insurance.model.Policy;
import com.mongodb.course.m14.insurance.service.ClaimSettlementSagaService;
import com.mongodb.course.m14.saga.SagaLogRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class ClaimSettlementSteps {

    @Autowired
    private ClaimSettlementSagaService claimSettlementSagaService;

    @Autowired
    private SagaLogRepository sagaLogRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private String sagaId;

    @Given("保單 {string} 持有人 {string} 保額 {int} 已理賠 {int}")
    public void createPolicy(String policyId, String holderName, int coverageAmount, int paidClaimsTotal) {
        mongoTemplate.save(new Policy(policyId, holderName, coverageAmount, paidClaimsTotal));
    }

    @Given("理賠案 {string} 保單 {string} 理賠人 {string} 金額 {int}")
    public void createClaim(String claimId, String policyId, String claimantName, int amount) {
        mongoTemplate.save(new Claim(claimId, policyId, claimantName, amount, ClaimSettlementStatus.PENDING));
    }

    @When("執行理賠結算 {string}")
    public void settleClaim(String claimId) {
        sagaId = claimSettlementSagaService.settleClaim(claimId);
    }

    @Then("結算 Saga 狀態為 {string}")
    public void verifySettlementSagaStatus(String expectedStatus) {
        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.status().name()).isEqualTo(expectedStatus);
    }

    @Then("理賠案 {string} 結算狀態為 {string}")
    public void verifyClaimSettlementStatus(String claimId, String expectedStatus) {
        var claim = mongoTemplate.findById(claimId, Claim.class);
        assertThat(claim.settlementStatus()).isEqualTo(ClaimSettlementStatus.valueOf(expectedStatus));
    }

    @Then("保單 {string} 已理賠總額為 {int}")
    public void verifyPolicyPaidTotal(String policyId, int expectedTotal) {
        var policy = mongoTemplate.findById(policyId, Policy.class);
        assertThat(policy.paidClaimsTotal()).isEqualTo(expectedTotal);
    }

    @Then("結算 Saga 日誌包含 {int} 個步驟")
    public void verifySettlementSagaLogStepCount(int expectedCount) {
        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.steps()).hasSize(expectedCount);
    }

    @Then("結算 Saga 每個步驟狀態為 {string}")
    public void verifySettlementAllStepsStatus(String expectedStatus) {
        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.steps()).allMatch(s -> expectedStatus.equals(s.status()));
    }

    @Then("結算 Saga 日誌中 {string} 步驟狀態為 {string}")
    public void verifySettlementStepStatus(String stepName, String expectedStatus) {
        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        var step = sagaLog.steps().stream()
                .filter(s -> stepName.equals(s.stepName()))
                .findFirst()
                .orElseThrow();
        assertThat(step.status()).isEqualTo(expectedStatus);
    }
}
