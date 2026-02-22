package com.mongodb.course.m21.order.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("m21_order_events")
@TypeAlias("OrderCancelled")
public record OrderCancelled(
        @Id String eventId,
        String aggregateId,
        long version,
        Instant occurredAt,
        String reason
) implements OrderEvent {
}
