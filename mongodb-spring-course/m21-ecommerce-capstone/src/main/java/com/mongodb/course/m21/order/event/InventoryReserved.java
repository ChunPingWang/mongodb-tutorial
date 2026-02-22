package com.mongodb.course.m21.order.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("m21_order_events")
@TypeAlias("InventoryReserved")
public record InventoryReserved(
        @Id String eventId,
        String aggregateId,
        long version,
        Instant occurredAt,
        List<String> reservedProductIds
) implements OrderEvent {
}
