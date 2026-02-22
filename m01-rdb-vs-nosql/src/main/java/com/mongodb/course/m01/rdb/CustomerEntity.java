package com.mongodb.course.m01.rdb;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * RDB representation: Normalized customer with separate order table.
 * Demonstrates Schema-on-Write and referential integrity.
 */
@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String email;

    @Embedded
    private AddressEmbeddable address;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderEntity> orders = new HashSet<>();

    protected CustomerEntity() {}

    public CustomerEntity(String name, String email, AddressEmbeddable address) {
        this.name = name;
        this.email = email;
        this.address = address;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public AddressEmbeddable getAddress() { return address; }
    public Set<OrderEntity> getOrders() { return orders; }

    public void addOrder(OrderEntity order) {
        orders.add(order);
        order.setCustomer(this);
    }
}
