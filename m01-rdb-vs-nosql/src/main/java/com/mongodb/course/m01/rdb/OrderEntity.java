package com.mongodb.course.m01.rdb;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * RDB representation: Normalized order with separate order_items table.
 */
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItemEntity> items = new HashSet<>();

    protected OrderEntity() {}

    public OrderEntity(LocalDateTime orderDate, BigDecimal totalAmount) {
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
    }

    public Long getId() { return id; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public CustomerEntity getCustomer() { return customer; }
    public Set<OrderItemEntity> getItems() { return items; }

    void setCustomer(CustomerEntity customer) { this.customer = customer; }

    public void addItem(OrderItemEntity item) {
        items.add(item);
        item.setOrder(this);
    }
}
