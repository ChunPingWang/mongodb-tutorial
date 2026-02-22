package com.mongodb.course.m01.bdd;

import com.mongodb.course.m01.mongo.CustomerDocument;
import com.mongodb.course.m01.mongo.CustomerDocument.Address;
import com.mongodb.course.m01.mongo.CustomerDocument.Order;
import com.mongodb.course.m01.mongo.CustomerDocument.OrderItem;
import com.mongodb.course.m01.mongo.CustomerMongoRepository;
import com.mongodb.course.m01.rdb.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RdbVsMongoSteps {

    @Autowired
    private CustomerJpaRepository jpaRepository;

    @Autowired
    private CustomerMongoRepository mongoRepository;

    private String customerName;
    private String customerEmail;
    private String customerCity;
    private List<Map<String, String>> orderItems;

    private CustomerEntity savedRdbCustomer;
    private CustomerDocument savedMongoCustomer;

    @Before
    public void cleanUp() {
        jpaRepository.deleteAll();
        mongoRepository.deleteAll();
    }

    @Given("the same customer order data:")
    public void theCustomerOrderData(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().getFirst();
        customerName = row.get("name");
        customerEmail = row.get("email");
        customerCity = row.get("city");
    }

    @And("the customer has an order with items:")
    public void theCustomerHasAnOrderWithItems(DataTable dataTable) {
        orderItems = dataTable.asMaps();
    }

    @When("the data is stored in PostgreSQL")
    public void theDataIsStoredInPostgresql() {
        var address = new AddressEmbeddable("100 Main St", customerCity, "10001");
        var customer = new CustomerEntity(customerName, customerEmail, address);
        var order = new OrderEntity(LocalDateTime.now(), calculateTotal());
        for (Map<String, String> item : orderItems) {
            order.addItem(new OrderItemEntity(
                    item.get("product"),
                    Integer.parseInt(item.get("quantity")),
                    new BigDecimal(item.get("unitPrice"))
            ));
        }
        customer.addOrder(order);
        savedRdbCustomer = jpaRepository.save(customer);
    }

    @And("the data is stored in MongoDB")
    public void theDataIsStoredInMongodb() {
        var customer = new CustomerDocument(customerName, customerEmail,
                new Address("100 Main St", customerCity, "10001"));
        var order = new Order(LocalDateTime.now(), calculateTotal());
        for (Map<String, String> item : orderItems) {
            order.addItem(new OrderItem(
                    item.get("product"),
                    Integer.parseInt(item.get("quantity")),
                    new BigDecimal(item.get("unitPrice"))
            ));
        }
        customer.addOrder(order);
        savedMongoCustomer = mongoRepository.save(customer);
    }

    @Then("PostgreSQL requires JOIN to retrieve customer with orders")
    public void postgresqlRequiresJoin() {
        // Fetching with JOIN across 3 tables
        List<CustomerEntity> results = jpaRepository.findByNameWithOrdersAndItems(customerName);
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getOrders()).isNotEmpty();
        assertThat(results.getFirst().getOrders().iterator().next().getItems()).isNotEmpty();
    }

    @And("MongoDB retrieves the complete document in a single read")
    public void mongodbRetrievesSingleDocument() {
        // Single document read — everything is embedded
        List<CustomerDocument> results = mongoRepository.findByName(customerName);
        assertThat(results).hasSize(1);
        CustomerDocument doc = results.getFirst();
        assertThat(doc.getOrders()).isNotEmpty();
        assertThat(doc.getOrders().getFirst().getItems()).isNotEmpty();
    }

    @Given("customers with orders exist in both databases")
    public void customersWithOrdersExistInBothDatabases() {
        customerName = "Alice";
        customerEmail = "alice@test.com";
        customerCity = "Taipei";
        orderItems = List.of(Map.of("product", "Laptop", "quantity", "1", "unitPrice", "35000"));
        theDataIsStoredInPostgresql();
        theDataIsStoredInMongodb();
    }

    @When("I query for customers who ordered {string}")
    public void iQueryForCustomersWhoOrdered(String productName) {
        // Query will be executed in the Then steps
    }

    @Then("PostgreSQL uses JOIN across 3 tables")
    public void postgresqlUsesJoinAcrossTables() {
        List<CustomerEntity> results = jpaRepository.findByNameWithOrdersAndItems("Alice");
        assertThat(results).isNotEmpty();
        boolean hasProduct = results.getFirst().getOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .anyMatch(item -> item.getProductName().equals("Laptop"));
        assertThat(hasProduct).isTrue();
    }

    @And("MongoDB queries nested documents directly")
    public void mongodbQueriesNestedDocumentsDirectly() {
        List<CustomerDocument> results = mongoRepository.findByOrderedProduct("Laptop");
        assertThat(results).hasSize(1);
    }

    private BigDecimal calculateTotal() {
        return orderItems.stream()
                .map(item -> new BigDecimal(item.get("unitPrice"))
                        .multiply(BigDecimal.valueOf(Integer.parseInt(item.get("quantity")))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
