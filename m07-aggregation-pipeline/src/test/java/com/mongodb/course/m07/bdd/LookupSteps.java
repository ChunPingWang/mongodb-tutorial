package com.mongodb.course.m07.bdd;

import com.mongodb.course.m07.ecommerce.*;
import com.mongodb.course.m07.service.LookupAggregationService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupSteps {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private LookupAggregationService lookupService;

    private List<Map> lookupResults;
    private List<Map> customerSummary;

    @Given("系統中有以下商品資料")
    public void setupProducts(DataTable table) {
        mongoTemplate.dropCollection(Product.class);
        List<Map<String, String>> rows = table.asMaps();
        for (Map<String, String> row : rows) {
            Product product = new Product(
                    row.get("sku"),
                    row.get("name"),
                    row.get("category"),
                    new BigDecimal(row.get("price")),
                    true
            );
            mongoTemplate.insert(product);
        }
    }

    @Given("系統中有以下訂單資料")
    public void setupOrders(DataTable table) {
        mongoTemplate.dropCollection(Order.class);
        List<Map<String, String>> rows = table.asMaps();
        for (Map<String, String> row : rows) {
            Order order = new Order(
                    row.get("orderNumber"),
                    row.get("customerName"),
                    new BigDecimal(row.get("totalAmount")),
                    OrderStatus.valueOf(row.get("status")),
                    LocalDate.of(2025, 1, 15)
            );

            // Parse items: "SKU-001:Mouse:2:29.99,SKU-002:Keyboard:1:89.99"
            String itemsStr = row.get("items");
            List<OrderItem> items = new ArrayList<>();
            for (String itemStr : itemsStr.split(",")) {
                String[] parts = itemStr.split(":");
                items.add(new OrderItem(parts[0].trim(), parts[1].trim(),
                        Integer.parseInt(parts[2].trim()), new BigDecimal(parts[3].trim())));
            }
            order.setItems(items);
            mongoTemplate.insert(order);
        }
    }

    @When("我使用 $lookup 關聯訂單與商品")
    public void lookupOrdersWithProducts() {
        lookupResults = lookupService.ordersWithProductDetails();
    }

    @When("我以 Aggregation 計算客戶訂單匯總")
    public void aggregateCustomerSummary() {
        customerSummary = lookupService.customerOrderSummary();
    }

    @Then("訂單 {string} 應包含 {int} 筆關聯商品資料")
    public void verifyLookupCount(String orderNumber, int expected) {
        long count = lookupResults.stream()
                .filter(r -> orderNumber.equals(r.get("orderNumber")))
                .count();
        assertThat(count).isEqualTo(expected);
    }

    @Then("{string} 的訂單數為 {int} 且消費總額為 {int}")
    public void verifyCustomerSummary(String customer, int orderCount, int totalSpent) {
        Map result = customerSummary.stream()
                .filter(r -> customer.equals(r.get("customerName")))
                .findFirst().orElseThrow();
        assertThat(((Number) result.get("orderCount")).intValue()).isEqualTo(orderCount);
        BigDecimal total = new BigDecimal(result.get("totalSpent").toString());
        assertThat(total).isEqualByComparingTo(new BigDecimal(totalSpent));
    }

}
