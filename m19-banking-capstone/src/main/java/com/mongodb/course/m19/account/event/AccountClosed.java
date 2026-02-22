package com.mongodb.course.m19.account.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("m19_account_events")
@TypeAlias("AccountClosed")
public record AccountClosed(
        @Id String eventId,
        String aggregateId,
        long version,
        Instant occurredAt
) implements AccountEvent {
}
