package com.mongodb.course.m21.observability;

import java.time.Instant;

public record SlowQueryEntry(
        String commandName,
        long durationMs,
        String databaseName,
        Instant capturedAt
) {
}
