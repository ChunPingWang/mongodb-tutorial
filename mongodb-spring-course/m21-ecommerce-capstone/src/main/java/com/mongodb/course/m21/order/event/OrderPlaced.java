package com.mongodb.course.m21.order.event;

import com.mongodb.course.m21.order.model.OrderLine;
import com.mongodb.course.m21.order.model.ShippingAddress;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document("m21_order_events")
@TypeAlias("OrderPlaced")
public record OrderPlaced(
        @Id String eventId,
        String aggregateId,
        long version,
        Instant occurredAt,
        String customerId,
        List<OrderLine> lines,
        ShippingAddress shippingAddress,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal totalAmount
) implements OrderEvent {
}
