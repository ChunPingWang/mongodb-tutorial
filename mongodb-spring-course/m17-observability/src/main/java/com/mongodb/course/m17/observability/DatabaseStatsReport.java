package com.mongodb.course.m17.observability;

public record DatabaseStatsReport(
        String databaseName,
        int collections,
        long documents,
        long dataSizeBytes,
        long storageSizeBytes
) {
}
