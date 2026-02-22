package com.mongodb.course.m06.bdd;

import com.mongodb.course.m06.ecommerce.Product;
import com.mongodb.course.m06.ecommerce.ProductQueryService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TextSearchSteps {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ProductQueryService productQueryService;

    private List<Product> searchResults;

    @Given("系統中有以下產品資料")
    public void setupProducts(DataTable table) {
        mongoTemplate.dropCollection(Product.class);

        // Create text index programmatically
        TextIndexDefinition textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
                .onField("name", 3F)
                .onField("description")
                .build();
        mongoTemplate.indexOps(Product.class).ensureIndex(textIndex);

        List<Map<String, String>> rows = table.asMaps();
        for (Map<String, String> row : rows) {
            Product product = new Product(
                    row.get("sku"),
                    row.get("name"),
                    row.get("category"),
                    new BigDecimal("999"),
                    true
            );
            product.setDescription(row.get("description"));
            mongoTemplate.insert(product);
        }
    }

    @When("我以關鍵字 {string} 進行全文檢索")
    public void textSearch(String keyword) {
        searchResults = productQueryService.textSearch(keyword);
    }

    @When("我搜尋 {string} 但排除 {string}")
    public void textSearchExcluding(String include, String exclude) {
        searchResults = productQueryService.textSearchExcluding(include, exclude);
    }

    @Then("應該搜尋到至少 {int} 個產品")
    public void verifyMinResults(int min) {
        assertThat(searchResults).hasSizeGreaterThanOrEqualTo(min);
    }

    @And("搜尋結果中包含 {string}")
    public void verifyResultContains(String productName) {
        assertThat(searchResults).anyMatch(p -> p.getName().equals(productName));
    }

    @Then("搜尋結果不包含 {string}")
    public void verifyResultNotContains(String productName) {
        assertThat(searchResults).noneMatch(p -> p.getName().equals(productName));
    }
}
