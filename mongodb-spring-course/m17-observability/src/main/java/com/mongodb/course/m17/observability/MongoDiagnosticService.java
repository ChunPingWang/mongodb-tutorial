package com.mongodb.course.m17.observability;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MongoDiagnosticService {

    private final MongoTemplate mongoTemplate;

    public MongoDiagnosticService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public ServerStatusReport getServerStatus() {
        var result = mongoTemplate.getDb().runCommand(new Document("serverStatus", 1));

        var version = result.getString("version");
        var uptime = result.get("uptimeEstimate", Number.class).longValue();

        var connections = result.get("connections", Document.class);
        var current = connections.getInteger("current");
        var available = connections.getInteger("available");

        var repl = result.get("repl", Document.class);
        var replicaSetName = repl != null ? repl.getString("setName") : null;

        return new ServerStatusReport(version, uptime, current, available, replicaSetName);
    }

    public DatabaseStatsReport getDatabaseStats() {
        var result = mongoTemplate.getDb().runCommand(new Document("dbStats", 1));

        return new DatabaseStatsReport(
                result.getString("db"),
                result.get("collections", Number.class).intValue(),
                result.get("objects", Number.class).longValue(),
                result.get("dataSize", Number.class).longValue(),
                result.get("storageSize", Number.class).longValue()
        );
    }

    public CollectionStatsReport getCollectionStats(String collectionName) {
        var pipeline = List.of(
                new Document("$collStats", new Document("storageStats", new Document()))
        );

        var results = mongoTemplate.getDb()
                .getCollection(collectionName)
                .aggregate(pipeline)
                .first();

        if (results == null) {
            return new CollectionStatsReport(collectionName, 0, 0, 0);
        }

        var storageStats = results.get("storageStats", Document.class);
        return new CollectionStatsReport(
                collectionName,
                storageStats.get("count", Number.class).longValue(),
                storageStats.get("size", Number.class).longValue(),
                storageStats.get("avgObjSize", Number.class).longValue()
        );
    }
}
