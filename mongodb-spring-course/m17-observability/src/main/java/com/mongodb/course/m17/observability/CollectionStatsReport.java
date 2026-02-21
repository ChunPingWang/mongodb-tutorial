package com.mongodb.course.m17.observability;

public record CollectionStatsReport(
        String collectionName,
        long documentCount,
        long totalSizeBytes,
        long avgDocSizeBytes
) {
}
