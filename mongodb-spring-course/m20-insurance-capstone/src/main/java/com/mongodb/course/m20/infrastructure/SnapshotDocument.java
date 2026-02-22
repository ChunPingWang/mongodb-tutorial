package com.mongodb.course.m20.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document("m20_snapshots")
public record SnapshotDocument(
        @Id String id,
        String aggregateId,
        String aggregateType,
        long version,
        Instant createdAt,
        Map<String, Object> state
) {}
