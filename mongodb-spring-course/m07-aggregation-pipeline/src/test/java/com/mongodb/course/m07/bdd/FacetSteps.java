package com.mongodb.course.m07.bdd;

import com.mongodb.course.m07.banking.*;
import com.mongodb.course.m07.ecommerce.Product;
import com.mongodb.course.m07.service.FacetAggregationService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FacetSteps {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private FacetAggregationService facetService;

    private Map productFacetResult;
    private Map bankingDashboard;

    @Given("系統中有以下商品可供搜尋")
    public void setupProductsForSearch(DataTable table) {
        mongoTemplate.dropCollection(Product.class);
        List<Map<String, String>> rows = table.asMaps();
        for (Map<String, String> row : rows) {
            Product product = new Product(
                    row.get("sku"),
                    row.get("name"),
                    row.get("category"),
                    new BigDecimal(row.get("price")),
                    Boolean.parseBoolean(row.get("inStock"))
            );
            mongoTemplate.insert(product);
        }
    }

    @Given("系統中有以下銀行帳戶可供分析")
    public void setupBankAccountsForAnalysis(DataTable table) {
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

    @When("我以 $facet 搜尋 {string} 類別商品")
    public void facetSearchByCategory(String category) {
        productFacetResult = facetService.productSearchFacet(category);
    }

    @When("我以 $facet 產生銀行帳戶儀表板")
    public void facetBankingDashboard() {
        bankingDashboard = facetService.bankingDashboard();
    }

    @Then("搜尋結果總數應為 {int}")
    @SuppressWarnings("unchecked")
    public void verifyTotalCount(int expected) {
        List<Map> totalCount = (List<Map>) productFacetResult.get("totalCount");
        assertThat(((Number) totalCount.getFirst().get("total")).intValue()).isEqualTo(expected);
    }

    @And("搜尋結果應包含分頁資料與價格統計")
    @SuppressWarnings("unchecked")
    public void verifyDataAndStats() {
        List<Map> data = (List<Map>) productFacetResult.get("data");
        assertThat(data).isNotEmpty();

        List<Map> stats = (List<Map>) productFacetResult.get("stats");
        assertThat(stats).hasSize(1);
        assertThat(stats.getFirst()).containsKeys("avgPrice", "minPrice", "maxPrice");
    }

    @Then("儀表板應包含狀態統計與類型統計")
    @SuppressWarnings("unchecked")
    public void verifyDashboardCounts() {
        List<Map> statusCounts = (List<Map>) bankingDashboard.get("statusCounts");
        assertThat(statusCounts).isNotEmpty();

        List<Map> typeCounts = (List<Map>) bankingDashboard.get("typeCounts");
        assertThat(typeCounts).isNotEmpty();
    }

    @And("儀表板應包含餘額統計資訊")
    @SuppressWarnings("unchecked")
    public void verifyDashboardBalanceStats() {
        List<Map> balanceStats = (List<Map>) bankingDashboard.get("balanceStats");
        assertThat(balanceStats).hasSize(1);
        assertThat(balanceStats.getFirst()).containsKeys("totalBalance", "avgBalance");
    }
}
