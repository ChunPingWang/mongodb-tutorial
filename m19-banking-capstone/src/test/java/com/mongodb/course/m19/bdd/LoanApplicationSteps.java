package com.mongodb.course.m19.bdd;

import com.mongodb.course.m19.loan.model.Applicant;
import com.mongodb.course.m19.loan.model.LoanApplication;
import com.mongodb.course.m19.loan.service.LoanApplicationService;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class LoanApplicationSteps {

    @Autowired private LoanApplicationService loanApplicationService;

    private LoanApplication currentApplication;

    @When("申請人 {string} 年收入 {int} 以帳戶 {string} 申請貸款 {int} 元期限 {int} 個月")
    public void submitLoan(String name, int income, String accountId, int amount, int termMonths) {
        var applicant = new Applicant(name, accountId, BigDecimal.valueOf(income));
        currentApplication = loanApplicationService.submit(applicant, BigDecimal.valueOf(amount), termMonths);
    }

    @When("執行貸款審核")
    public void reviewLoan() {
        currentApplication = loanApplicationService.review(currentApplication.getId());
    }

    @Then("貸款申請狀態為 {string}")
    public void verifyLoanStatus(String expectedStatus) {
        assertThat(currentApplication.getStatus().name()).isEqualTo(expectedStatus);
    }
}
