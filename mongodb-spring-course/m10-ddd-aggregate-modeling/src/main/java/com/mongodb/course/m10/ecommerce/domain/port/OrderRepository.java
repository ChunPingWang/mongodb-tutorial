package com.mongodb.course.m10.ecommerce.domain.port;

import com.mongodb.course.m10.ecommerce.domain.model.Order;

import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(String id);
}
