package com.mongodb.course.m01;

import com.mongodb.course.m01.mongo.CustomerDocument;
import com.mongodb.course.m01.mongo.CustomerDocument.Address;
import com.mongodb.course.m01.mongo.CustomerDocument.Order;
import com.mongodb.course.m01.mongo.CustomerDocument.OrderItem;
import com.mongodb.course.m01.mongo.CustomerMongoRepository;
import com.mongodb.course.m01.rdb.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M01-LAB-01: Side-by-side comparison of PostgreSQL (RDB) and MongoDB (Document DB).
 * Demonstrates the fundamental differences in data modeling and querying.
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
class RdbVsMongoComparisonTest {

    @Autowired
    private CustomerJpaRepository jpaRepository;

    @Autowired
    private CustomerMongoRepository mongoRepository;

    @BeforeEach
    void cleanUp() {
        jpaRepository.deleteAll();
        mongoRepository.deleteAll();
    }

    @Test
    @DisplayName("RDB: Normalized model requires 3 tables for customer-order data")
    void rdbNormalizedModel() {
        // RDB approach: Data split across customers, orders, order_items tables
        var address = new AddressEmbeddable("100 Main St", "Taipei", "10001");
        var customer = new CustomerEntity("Alice", "alice@test.com", address);

        var order = new OrderEntity(LocalDateTime.now(), new BigDecimal("36000"));
        order.addItem(new OrderItemEntity("Laptop", 1, new BigDecimal("35000")));
        order.addItem(new OrderItemEntity("Mouse", 2, new BigDecimal("500")));
        customer.addOrder(order);

        jpaRepository.save(customer);

        // Reading requires JOIN across tables
        List<CustomerEntity> found = jpaRepository.findByNameWithOrdersAndItems("Alice");
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getOrders()).hasSize(1);
        assertThat(found.getFirst().getOrders().iterator().next().getItems()).hasSize(2);
    }

    @Test
    @DisplayName("MongoDB: Denormalized document stores everything in one place")
    void mongoDbDenormalizedModel() {
        // MongoDB approach: Single document contains all related data
        var customer = new CustomerDocument("Alice", "alice@test.com",
                new Address("100 Main St", "Taipei", "10001"));

        var order = new Order(LocalDateTime.now(), new BigDecimal("36000"));
        order.addItem(new OrderItem("Laptop", 1, new BigDecimal("35000")));
        order.addItem(new OrderItem("Mouse", 2, new BigDecimal("500")));
        customer.addOrder(order);

        mongoRepository.save(customer);

        // Single document read — no JOINs needed
        List<CustomerDocument> found = mongoRepository.findByName("Alice");
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getOrders()).hasSize(1);
        assertThat(found.getFirst().getOrders().getFirst().getItems()).hasSize(2);
    }

    @Test
    @DisplayName("Nested query: RDB requires multi-table JOIN vs MongoDB queries embedded documents")
    void nestedQueryComparison() {
        // Setup: Create customer with orders in both databases
        seedRdbData();
        seedMongoData();

        // RDB: Finding customers who ordered "Laptop" requires JOIN across 3 tables
        List<CustomerEntity> rdbResults = jpaRepository.findByNameWithOrdersAndItems("Alice");
        assertThat(rdbResults).isNotEmpty();
        boolean hasLaptop = rdbResults.getFirst().getOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .anyMatch(item -> item.getProductName().equals("Laptop"));
        assertThat(hasLaptop).isTrue();

        // MongoDB: Direct nested query on embedded documents
        List<CustomerDocument> mongoResults = mongoRepository.findByOrderedProduct("Laptop");
        assertThat(mongoResults).hasSize(1);
        assertThat(mongoResults.getFirst().getName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("Demonstrate data model flexibility: MongoDB stores heterogeneous structures")
    void mongoFlexibleSchema() {
        // MongoDB allows different document structures in the same collection
        var customer1 = new CustomerDocument("Bob", "bob@test.com",
                new Address("200 Oak Ave", "Kaohsiung", "80001"));
        mongoRepository.save(customer1);

        var customer2 = new CustomerDocument("Carol", "carol@test.com", null);
        var order = new Order(LocalDateTime.now(), new BigDecimal("5000"));
        order.addItem(new OrderItem("Keyboard", 1, new BigDecimal("5000")));
        customer2.addOrder(order);
        mongoRepository.save(customer2);

        // Both documents coexist without schema enforcement
        assertThat(mongoRepository.findAll()).hasSize(2);
        assertThat(mongoRepository.findByName("Bob").getFirst().getAddress()).isNotNull();
        assertThat(mongoRepository.findByName("Carol").getFirst().getAddress()).isNull();
    }

    private void seedRdbData() {
        var address = new AddressEmbeddable("100 Main St", "Taipei", "10001");
        var customer = new CustomerEntity("Alice", "alice@test.com", address);
        var order = new OrderEntity(LocalDateTime.now(), new BigDecimal("36000"));
        order.addItem(new OrderItemEntity("Laptop", 1, new BigDecimal("35000")));
        order.addItem(new OrderItemEntity("Mouse", 2, new BigDecimal("500")));
        customer.addOrder(order);
        jpaRepository.save(customer);
    }

    private void seedMongoData() {
        var customer = new CustomerDocument("Alice", "alice@test.com",
                new Address("100 Main St", "Taipei", "10001"));
        var order = new Order(LocalDateTime.now(), new BigDecimal("36000"));
        order.addItem(new OrderItem("Laptop", 1, new BigDecimal("35000")));
        order.addItem(new OrderItem("Mouse", 2, new BigDecimal("500")));
        customer.addOrder(order);
        mongoRepository.save(customer);
    }
}
