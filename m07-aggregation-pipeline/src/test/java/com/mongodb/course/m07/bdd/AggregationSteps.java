package com.mongodb.course.m07.bdd;

import com.mongodb.course.m07.banking.*;
import com.mongodb.course.m07.dto.TypeCount;
import com.mongodb.course.m07.dto.TypeStats;
import com.mongodb.course.m07.insurance.*;
import com.mongodb.course.m07.service.BasicAggregationService;
import com.mongodb.course.m07.service.GroupAccumulatorService;
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

public class AggregationSteps {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BasicAggregationService basicAggregationService;

    @Autowired
    private GroupAccumulatorService groupAccumulatorService;

    private List<TypeCount> typeCounts;
    private List<TypeStats> typeStats;

    @Given("系統中有以下銀行帳戶資料")
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

    @Given("系統中有以下保險保單資料")
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

    @When("我以 Aggregation 統計活躍帳戶依類型的數量")
    public void aggregateActiveAccountsByType() {
        typeCounts = basicAggregationService.countByType(AccountStatus.ACTIVE);
    }

    @When("我以 Aggregation 計算各保單類型的保費統計")
    public void aggregatePremiumStatsByType() {
        typeStats = groupAccumulatorService.premiumStatsByType();
    }

    @Then("SAVINGS 類型應有 {int} 個帳戶")
    public void verifySavingsCount(int expected) {
        TypeCount savings = typeCounts.stream()
                .filter(r -> "SAVINGS".equals(r.type())).findFirst().orElseThrow();
        assertThat(savings.count()).isEqualTo(expected);
    }

    @And("CHECKING 類型應有 {int} 個帳戶")
    public void verifyCheckingCount(int expected) {
        TypeCount checking = typeCounts.stream()
                .filter(r -> "CHECKING".equals(r.type())).findFirst().orElseThrow();
        assertThat(checking.count()).isEqualTo(expected);
    }

    @Then("TERM_LIFE 類型的保費總計應為 {int}")
    public void verifyTermLifeTotal(int expected) {
        TypeStats termLife = typeStats.stream()
                .filter(r -> "TERM_LIFE".equals(r.type())).findFirst().orElseThrow();
        assertThat(termLife.total()).isEqualByComparingTo(new BigDecimal(expected));
    }

    @And("HEALTH 類型的保費總計應為 {int}")
    public void verifyHealthTotal(int expected) {
        TypeStats health = typeStats.stream()
                .filter(r -> "HEALTH".equals(r.type())).findFirst().orElseThrow();
        assertThat(health.total()).isEqualByComparingTo(new BigDecimal(expected));
    }
}
