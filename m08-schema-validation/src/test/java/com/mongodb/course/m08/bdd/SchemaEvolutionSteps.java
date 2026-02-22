package com.mongodb.course.m08.bdd;

import com.mongodb.MongoWriteException;
import com.mongodb.course.m08.ecommerce.Product;
import com.mongodb.course.m08.ecommerce.ProductV1;
import com.mongodb.course.m08.service.BeanValidationService;
import com.mongodb.course.m08.service.SchemaEvolutionService;
import com.mongodb.course.m08.service.SchemaValidationService;
import io.cucumber.java.en.And;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaEvolutionSteps {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SchemaValidationService schemaValidationService;

    @Autowired
    private BeanValidationService beanValidationService;

    @Autowired
    private SchemaEvolutionService schemaEvolutionService;

    private String productCollectionName;
    private String versionCollectionName;
    private Exception lastException;

    @Given("建立 {string} 集合帶有產品 Schema 驗證")
    public void createProductCollectionWithSchema(String collectionName) {
        this.productCollectionName = collectionName;
        mongoTemplate.dropCollection(collectionName);
        MongoJsonSchema schema = schemaValidationService.buildProductSchema();
        schemaValidationService.createCollectionStrict(collectionName, schema);
    }

    @When("透過 Java 儲存 SKU 為空白的產品")
    public void saveProductWithBlankSku() {
        Product invalid = new Product();
        invalid.setSku("");
        invalid.setName("Test Product");
        invalid.setCategory("Test");
        invalid.setPrice(new BigDecimal("10"));

        this.lastException = null;
        try {
            beanValidationService.saveProduct(invalid);
        } catch (Exception e) {
            this.lastException = e;
        }
    }

    @Then("應該被 Bean Validation 攔截")
    public void shouldBeCaughtByBeanValidation() {
        assertThat(lastException).isInstanceOf(ConstraintViolationException.class);
    }

    @When("透過原始 BSON 插入缺少 {string} 的產品")
    public void insertRawBsonMissingField(String fieldName) {
        Document doc = new Document()
                .append("name", "Test Product")
                .append("category", "Test")
                .append("price", new Decimal128(new BigDecimal("10")));
        // sku is intentionally not added

        this.lastException = null;
        try {
            mongoTemplate.getCollection(productCollectionName).insertOne(doc);
        } catch (Exception e) {
            this.lastException = e;
        }
    }

    @Then("應該被 MongoDB Schema 驗證攔截")
    public void shouldBeCaughtByMongoDBSchema() {
        assertThat(lastException).isInstanceOf(MongoWriteException.class);
    }

    @Given("在 {string} 集合中插入 {int} 筆 V1 產品")
    public void insertV1Products(String collectionName, int count) {
        this.versionCollectionName = collectionName;
        mongoTemplate.dropCollection(collectionName);
        for (int i = 1; i <= count; i++) {
            ProductV1 product = new ProductV1("Product-" + i, new BigDecimal(100 * i), true);
            mongoTemplate.save(product, collectionName);
        }
    }

    @When("執行遷移到版本 {int}")
    public void migrateToVersion(int targetVersion) {
        schemaEvolutionService.migrateToVersion(targetVersion);
    }

    @Then("所有產品的 schemaVersion 應該為 {int}")
    public void allProductsShouldHaveVersion(int expectedVersion) {
        List<Document> docs = mongoTemplate.findAll(Document.class, versionCollectionName);
        for (Document doc : docs) {
            assertThat(doc.getInteger("schemaVersion")).isEqualTo(expectedVersion);
        }
    }

    @Then("所有產品應該包含 {string} 和 {string} 和 {string} 欄位")
    public void allProductsShouldContainFields(String field1, String field2, String field3) {
        List<Document> docs = mongoTemplate.findAll(Document.class, versionCollectionName);
        for (Document doc : docs) {
            assertThat(doc).containsKey(field1);
            assertThat(doc).containsKey(field2);
            assertThat(doc).containsKey(field3);
        }
    }
}
