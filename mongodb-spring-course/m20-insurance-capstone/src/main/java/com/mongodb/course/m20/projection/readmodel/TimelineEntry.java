package com.mongodb.course.m20.projection.readmodel;

import java.time.Instant;

public record TimelineEntry(
        String action,
        Instant occurredAt,
        String detail
) {
}
