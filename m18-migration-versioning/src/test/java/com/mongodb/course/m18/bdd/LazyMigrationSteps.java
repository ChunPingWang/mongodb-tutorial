package com.mongodb.course.m18.bdd;

import com.mongodb.course.m18.ecommerce.Customer;
import com.mongodb.course.m18.ecommerce.CustomerService;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class LazyMigrationSteps {

    private static final String COLLECTION = "m18_customers";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CustomerService customerService;

    private Customer currentCustomer;

    @Given("資料庫中存在一筆 V1 客戶 {string} 地址為 {string}")
    public void insertV1CustomerWithAddress(String name, String street) {
        mongoTemplate.getCollection(COLLECTION).insertOne(new Document()
                .append("name", name)
                .append("email", name.toLowerCase() + "@test.com")
                .append("phone", "0900000000")
                .append("street", street)
                .append("city", "台灣")
                .append("zipCode", "100")
                .append("country", "TW")
                .append("schemaVersion", 1));
    }

    @Given("資料庫中存在一筆 V2 客戶 {string} 含嵌入式地址")
    public void insertV2Customer(String name) {
        mongoTemplate.getCollection(COLLECTION).insertOne(new Document()
                .append("name", name)
                .append("email", name.toLowerCase() + "@test.com")
                .append("phone", "0900000001")
                .append("address", new Document()
                        .append("street", "中山路一段")
                        .append("city", "台北市")
                        .append("zipCode", "104")
                        .append("country", "TW"))
                .append("schemaVersion", 2));
    }

    @When("透過 CustomerService 讀取 {string} 的資料")
    public void readCustomerByName(String name) {
        currentCustomer = customerService.findByName(name);
    }

    @When("讀取 {string} 並儲存回資料庫")
    public void readAndSave(String name) {
        currentCustomer = customerService.findByName(name);
        customerService.save(currentCustomer);
    }

    @Then("回傳的客戶地址應為嵌入式 Address 物件")
    public void addressShouldBeEmbedded() {
        assertThat(currentCustomer.address()).isNotNull();
    }

    @Then("地址街道應為 {string}")
    public void streetShouldBe(String expected) {
        assertThat(currentCustomer.address().street()).isEqualTo(expected);
    }

    @Then("loyaltyTier 應為 {string}")
    public void loyaltyTierShouldBe(String expected) {
        assertThat(currentCustomer.loyaltyTier()).isEqualTo(expected);
    }

    @Then("registeredAt 不為空")
    public void registeredAtShouldNotBeNull() {
        assertThat(currentCustomer.registeredAt()).isNotNull();
    }

    @Then("資料庫中 {string} 的 schemaVersion 應為 {int}")
    public void rawSchemaVersionShouldBe(String name, int expectedVersion) {
        var rawDoc = mongoTemplate.getCollection(COLLECTION)
                .find(new Document("name", name)).first();
        assertThat(rawDoc).isNotNull();
        assertThat(rawDoc.getInteger("schemaVersion")).isEqualTo(expectedVersion);
    }

    @Then("原始扁平地址欄位 {string} 應已移除")
    public void flatFieldShouldBeRemoved(String fieldName) {
        var rawDoc = mongoTemplate.getCollection(COLLECTION)
                .find(new Document("name", currentCustomer.name())).first();
        assertThat(rawDoc).isNotNull();
        assertThat(rawDoc.containsKey(fieldName)).isFalse();
    }
}
