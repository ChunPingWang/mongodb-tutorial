package com.mongodb.course.m15.bdd;

import com.mongodb.client.MongoClient;
import com.mongodb.course.m15.banking.TransactionDataGenerator;
import com.mongodb.course.m15.banking.TransactionQueryService;
import com.mongodb.course.m15.index.ExplainAnalyzer;
import com.mongodb.course.m15.index.ExplainResult;
import com.mongodb.course.m15.index.IndexManagementService;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionIndexSteps {

    private static final String COLLECTION = "m15_transactions";

    @Autowired
    private TransactionDataGenerator dataGenerator;

    @Autowired
    private TransactionQueryService queryService;

    @Autowired
    private ExplainAnalyzer explainAnalyzer;

    @Autowired
    private IndexManagementService indexManagementService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    private ExplainResult lastExplainResult;

    @Before("@transaction or not @product")
    public void cleanTransactions() {
        // Only clean if this is a transaction scenario
    }

    @Given("系統已產生 {int} 筆交易資料涵蓋 {int} 個帳戶")
    public void generateTransactions(int totalCount, int accountCount) {
        mongoTemplate.remove(new Query(), COLLECTION);
        mongoTemplate.indexOps(COLLECTION).dropAllIndexes();
        dataGenerator.generateTransactions(totalCount, accountCount);
    }

    @Given("已建立複合索引 accountId_type_transactionDate")
    public void createEsrIndex() {
        var keys = new LinkedHashMap<String, Sort.Direction>();
        keys.put("accountId", Sort.Direction.ASC);
        keys.put("type", Sort.Direction.ASC);
        keys.put("transactionDate", Sort.Direction.ASC);
        indexManagementService.createCompoundIndex(COLLECTION, keys);
    }

    @Given("已建立覆蓋查詢索引 accountId_amount")
    public void createCoveredQueryIndex() {
        var keys = new LinkedHashMap<String, Sort.Direction>();
        keys.put("accountId", Sort.Direction.ASC);
        keys.put("amount", Sort.Direction.ASC);
        indexManagementService.createCompoundIndex(COLLECTION, keys);
    }

    @Given("已建立 TTL 索引過期秒數為 {int}")
    public void createTtlIndex(int expireSeconds) {
        // Speed up TTL monitor for testing
        mongoClient.getDatabase("admin").runCommand(
                new Document("setParameter", 1).append("ttlMonitorSleepSecs", 1));
        indexManagementService.createTtlIndex(COLLECTION, "createdAt", expireSeconds);
    }

    @Given("系統已產生 {int} 筆暫存交易資料")
    public void generateTempTransactions(int count) {
        dataGenerator.generateTemporaryTransactions(count);
    }

    @When("查詢帳戶 {string} 的所有交易")
    public void queryByAccount(String accountId) {
        lastExplainResult = explainAnalyzer.explain(COLLECTION,
                new Document("accountId", accountId));
    }

    @When("以帳戶 {string} 類型 {string} 日期範圍查詢")
    public void queryByAccountTypeAndDateRange(String accountId, String type) {
        var from = Instant.parse("2024-01-01T00:00:00Z");
        var to = Instant.parse("2024-12-31T00:00:00Z");

        queryService.findByAccountTypeAndDateRange(
                accountId,
                com.mongodb.course.m15.banking.TransactionType.valueOf(type),
                from, to);

        lastExplainResult = explainAnalyzer.explain(COLLECTION,
                new Document("accountId", accountId)
                        .append("type", type)
                        .append("transactionDate",
                                new Document("$gte", from).append("$lte", to)));
    }

    @When("僅投影帳戶與金額欄位查詢帳戶 {string}")
    public void queryCoveredProjection(String accountId) {
        queryService.findAccountAmountOnly(accountId);

        lastExplainResult = explainAnalyzer.explain(COLLECTION,
                new Document("accountId", accountId),
                new Document("accountId", 1).append("amount", 1).append("_id", 0));
    }

    @When("等待 {int} 秒讓 TTL 監控器執行")
    public void waitForTtlMonitor(int seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000L);
    }

    @When("以 description 欄位查詢交易")
    public void queryByDescription() {
        lastExplainResult = explainAnalyzer.explain(COLLECTION,
                new Document("description", "deposit transaction #0"));
    }

    @Then("查詢計畫應使用 {string}")
    public void verifyScanType(String expectedStage) {
        assertThat(lastExplainResult.stage()).isEqualTo(expectedStage);
    }

    @Then("檢查的索引鍵數應接近回傳文件數")
    public void verifyKeysCloseToReturned() {
        assertThat(lastExplainResult.keysExamined())
                .isLessThanOrEqualTo(lastExplainResult.nReturned() * 2);
    }

    @Then("檢查的文件數應等於回傳文件數")
    public void verifyDocsEqualReturned() {
        assertThat(lastExplainResult.docsExamined())
                .isEqualTo(lastExplainResult.nReturned());
    }

    @Then("查詢計畫應為覆蓋查詢")
    public void verifyCoveredQuery() {
        assertThat(lastExplainResult.isIndexOnly()).isTrue();
    }

    @Then("檢查的文件數應為 {int}")
    public void verifyDocsExamined(int expected) {
        assertThat(lastExplainResult.docsExamined()).isEqualTo(expected);
    }

    @Then("暫存交易資料應已被自動清除")
    public void verifyTtlCleanup() {
        long count = mongoTemplate.count(
                Query.query(org.springframework.data.mongodb.core.query.Criteria.where("category").is("temp")),
                COLLECTION);
        assertThat(count).isZero();
    }
}
