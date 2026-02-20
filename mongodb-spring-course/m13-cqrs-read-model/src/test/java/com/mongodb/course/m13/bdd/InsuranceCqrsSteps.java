package com.mongodb.course.m13.bdd;

import com.mongodb.course.m13.insurance.command.ClaimCommandService;
import com.mongodb.course.m13.insurance.query.ClaimQueryService;
import com.mongodb.course.m13.insurance.readmodel.ClaimDashboardDocument;
import com.mongodb.course.m13.insurance.readmodel.ClaimStatisticsDocument;
import com.mongodb.course.m13.projection.ProjectionRebuildService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InsuranceCqrsSteps {

    @Autowired
    private ClaimCommandService commandService;

    @Autowired
    private ClaimQueryService queryService;

    @Autowired
    private ProjectionRebuildService rebuildService;

    @Autowired
    private MongoTemplate mongoTemplate;

    private List<ClaimDashboardDocument> claimQueryResults;
    private ClaimStatisticsDocument categoryStatistics;

    @Before
    public void cleanup() {
        mongoTemplate.remove(new Query(), "m13_claim_events");
        mongoTemplate.remove(new Query(), "m13_claim_dashboards");
        mongoTemplate.remove(new Query(), "m13_claim_statistics");
    }

    @When("提出理賠 {string} 保單 {string} 理賠人 {string} 金額 {int} 類別 {string}")
    public void fileClaim(String claimId, String policyId, String claimant, int amount, String category) {
        commandService.fileClaim(claimId, policyId, claimant, new BigDecimal(amount), category);
    }

    @Given("已提出理賠 {string} 金額 {int} 類別 {string}")
    public void existingClaim(String claimId, int amount, String category) {
        commandService.fileClaim(claimId, "POL-DEFAULT", "TestClaimant", new BigDecimal(amount), category);
    }

    @When("調查理賠 {string}")
    public void investigateClaim(String claimId) {
        commandService.investigate(claimId, "Inspector", "Investigation complete");
    }

    @Given("已調查理賠 {string}")
    public void existingInvestigatedClaim(String claimId) {
        commandService.investigate(claimId, "Inspector", "Investigation complete");
    }

    @When("評估理賠 {string} 金額 {int}")
    public void assessClaim(String claimId, int amount) {
        commandService.assess(claimId, "Assessor", new BigDecimal(amount), "Assessment notes");
    }

    @When("核准理賠 {string} 金額 {int}")
    public void approveClaim(String claimId, int amount) {
        commandService.approve(claimId, new BigDecimal(amount), "Approver");
    }

    @Given("理賠 {string} 已調查並拒絕")
    public void investigateAndReject(String claimId) {
        commandService.investigate(claimId, "Inspector", "Findings");
        commandService.reject(claimId, "Insufficient evidence", "Reviewer");
    }

    @Then("理賠儀表板 {string} 狀態為 {string}")
    public void verifyDashboardStatus(String claimId, String expectedStatus) {
        var dashboard = queryService.getClaimDashboard(claimId).orElseThrow();
        assertThat(dashboard.currentStatus()).isEqualTo(expectedStatus);
    }

    @Then("理賠儀表板 {string} 時間線包含 {int} 筆記錄")
    public void verifyDashboardTimelineCount(String claimId, int expectedCount) {
        var dashboard = queryService.getClaimDashboard(claimId).orElseThrow();
        assertThat(dashboard.timeline()).hasSize(expectedCount);
    }

    @When("查詢狀態為 {string} 的理賠")
    public void queryClaimsByStatus(String status) {
        claimQueryResults = queryService.getClaimsByStatus(status);
    }

    @Then("查詢結果包含 {int} 筆理賠")
    public void verifyClaimQueryResultCount(int expectedCount) {
        assertThat(claimQueryResults).hasSize(expectedCount);
    }

    @When("查詢 {string} 類別統計")
    public void queryStatisticsByCategory(String category) {
        categoryStatistics = queryService.getStatisticsByCategory(category).orElseThrow();
    }

    @Then("該類別理賠總數為 {int}")
    public void verifyCategoryTotalClaims(int expectedCount) {
        assertThat(categoryStatistics.totalClaims()).isEqualTo(expectedCount);
    }

    @Then("該類別理賠總金額為 {int}")
    public void verifyCategoryTotalClaimedAmount(int expectedAmount) {
        assertThat(categoryStatistics.totalClaimedAmount())
                .isEqualByComparingTo(new BigDecimal(expectedAmount));
    }

    @When("清除保險讀取模型")
    public void clearInsuranceReadModels() {
        rebuildService.clearInsuranceReadModels();
    }

    @Then("理賠儀表板 {string} 不存在")
    public void verifyDashboardNotExists(String claimId) {
        var dashboard = queryService.getClaimDashboard(claimId);
        assertThat(dashboard).isEmpty();
    }

    @When("重建保險讀取模型")
    public void rebuildInsuranceProjections() {
        rebuildService.rebuildInsuranceProjections();
    }

    @Then("理賠統計 {string} 總數為 {int}")
    public void verifyStatisticsTotalClaims(String category, int expectedCount) {
        var stats = queryService.getStatisticsByCategory(category).orElseThrow();
        assertThat(stats.totalClaims()).isEqualTo(expectedCount);
    }
}
