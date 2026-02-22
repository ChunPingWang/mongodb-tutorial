package com.mongodb.course.m10.ecommerce.application;

import com.mongodb.course.m10.ecommerce.domain.model.Order;
import com.mongodb.course.m10.ecommerce.domain.model.OrderLine;
import com.mongodb.course.m10.ecommerce.domain.model.PaymentInfo;
import com.mongodb.course.m10.ecommerce.domain.model.ShippingAddress;
import com.mongodb.course.m10.ecommerce.domain.port.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order createOrder(String orderNumber, String customerId,
                             List<OrderLine> lines, ShippingAddress address) {
        Order order = Order.create(orderNumber, customerId, lines, address);
        return repository.save(order);
    }

    public Order pay(String orderId, PaymentInfo paymentInfo) {
        Order order = findOrThrow(orderId);
        order.pay(paymentInfo);
        return repository.save(order);
    }

    public Order ship(String orderId, String trackingNumber) {
        Order order = findOrThrow(orderId);
        order.ship(trackingNumber);
        return repository.save(order);
    }

    public Order deliver(String orderId) {
        Order order = findOrThrow(orderId);
        order.deliver();
        return repository.save(order);
    }

    public Order complete(String orderId) {
        Order order = findOrThrow(orderId);
        order.complete();
        return repository.save(order);
    }

    public Order requestReturn(String orderId) {
        Order order = findOrThrow(orderId);
        order.requestReturn();
        return repository.save(order);
    }

    public Order addLine(String orderId, OrderLine line) {
        Order order = findOrThrow(orderId);
        order.addLine(line);
        return repository.save(order);
    }

    private Order findOrThrow(String orderId) {
        return repository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }
}
