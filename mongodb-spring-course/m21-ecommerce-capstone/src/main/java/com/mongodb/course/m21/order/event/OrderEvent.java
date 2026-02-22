package com.mongodb.course.m21.order.event;

import com.mongodb.course.m21.shared.DomainEvent;

public sealed interface OrderEvent extends DomainEvent
        permits OrderPlaced, InventoryReserved, PaymentProcessed,
                OrderConfirmed, OrderShipped, OrderCancelled {
}
