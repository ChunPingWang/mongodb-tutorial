package com.mongodb.course.m21.bdd;

import com.mongodb.course.m21.listing.model.ProductListing;
import com.mongodb.course.m21.listing.service.ProductListingService;
import com.mongodb.course.m21.order.model.OrderLine;
import com.mongodb.course.m21.order.model.ShippingAddress;
import com.mongodb.course.m21.order.service.OrderCommandService;
import com.mongodb.course.m21.product.ProductCatalogService;
import com.mongodb.course.m21.projection.readmodel.OrderDashboardDocument;
import com.mongodb.course.m21.projection.readmodel.TimelineEntry;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProductListingSteps {

    @Autowired private ProductListingService listingService;
    @Autowired private OrderCommandService orderCommandService;
    @Autowired private ProductCatalogService productCatalogService;
    @Autowired private MongoTemplate mongoTemplate;

    private ProductListing currentListing;
    private Exception caughtException;

    @Given("類別 {string} 有 {int} 筆訂單平均金額 {int} 元取消率 {int}%")
    public void setupCategoryOrders(String category, int orderCount, int avgAmount, int cancelRate) {
        var address = new ShippingAddress("Test", "St", "Taipei", "100");
        int cancelCount = (int) Math.round(orderCount * cancelRate / 100.0);

        for (int i = 1; i <= orderCount; i++) {
            String orderId = "ORD-CAT-" + category + "-" + i;
            var lines = List.of(new OrderLine("P-CAT-" + i, "Item", category, 1, new BigDecimal(avgAmount)));
            orderCommandService.placeOrder(orderId, "CUST-CAT-" + i, lines, address);
        }

        for (int i = 1; i <= cancelCount; i++) {
            String orderId = "ORD-CAT-" + category + "-" + i;
            orderCommandService.cancel(orderId, "Test cancellation");
        }
    }

    @When("提交商品上架 SKU {string} 名稱 {string} 類別 {string} 價格 {int} 元庫存 {int} 件")
    public void submitListing(String sku, String name, String category, int price, int stock) {
        currentListing = listingService.submit(sku, name, category, "Electronics",
                new BigDecimal(price), stock);
    }

    @When("執行上架審核")
    public void executeReview() {
        currentListing = listingService.review(currentListing);
    }

    @Then("上架申請狀態為 {string}")
    public void verifyListingStatus(String status) {
        assertThat(currentListing.getStatus().name()).isEqualTo(status);
    }

    @Then("商品集合中存在 SKU {string}")
    public void verifyProductExists(String sku) {
        var product = productCatalogService.findBySku(sku);
        assertThat(product).isPresent();
    }

    @When("直接插入缺少必填欄位的商品文件")
    public void insertInvalidDocument() {
        try {
            var invalidDoc = new Document("name", "Incomplete Product");
            mongoTemplate.insert(invalidDoc, "m21_products");
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @Then("操作失敗並拋出 MongoWriteException")
    public void verifyMongoWriteException() {
        assertThat(caughtException).isNotNull();
        assertThat(caughtException).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
