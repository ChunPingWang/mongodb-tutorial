package com.mongodb.course.m20.bdd;

import com.mongodb.course.m20.observability.SlowQueryDetector;
import com.mongodb.course.m20.projection.ClaimQueryService;
import com.mongodb.course.m20.projection.readmodel.ClaimDashboardDocument;
import com.mongodb.course.m20.projection.readmodel.ClaimStatisticsDocument;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class InsuranceDashboardSteps {

    @Autowired private ClaimQueryService claimQueryService;
    @Autowired private SlowQueryDetector slowQueryDetector;

    private ClaimStatisticsDocument lastStatistics;
    private ClaimDashboardDocument lastDashboard;

    @Given("慢查詢偵測器門檻值設定為 {int} 毫秒")
    public void setSlowQueryThreshold(int thresholdMs) {
        slowQueryDetector.setThresholdMs(thresholdMs);
    }

    @When("查詢類別 {string} 的理賠統計")
    public void queryStatistics(String category) {
        lastStatistics = claimQueryService.findStatisticsByCategory(category).orElse(null);
    }

    @When("查詢理賠儀表板 {string}")
    public void queryDashboard(String claimId) {
        lastDashboard = claimQueryService.findDashboardByClaimId(claimId).orElse(null);
    }

    @Then("該類別總理賠件數為 {int}")
    public void verifyTotalClaims(int expected) {
        assertThat(lastStatistics).isNotNull();
        assertThat(lastStatistics.totalClaims()).isEqualTo(expected);
    }

    @Then("該類別總理賠金額為 {int} 元")
    public void verifyTotalClaimedAmount(int expected) {
        assertThat(lastStatistics).isNotNull();
        assertThat(lastStatistics.totalClaimedAmount())
                .isEqualByComparingTo(new BigDecimal(expected));
    }

    @Then("時間軸包含 {int} 筆紀錄")
    public void verifyTimelineSize(int expected) {
        assertThat(lastDashboard).isNotNull();
        assertThat(lastDashboard.timeline()).hasSize(expected);
    }

    @Then("慢查詢偵測器應捕獲至少 {int} 筆紀錄")
    public void verifySlowQueryCaptured(int minCount) {
        assertThat(slowQueryDetector.getCapturedCount()).isGreaterThanOrEqualTo(minCount);
    }
}
