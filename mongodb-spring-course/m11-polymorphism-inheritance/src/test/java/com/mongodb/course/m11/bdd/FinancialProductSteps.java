package com.mongodb.course.m11.bdd;

import com.mongodb.course.m11.banking.model.Deposit;
import com.mongodb.course.m11.banking.model.FinancialProduct;
import com.mongodb.course.m11.banking.model.Fund;
import com.mongodb.course.m11.banking.model.InsuranceProduct;
import com.mongodb.course.m11.banking.model.RiskProfile;
import com.mongodb.course.m11.banking.service.FinancialProductService;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FinancialProductSteps {

    @Autowired
    private FinancialProductService service;

    @Autowired
    private MongoTemplate mongoTemplate;

    private List<FinancialProduct> queryResults;
    private Document rawBson;
    private final Map<String, BigDecimal> annualReturns = new HashMap<>();

    @Before
    public void setUp() {
        mongoTemplate.dropCollection("m11_financial_products");
        queryResults = null;
        rawBson = null;
        annualReturns.clear();
    }

    @Given("已儲存定存商品 {string} 金額 {int} 年利率 {double}")
    public void saveDeposit(String name, int value, double rate) {
        service.save(new Deposit(null, name, BigDecimal.valueOf(value),
                BigDecimal.valueOf(rate), 12));
    }

    @Given("已儲存基金商品 {string} 金額 {int} 淨值 {double} 風險等級 {int}")
    public void saveFund(String name, int value, double nav, int riskLevel) {
        service.save(new Fund(null, name, BigDecimal.valueOf(value),
                BigDecimal.valueOf(nav), new RiskProfile(riskLevel, "LEVEL_" + riskLevel)));
    }

    @Given("已儲存保險商品 {string} 金額 {int} 繳費年期 {int} 保額 {int}")
    public void saveInsuranceProduct(String name, int value, int years, int coverage) {
        service.save(new InsuranceProduct(null, name, BigDecimal.valueOf(value),
                years, BigDecimal.valueOf(coverage)));
    }

    @When("統一查詢所有金融商品")
    public void queryAll() {
        queryResults = service.findAll();
    }

    @Then("返回 {int} 筆商品")
    public void verifyCount(int expected) {
        assertThat(queryResults).hasSize(expected);
    }

    @Then("篩選定存類型返回 {int} 筆")
    public void verifyDepositCount(int expected) {
        List<Deposit> deposits = service.findByType(Deposit.class);
        assertThat(deposits).hasSize(expected);
    }

    @When("查詢該商品的原始 BSON 文件")
    public void queryRawBson() {
        rawBson = mongoTemplate.getCollection("m11_financial_products").find().first();
    }

    @Then("_class 欄位值為 {string}")
    public void verifyClassAlias(String expectedAlias) {
        assertThat(rawBson).isNotNull();
        assertThat(rawBson.getString("_class")).isEqualTo(expectedAlias);
    }

    @When("計算各商品的預估年報酬")
    public void calculateReturns() {
        List<FinancialProduct> all = service.findAll();
        for (FinancialProduct product : all) {
            annualReturns.put(product.name(), service.estimateAnnualReturn(product));
        }
    }

    @Then("{string} 預估年報酬為 {int}")
    public void verifyReturn(String name, int expected) {
        assertThat(annualReturns.get(name)).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }
}
