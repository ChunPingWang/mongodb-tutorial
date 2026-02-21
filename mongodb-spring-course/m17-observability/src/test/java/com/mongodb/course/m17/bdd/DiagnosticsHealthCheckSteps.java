package com.mongodb.course.m17.bdd;

import com.mongodb.course.m17.ecommerce.ProductService;
import com.mongodb.course.m17.observability.CollectionStatsReport;
import com.mongodb.course.m17.observability.DatabaseStatsReport;
import com.mongodb.course.m17.observability.MongoDetailedHealthIndicator;
import com.mongodb.course.m17.observability.MongoDiagnosticService;
import com.mongodb.course.m17.observability.ServerStatusReport;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;

public class DiagnosticsHealthCheckSteps {

    @Autowired
    private MongoDiagnosticService diagnosticService;

    @Autowired
    private MongoDetailedHealthIndicator healthIndicator;

    @Autowired
    private ProductService productService;

    @Autowired
    private MongoTemplate mongoTemplate;

    private ServerStatusReport serverStatusReport;
    private DatabaseStatsReport databaseStatsReport;
    private CollectionStatsReport collectionStatsReport;
    private Health health;

    @Given("已新增 {int} 筆商品資料")
    public void insertProducts(int count) {
        for (int i = 1; i <= count; i++) {
            productService.create("Product " + i, "electronics", i * 100L);
        }
    }

    @Given("已清除商品集合")
    public void clearProducts() {
        mongoTemplate.remove(new Query(), "m17_products");
    }

    @When("查詢 MongoDB 伺服器狀態")
    public void queryServerStatus() {
        serverStatusReport = diagnosticService.getServerStatus();
    }

    @When("查詢資料庫統計")
    public void queryDatabaseStats() {
        databaseStatsReport = diagnosticService.getDatabaseStats();
    }

    @When("查詢商品集合統計")
    public void queryCollectionStats() {
        collectionStatsReport = diagnosticService.getCollectionStats("m17_products");
    }

    @When("執行 MongoDB 健康檢查")
    public void performHealthCheck() {
        health = healthIndicator.health();
    }

    @Then("伺服器版本應以 {string} 開頭")
    public void verifyServerVersion(String prefix) {
        assertThat(serverStatusReport.version()).startsWith(prefix);
    }

    @Then("目前連線數應大於 {int}")
    public void verifyCurrentConnections(int min) {
        assertThat(serverStatusReport.currentConnections()).isGreaterThan(min);
    }

    @Then("集合數量應大於 {int}")
    public void verifyCollectionsCount(int min) {
        assertThat(databaseStatsReport.collections()).isGreaterThan(min);
    }

    @Then("文件總數應大於 {int}")
    public void verifyTotalDocuments(int min) {
        assertThat(databaseStatsReport.documents()).isGreaterThan(min);
    }

    @Then("文件數量應為 {int}")
    public void verifyDocumentCount(int expected) {
        assertThat(collectionStatsReport.documentCount()).isEqualTo(expected);
    }

    @Then("健康狀態應為 {string}")
    public void verifyHealthStatus(String status) {
        assertThat(health.getStatus().getCode()).isEqualTo(status);
    }

    @Then("健康詳細資訊應包含 {string}")
    public void verifyHealthDetail(String key) {
        assertThat(health.getDetails()).containsKey(key);
    }

    @Then("版本資訊應以 {string} 開頭")
    public void verifyHealthVersionPrefix(String prefix) {
        var version = (String) health.getDetails().get("version");
        assertThat(version).startsWith(prefix);
    }
}
