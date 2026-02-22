package com.mongodb.course.m20.bdd;

import com.mongodb.course.m20.claim.service.ClaimCommandService;
import com.mongodb.course.m20.infrastructure.saga.SagaLogRepository;
import com.mongodb.course.m20.policy.PolicyService;
import com.mongodb.course.m20.projection.ClaimQueryService;
import com.mongodb.course.m20.settlement.ClaimSettlementSagaService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.bson.types.Decimal128;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class ClaimSettlementSteps {

    @Autowired private ClaimSettlementSagaService sagaService;
    @Autowired private ClaimCommandService claimCommandService;
    @Autowired private SagaLogRepository sagaLogRepository;
    @Autowired private PolicyService policyService;
    @Autowired private ClaimQueryService claimQueryService;
    @Autowired private MongoTemplate mongoTemplate;

    private String lastSagaId;
    private String lastClaimId;
    private String lastPolicyId;
    private BigDecimal lastAssessedAmount;

    @Given("理賠 {string} 已調查評估為 {int} 元風險 {string}")
    public void investigateAndAssess(String claimId, int assessedAmount, String fraudRisk) {
        claimCommandService.investigate(claimId, "Inspector", "Findings", fraudRisk);
        claimCommandService.assess(claimId, new BigDecimal(assessedAmount), "Assessment");
        lastClaimId = claimId;
        lastAssessedAmount = new BigDecimal(assessedAmount);
    }

    @Given("類別 {string} 已有 {int} 件理賠且核准率低於 30%")
    public void prepopulateStatistics(String category, int filedCount) {
        int approvedCount = (int) (filedCount * 0.15); // 15% approval rate
        var statsQuery = Query.query(Criteria.where("_id").is(category));
        var statsUpdate = new Update()
                .set("totalClaims", filedCount)
                .set("filedCount", filedCount)
                .set("approvedCount", approvedCount)
                .set("rejectedCount", filedCount - approvedCount)
                .set("paidCount", 0)
                .set("investigatedCount", filedCount)
                .set("assessedCount", filedCount)
                .set("totalClaimedAmount", new Decimal128(new BigDecimal(filedCount * 100000)))
                .set("totalApprovedAmount", new Decimal128(new BigDecimal(approvedCount * 80000)))
                .set("totalPaidAmount", new Decimal128(BigDecimal.ZERO));
        mongoTemplate.upsert(statsQuery, statsUpdate, "m20_claim_statistics");
    }

    @When("執行理賠結算 Saga 理賠 {string}")
    public void executeSettlementSaga(String claimId) {
        var dashboard = claimQueryService.findDashboardByClaimId(claimId).orElseThrow();
        String policyId = dashboard.policyId();
        String category = dashboard.category();
        BigDecimal assessedAmount = dashboard.assessedAmount();

        lastSagaId = sagaService.settle(claimId, policyId, category, assessedAmount);
        lastClaimId = claimId;
        lastPolicyId = policyId;
    }

    @Then("結算 Saga 狀態為 {string}")
    public void verifySagaStatus(String expectedStatus) {
        var sagaLog = sagaLogRepository.findById(lastSagaId);
        assertThat(sagaLog).isPresent();
        assertThat(sagaLog.get().status().name()).isEqualTo(expectedStatus);
    }

    @Then("保單 {string} 累計理賠金額為 {int} 元")
    public void verifyPolicyTotalClaimsPaid(String policyNumber, int expectedAmount) {
        var policy = policyService.findByPolicyNumber(policyNumber).orElseThrow();
        assertThat(policy.getTotalClaimsPaid())
                .isEqualByComparingTo(new BigDecimal(expectedAmount));
    }

    @Then("結算 Saga 日誌包含 {int} 個步驟")
    public void verifySagaStepCount(int expectedStepCount) {
        var sagaLog = sagaLogRepository.findById(lastSagaId);
        assertThat(sagaLog).isPresent();
        assertThat(sagaLog.get().steps()).hasSize(expectedStepCount);
    }
}
