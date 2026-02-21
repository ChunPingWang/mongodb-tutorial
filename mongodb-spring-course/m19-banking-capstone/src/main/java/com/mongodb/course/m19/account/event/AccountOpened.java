package com.mongodb.course.m19.account.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document("m19_account_events")
@TypeAlias("AccountOpened")
public record AccountOpened(
        @Id String eventId,
        String aggregateId,
        long version,
        Instant occurredAt,
        String accountHolder,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal initialBalance,
        String currency
) implements AccountEvent {
}
