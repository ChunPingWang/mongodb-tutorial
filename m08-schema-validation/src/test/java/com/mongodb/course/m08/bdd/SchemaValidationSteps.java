package com.mongodb.course.m08.bdd;

import com.mongodb.MongoWriteException;
import com.mongodb.course.m08.insurance.InsurancePolicyDocument;
import com.mongodb.course.m08.insurance.PolicyType;
import com.mongodb.course.m08.service.BeanValidationService;
import com.mongodb.course.m08.service.SchemaValidationService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.validation.ConstraintViolationException;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaValidationSteps {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SchemaValidationService schemaValidationService;

    @Autowired
    private BeanValidationService beanValidationService;

    private String currentCollectionName;
    private Exception lastException;

    @Given("建立 {string} 集合帶有嚴格的銀行帳戶 Schema 驗證")
    public void createBankAccountCollectionWithSchema(String collectionName) {
        this.currentCollectionName = collectionName;
        mongoTemplate.dropCollection(collectionName);
        MongoJsonSchema schema = schemaValidationService.buildBankAccountSchema();
        schemaValidationService.createCollectionStrict(collectionName, schema);
    }

    @When("嘗試插入缺少 {string} 的銀行帳戶文件")
    public void insertDocumentMissingField(String fieldName) {
        Document doc = new Document()
                .append("accountNumber", "ACC-12345")
                .append("type", "SAVINGS")
                .append("balance", new Decimal128(new BigDecimal("1000")))
                .append("status", "ACTIVE");
        // holderName is intentionally not added

        this.lastException = null;
        try {
            mongoTemplate.getCollection(currentCollectionName).insertOne(doc);
        } catch (Exception e) {
            this.lastException = e;
        }
    }

    @Then("應該拋出 MongoDB 寫入錯誤")
    public void shouldThrowMongoWriteException() {
        assertThat(lastException).isInstanceOf(MongoWriteException.class);
    }

    @Given("系統已啟用 Bean Validation")
    public void beanValidationEnabled() {
        // ValidatingMongoEventListener is auto-configured via ValidationConfig
    }

    @When("嘗試儲存保費為 {string} 的保險保單")
    public void savePolicyWithPremium(String premium) {
        InsurancePolicyDocument policy = new InsurancePolicyDocument(
                "POL-001", "Alice Chen", PolicyType.HEALTH,
                new BigDecimal(premium), new BigDecimal("1000000"),
                LocalDate.now(), LocalDate.now().plusYears(1)
        );
        this.lastException = null;
        try {
            beanValidationService.savePolicy(policy);
        } catch (Exception e) {
            this.lastException = e;
        }
    }

    @Then("應該拋出 ConstraintViolationException")
    public void shouldThrowConstraintViolationException() {
        assertThat(lastException).isInstanceOf(ConstraintViolationException.class);
    }

    @When("嘗試儲存持有人姓名為空白的保險保單")
    public void savePolicyWithBlankHolderName() {
        InsurancePolicyDocument policy = new InsurancePolicyDocument(
                "POL-002", "", PolicyType.HEALTH,
                new BigDecimal("500"), new BigDecimal("1000000"),
                LocalDate.now(), LocalDate.now().plusYears(1)
        );
        this.lastException = null;
        try {
            beanValidationService.savePolicy(policy);
        } catch (Exception e) {
            this.lastException = e;
        }
    }
}
