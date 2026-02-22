package com.mongodb.course.m21.order.event;

import com.mongodb.course.m21.order.model.PaymentInfo;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document("m21_order_events")
@TypeAlias("PaymentProcessed")
public record PaymentProcessed(
        @Id String eventId,
        String aggregateId,
        long version,
        Instant occurredAt,
        PaymentInfo paymentInfo,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal amount
) implements OrderEvent {
}
