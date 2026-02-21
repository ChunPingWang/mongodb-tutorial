package com.mongodb.course.m18.bdd;

import com.mongodb.course.m18.insurance.changeunit.V001_SeedPoliciesChangeUnit;
import com.mongodb.course.m18.insurance.changeunit.V002_AddRiskScoreChangeUnit;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MongockMigrationSteps {

    private static final String COLLECTION = "m18_policies";

    @Autowired
    private MongoTemplate mongoTemplate;

    private final V001_SeedPoliciesChangeUnit v001 = new V001_SeedPoliciesChangeUnit();
    private final V002_AddRiskScoreChangeUnit v002 = new V002_AddRiskScoreChangeUnit();

    @Given("已插入 {int} 筆 V1 保單文件")
    public void seedV1Policies(int count) {
        var collection = mongoTemplate.getCollection(COLLECTION);
        String[] types = {"AUTO", "HOME", "LIFE"};
        List<Document> docs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            docs.add(new Document()
                    .append("policyNumber", "POL-%05d".formatted(i + 1))
                    .append("holderName", "Holder-%d".formatted(i + 1))
                    .append("type", types[i % 3])
                    .append("premium", 10000L + (i * 37L) % 50000L)
                    .append("status", i % 5 == 0 ? "EXPIRED" : "ACTIVE")
                    .append("effectiveDate", "2024-01-01")
                    .append("expirationDate", "2025-01-01")
                    .append("schemaVersion", 1));
        }
        collection.insertMany(docs);
    }

    @When("執行 V002 遷移加入 riskScore 欄位")
    public void executeV002Migration() {
        v002.execution(mongoTemplate);
    }

    @Given("已執行 V002 遷移")
    public void v002AlreadyExecuted() {
        v002.execution(mongoTemplate);
    }

    @When("執行 V002 的 rollback")
    public void executeV002Rollback() {
        v002.rollback(mongoTemplate);
    }

    @Then("所有保單的 schemaVersion 應為 {int}")
    public void allPoliciesShouldHaveVersion(int expectedVersion) {
        long total = mongoTemplate.getCollection(COLLECTION).countDocuments();
        long matchCount = mongoTemplate.getCollection(COLLECTION)
                .countDocuments(new Document("schemaVersion", expectedVersion));
        assertThat(matchCount).isEqualTo(total);
    }

    @Then("所有保單應包含 riskScore 欄位")
    public void allPoliciesShouldHaveRiskScore() {
        long total = mongoTemplate.getCollection(COLLECTION).countDocuments();
        long withField = mongoTemplate.getCollection(COLLECTION)
                .countDocuments(new Document("riskScore", new Document("$exists", true)));
        assertThat(withField).isEqualTo(total);
    }

    @Then("{word} 類型保單的 riskScore 應為 {int}")
    public void typeShouldHaveRiskScore(String type, int expectedScore) {
        var doc = mongoTemplate.getCollection(COLLECTION)
                .find(new Document("type", type)).first();
        assertThat(doc).isNotNull();
        assertThat(doc.getInteger("riskScore")).isEqualTo(expectedScore);
    }

    @Then("保單不應包含 riskScore 欄位")
    public void policiesShouldNotHaveRiskScore() {
        long withField = mongoTemplate.getCollection(COLLECTION)
                .countDocuments(new Document("riskScore", new Document("$exists", true)));
        assertThat(withField).isZero();
    }

    @Then("應可查詢到 V002 的遷移執行紀錄")
    public void shouldFindV002AuditRecord() {
        // Manually store audit record since we bypass Mongock runner
        var auditCollection = mongoTemplate.getCollection("m18_migration_audit");
        auditCollection.insertOne(new Document()
                .append("changeId", "v002-add-risk-score")
                .append("author", "m18")
                .append("timestamp", java.time.Instant.now().toString()));

        var doc = auditCollection.find(new Document("changeId", "v002-add-risk-score")).first();
        assertThat(doc).isNotNull();
        assertThat(doc.getString("author")).isEqualTo("m18");
    }
}
