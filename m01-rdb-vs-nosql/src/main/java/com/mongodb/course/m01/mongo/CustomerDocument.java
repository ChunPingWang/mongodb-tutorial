package com.mongodb.course.m01.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB representation: Denormalized customer with embedded orders.
 * Demonstrates Schema-on-Read and "data that is accessed together is stored together".
 */
@Document(collection = "customers")
public class CustomerDocument {

    @Id
    private String id;
    private String name;
    private String email;
    private Address address;
    private List<Order> orders = new ArrayList<>();

    public CustomerDocument() {}

    public CustomerDocument(String name, String email, Address address) {
        this.name = name;
        this.email = email;
        this.address = address;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Address getAddress() { return address; }
    public List<Order> getOrders() { return orders; }

    public void setId(String id) { this.id = id; }

    public void addOrder(Order order) {
        orders.add(order);
    }

    /**
     * Embedded address — no separate collection needed.
     */
    public record Address(String street, String city, String zipCode) {}

    /**
     * Embedded order with nested items — single document read retrieves everything.
     */
    public static class Order {
        private LocalDateTime orderDate;
        private BigDecimal totalAmount;
        private List<OrderItem> items = new ArrayList<>();

        public Order() {}

        public Order(LocalDateTime orderDate, BigDecimal totalAmount) {
            this.orderDate = orderDate;
            this.totalAmount = totalAmount;
        }

        public LocalDateTime getOrderDate() { return orderDate; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public List<OrderItem> getItems() { return items; }

        public void addItem(OrderItem item) { items.add(item); }
    }

    /**
     * Embedded order item — no JOIN required.
     */
    public record OrderItem(String productName, int quantity, BigDecimal unitPrice) {}
}
