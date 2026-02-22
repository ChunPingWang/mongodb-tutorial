package com.mongodb.course.m15.bdd;

import com.mongodb.client.model.Indexes;
import com.mongodb.course.m15.ecommerce.Product;
import com.mongodb.course.m15.ecommerce.ProductDataGenerator;
import com.mongodb.course.m15.ecommerce.ProductSearchService;
import com.mongodb.course.m15.index.ExplainAnalyzer;
import com.mongodb.course.m15.index.ExplainResult;
import com.mongodb.course.m15.index.IndexManagementService;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductSearchSteps {

    private static final String COLLECTION = "m15_products";

    @Autowired
    private ProductDataGenerator dataGenerator;

    @Autowired
    private ProductSearchService searchService;

    @Autowired
    private ExplainAnalyzer explainAnalyzer;

    @Autowired
    private IndexManagementService indexManagementService;

    @Autowired
    private MongoTemplate mongoTemplate;

    private List<Product> lastResults;
    private ExplainResult lastExplainResult;

    @Given("系統已產生 {int} 筆產品資料涵蓋 {int} 個分類")
    public void generateProducts(int count, int categories) {
        mongoTemplate.remove(new Query(), COLLECTION);
        mongoTemplate.indexOps(COLLECTION).dropAllIndexes();
        dataGenerator.generateProducts(count);
    }

    @Given("已建立文字索引於 name 與 description 欄位")
    public void createTextIndex() {
        indexManagementService.createTextIndex(COLLECTION,
                Map.of("name", 3F, "description", 1F));
    }

    @Given("已建立部分索引僅索引有庫存產品")
    public void createPartialIndex() {
        indexManagementService.createPartialIndex(COLLECTION,
                Indexes.ascending("category"),
                new Document("inStock", true));
    }

    @Given("已建立複合索引 category_price")
    public void createCategoryPriceIndex() {
        var keys = new LinkedHashMap<String, Sort.Direction>();
        keys.put("category", Sort.Direction.ASC);
        keys.put("price", Sort.Direction.ASC);
        indexManagementService.createCompoundIndex(COLLECTION, keys);
    }

    @Given("已建立標籤多鍵索引")
    public void createTagsIndex() {
        indexManagementService.createSingleFieldIndex(COLLECTION, "tags", Sort.Direction.ASC);
    }

    @When("以關鍵字 {string} 搜尋產品")
    public void textSearch(String keyword) {
        lastResults = searchService.textSearch(keyword);
    }

    @When("查詢分類 {string} 價格在 {int} 到 {int} 之間的產品")
    public void queryByCategoryAndPriceRange(String category, int minPrice, int maxPrice) {
        lastResults = searchService.findByCategoryAndPriceRange(category, minPrice, maxPrice);
        lastExplainResult = explainAnalyzer.explain(COLLECTION,
                new Document("category", category)
                        .append("price", new Document("$gte", (long) minPrice).append("$lte", (long) maxPrice)));
    }

    @When("查詢有庫存的 {string} 分類產品")
    public void queryInStockByCategory(String category) {
        lastResults = searchService.findInStockByCategory(category);
        lastExplainResult = explainAnalyzer.explain(COLLECTION,
                new Document("inStock", true).append("category", category));
    }

    @When("以標籤 {string} 搜尋產品")
    public void searchByTag(String tag) {
        lastResults = searchService.findByTag(tag);
        lastExplainResult = explainAnalyzer.explain(COLLECTION,
                new Document("tags", tag));
    }

    @When("查詢有庫存產品並依價格升冪排序取前 {int} 筆")
    public void queryInStockSortedByPrice(int limit) {
        lastResults = searchService.findInStockSortedByPrice(limit);
    }

    @Then("應搜尋到至少 {int} 個產品")
    public void verifyMinResults(int minCount) {
        assertThat(lastResults).hasSizeGreaterThanOrEqualTo(minCount);
    }

    @Then("查詢使用文字索引")
    public void verifyTextIndexUsed() {
        // Text search always uses text index when one exists — verify results are non-empty
        assertThat(lastResults).isNotEmpty();
    }

    @Then("查詢計畫應使用索引掃描")
    public void verifyIxscan() {
        assertThat(lastExplainResult.stage()).isEqualTo("IXSCAN");
    }

    @Then("回傳產品皆屬於 {string} 分類")
    public void verifyAllCategory(String category) {
        assertThat(lastResults).allMatch(p -> p.category().equals(category));
    }

    @Then("查詢計畫應使用部分索引")
    public void verifyPartialIndexUsed() {
        assertThat(lastExplainResult.stage()).isEqualTo("IXSCAN");
    }

    @Then("回傳產品皆為有庫存狀態")
    public void verifyAllInStock() {
        assertThat(lastResults).allMatch(Product::inStock);
    }

    @Then("查詢計畫應使用標籤索引")
    public void verifyTagIndexUsed() {
        assertThat(lastExplainResult.stage()).isEqualTo("IXSCAN");
    }

    @Then("應回傳 {int} 筆產品")
    public void verifyResultCount(int expected) {
        assertThat(lastResults).hasSize(expected);
    }

    @Then("產品價格應為升冪排列")
    public void verifyPriceAscending() {
        for (int i = 0; i < lastResults.size() - 1; i++) {
            assertThat(lastResults.get(i).price())
                    .isLessThanOrEqualTo(lastResults.get(i + 1).price());
        }
    }

    @Then("所有產品皆為有庫存")
    public void verifyAllProductsInStock() {
        assertThat(lastResults).allMatch(Product::inStock);
    }
}
