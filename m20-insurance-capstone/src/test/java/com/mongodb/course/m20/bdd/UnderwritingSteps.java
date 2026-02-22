package com.mongodb.course.m20.bdd;

import com.mongodb.course.m20.policy.AutoPolicy;
import com.mongodb.course.m20.policy.HealthPolicy;
import com.mongodb.course.m20.policy.LifePolicy;
import com.mongodb.course.m20.policy.PolicyService;
import com.mongodb.course.m20.underwriting.model.PolicyApplicant;
import com.mongodb.course.m20.underwriting.model.UnderwritingApplication;
import com.mongodb.course.m20.underwriting.service.UnderwritingService;
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

public class UnderwritingSteps {

    @Autowired private UnderwritingService underwritingService;
    @Autowired private PolicyService policyService;
    @Autowired private MongoTemplate mongoTemplate;

    private UnderwritingApplication lastApplication;

    @Given("類別 {string} 已有 {int} 件已付款理賠")
    public void prepopulatePaidClaims(String category, int paidCount) {
        var statsQuery = Query.query(Criteria.where("_id").is(category));
        var statsUpdate = new Update()
                .set("totalClaims", paidCount)
                .set("filedCount", paidCount)
                .set("approvedCount", paidCount)
                .set("rejectedCount", 0)
                .set("paidCount", paidCount)
                .set("investigatedCount", paidCount)
                .set("assessedCount", paidCount)
                .set("totalClaimedAmount", new Decimal128(new BigDecimal(paidCount * 50000)))
                .set("totalApprovedAmount", new Decimal128(new BigDecimal(paidCount * 40000)))
                .set("totalPaidAmount", new Decimal128(new BigDecimal(paidCount * 40000)));
        mongoTemplate.upsert(statsQuery, statsUpdate, "m20_claim_statistics");
    }

    @When("申請人 {string} 年齡 {int} 職業 {string} 申請 {string} 保單保額 {int} 元")
    public void submitApplication(String name, int age, String occupation,
                                   String policyType, int coverage) {
        var applicant = new PolicyApplicant(name, age, occupation);
        lastApplication = underwritingService.submit(applicant, policyType, new BigDecimal(coverage));
    }

    @When("執行核保審核")
    public void performReview() {
        lastApplication = underwritingService.review(lastApplication);
    }

    @Then("核保申請狀態為 {string}")
    public void verifyApplicationStatus(String expectedStatus) {
        assertThat(lastApplication.getStatus().name()).isEqualTo(expectedStatus);
    }

    @Then("保單集合中存在保單類型 {string} 持有人 {string}")
    public void verifyPolicyCreated(String policyType, String holderName) {
        var policies = switch (policyType) {
            case "AUTO" -> policyService.findByType(AutoPolicy.class);
            case "HEALTH" -> policyService.findByType(HealthPolicy.class);
            case "LIFE" -> policyService.findByType(LifePolicy.class);
            default -> throw new IllegalArgumentException("Unknown type: " + policyType);
        };
        assertThat(policies).anyMatch(p -> p.getHolderName().equals(holderName));
    }
}
