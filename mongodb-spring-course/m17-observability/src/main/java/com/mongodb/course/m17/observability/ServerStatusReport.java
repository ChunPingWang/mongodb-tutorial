package com.mongodb.course.m17.observability;

public record ServerStatusReport(
        String version,
        long uptimeSeconds,
        int currentConnections,
        int availableConnections,
        String replicaSetName
) {
}
