package com.mongodb.course.m18.bdd;

import com.mongodb.course.m18.ecommerce.Address;
import com.mongodb.course.m18.ecommerce.Customer;
import com.mongodb.course.m18.ecommerce.CustomerMigrationService;
import com.mongodb.course.m18.ecommerce.CustomerService;
import com.mongodb.course.m18.ecommerce.VersionCoexistenceService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ZeroDowntimeMigrationSteps {

    private static final String COLLECTION = "m18_customers";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerMigrationService migrationService;

    @Autowired
    private VersionCoexistenceService versionCoexistenceService;

    private List<Customer> allCustomers;
    private int migratedCount;

    @Given("資料庫中存在 {int} 筆 V1 和 {int} 筆 V2 和 {int} 筆 V3 客戶")
    public void insertMixedVersionCustomers(int v1Count, int v2Count, int v3Count) {
        for (int i = 0; i < v1Count; i++) {
            versionCoexistenceService.insertRawV1Customer(
                    "V1User%d".formatted(i), "v1_%d@test.com".formatted(i), "090000000%d".formatted(i),
                    "街道%d".formatted(i), "城市%d".formatted(i), "%03d".formatted(100 + i), "TW");
        }
        for (int i = 0; i < v2Count; i++) {
            versionCoexistenceService.insertRawV2Customer(
                    "V2User%d".formatted(i), "v2_%d@test.com".formatted(i), "091000000%d".formatted(i),
                    new Address("街道%d".formatted(i), "城市%d".formatted(i), "%03d".formatted(200 + i), "TW"));
        }
        for (int i = 0; i < v3Count; i++) {
            versionCoexistenceService.insertRawV3Customer(
                    "V3User%d".formatted(i), "v3_%d@test.com".formatted(i), "092000000%d".formatted(i),
                    new Address("街道%d".formatted(i), "城市%d".formatted(i), "%03d".formatted(300 + i), "TW"),
                    "SILVER");
        }
    }

    @Given("資料庫中存在 {int} 筆 V1 和 {int} 筆 V2 客戶")
    public void insertV1AndV2Customers(int v1Count, int v2Count) {
        for (int i = 0; i < v1Count; i++) {
            versionCoexistenceService.insertRawV1Customer(
                    "V1User%d".formatted(i), "v1_%d@test.com".formatted(i), "090000000%d".formatted(i),
                    "街道%d".formatted(i), "城市%d".formatted(i), "%03d".formatted(100 + i), "TW");
        }
        for (int i = 0; i < v2Count; i++) {
            versionCoexistenceService.insertRawV2Customer(
                    "V2User%d".formatted(i), "v2_%d@test.com".formatted(i), "091000000%d".formatted(i),
                    new Address("街道%d".formatted(i), "城市%d".formatted(i), "%03d".formatted(200 + i), "TW"));
        }
    }

    @Given("資料庫中存在一筆 V1 客戶 {string} email 為 {string}")
    public void insertV1CustomerWithEmail(String name, String email) {
        versionCoexistenceService.insertRawV1Customer(
                name, email, "0900000000",
                "保存街道", "保存城市", "100", "TW");
    }

    @When("透過 CustomerService 讀取所有客戶")
    public void readAllCustomers() {
        allCustomers = customerService.findAll();
    }

    @When("執行背景批次遷移至 V3")
    public void executeBulkMigration() {
        migratedCount = migrationService.migrateAllToLatest();
    }

    @Then("應回傳 {int} 筆客戶資料")
    public void shouldReturnCount(int expected) {
        assertThat(allCustomers).hasSize(expected);
    }

    @Then("每筆資料的 loyaltyTier 均不為空")
    public void allShouldHaveLoyaltyTier() {
        assertThat(allCustomers).allSatisfy(c ->
                assertThat(c.loyaltyTier()).isNotNull().isNotEmpty());
    }

    @Then("遷移數量應為 {int}")
    public void migratedCountShouldBe(int expected) {
        assertThat(migratedCount).isEqualTo(expected);
    }

    @Then("版本統計應顯示全部為 V3")
    public void allShouldBeV3() {
        var counts = versionCoexistenceService.countPerVersion();
        assertThat(counts).containsOnlyKeys(3);
    }

    @Then("{string} 的 email 仍為 {string}")
    public void emailShouldBe(String name, String expectedEmail) {
        var customer = customerService.findByName(name);
        assertThat(customer).isNotNull();
        assertThat(customer.email()).isEqualTo(expectedEmail);
    }

    @Then("{string} 的地址資料保持完整")
    public void addressShouldBeIntact(String name) {
        var customer = customerService.findByName(name);
        assertThat(customer).isNotNull();
        assertThat(customer.address()).isNotNull();
        assertThat(customer.address().street()).isEqualTo("保存街道");
        assertThat(customer.address().city()).isEqualTo("保存城市");
    }
}
