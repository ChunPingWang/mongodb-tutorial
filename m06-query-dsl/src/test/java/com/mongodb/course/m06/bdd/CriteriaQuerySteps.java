package com.mongodb.course.m06.bdd;

import com.mongodb.course.m06.banking.*;
import com.mongodb.course.m06.insurance.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CriteriaQuerySteps {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BankAccountQueryService bankAccountQueryService;

    @Autowired
    private InsurancePolicyQueryService insurancePolicyQueryService;

    private List<BankAccount> bankResults;
    private List<InsurancePolicyDocument> policyResults;

    @Given("系統中有以下銀行帳戶")
    public void setupBankAccounts(DataTable table) {
        mongoTemplate.dropCollection(BankAccount.class);
        List<Map<String, String>> rows = table.asMaps();
        for (Map<String, String> row : rows) {
            BankAccount account = new BankAccount(
                    row.get("accountNumber"),
                    row.get("holderName"),
                    AccountType.valueOf(row.get("type")),
                    new BigDecimal(row.get("balance"))
            );
            account.setStatus(AccountStatus.valueOf(row.get("status")));
            mongoTemplate.insert(account);
        }
    }

    @Given("系統中有以下保險保單")
    public void setupInsurancePolicies(DataTable table) {
        mongoTemplate.dropCollection(InsurancePolicyDocument.class);
        List<Map<String, String>> rows = table.asMaps();
        LocalDate now = LocalDate.now();
        for (Map<String, String> row : rows) {
            InsurancePolicyDocument policy = new InsurancePolicyDocument(
                    row.get("policyNumber"),
                    row.get("holderName"),
                    PolicyType.valueOf(row.get("policyType")),
                    new BigDecimal(row.get("premium")),
                    new BigDecimal("100000"),
                    now.minusYears(1),
                    now.plusYears(1)
            );
            policy.setStatus(PolicyStatus.valueOf(row.get("status")));
            mongoTemplate.insert(policy);
        }
    }

    @When("我查詢餘額在 {int} 到 {int} 之間的帳戶")
    public void queryByBalanceRange(int min, int max) {
        bankResults = bankAccountQueryService.findByBalanceRange(
                new BigDecimal(min), new BigDecimal(max));
    }

    @When("我以動態條件查詢 policyType 為 {string} 且 status 為 {string}")
    public void queryByDynamicConditions(String type, String status) {
        policyResults = insurancePolicyQueryService.findByMultipleConditions(
                PolicyType.valueOf(type), PolicyStatus.valueOf(status), null);
    }

    @Then("應該回傳 {int} 個帳戶")
    public void verifyBankAccountCount(int count) {
        assertThat(bankResults).hasSize(count);
    }

    @Then("應該回傳 {int} 個保單")
    public void verifyPolicyCount(int count) {
        assertThat(policyResults).hasSize(count);
    }

    @And("回傳的帳戶餘額都在範圍內")
    public void verifyBalanceRange() {
        assertThat(bankResults).allMatch(a ->
                a.getBalance().compareTo(new BigDecimal("10000")) >= 0 &&
                        a.getBalance().compareTo(new BigDecimal("60000")) <= 0);
    }

    @And("保單號碼為 {string}")
    public void verifyPolicyNumber(String policyNumber) {
        assertThat(policyResults.getFirst().getPolicyNumber()).isEqualTo(policyNumber);
    }
}
