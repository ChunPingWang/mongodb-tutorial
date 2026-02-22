package com.mongodb.course.m10.bdd;

import com.mongodb.course.m10.banking.application.LoanApplicationService;
import com.mongodb.course.m10.banking.domain.model.Applicant;
import com.mongodb.course.m10.banking.domain.model.LoanApplication;
import com.mongodb.course.m10.banking.domain.model.LoanTerm;
import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.banking.infrastructure.persistence.LoanApplicationDocument;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class LoanApplicationSteps {

    @Autowired
    private LoanApplicationService loanService;

    @Autowired
    private MongoTemplate mongoTemplate;

    private String applicantName;
    private int applicantIncome;
    private LoanApplication currentApplication;
    private Exception lastException;

    @Before
    public void setUp() {
        mongoTemplate.dropCollection("m10_loan_applications");
        currentApplication = null;
        lastException = null;
    }

    @Given("申請人 {string} 年收入 {int} 元")
    public void applicantWithIncome(String name, int income) {
        applicantName = name;
        applicantIncome = income;
    }

    @When("提交貸款金額 {int} 元期限 {int} 年利率 {double}%")
    public void submitLoan(int amount, int years, double rate) {
        var applicant = new Applicant(applicantName, "ID-" + applicantName,
                Money.twd(applicantIncome), "公司");
        currentApplication = loanService.submitApplication(
                applicant, Money.twd(amount),
                new LoanTerm(years, BigDecimal.valueOf(rate)));
    }

    @Given("已提交貸款金額 {int} 元期限 {int} 年利率 {double}%")
    public void alreadySubmittedLoan(int amount, int years, double rate) {
        submitLoan(amount, years, rate);
    }

    @When("執行自動初審")
    public void performPreliminaryReview() {
        try {
            currentApplication = loanService.performPreliminaryReview(currentApplication.getId());
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Given("已提交並完成初審的貸款申請")
    public void alreadyReviewedApplication() {
        submitLoan(1_000_000, 20, 2.5);
        currentApplication = loanService.performPreliminaryReview(currentApplication.getId());
    }

    @When("再次執行自動初審")
    public void performReviewAgain() {
        try {
            currentApplication = loanService.performPreliminaryReview(currentApplication.getId());
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Then("貸款申請狀態為 {string}")
    public void verifyLoanStatus(String status) {
        assertThat(currentApplication.getStatus().name()).isEqualTo(status);
    }

    @And("產生 {string} 領域事件")
    public void verifyDomainEvent(String eventType) {
        var doc = mongoTemplate.findById(currentApplication.getId(), LoanApplicationDocument.class);
        assertThat(doc).isNotNull();
        assertThat(doc.getDomainEvents())
                .extracting(m -> m.get("type"))
                .contains(eventType);
    }

    @Then("操作失敗並回傳狀態錯誤")
    public void operationFailedWithStatusError() {
        assertThat(lastException).isInstanceOf(IllegalStateException.class);
    }
}
