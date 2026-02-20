package com.mongodb.course.m08.migration;

import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class MigrationService {

    private final List<DocumentMigrator> migrators;

    public MigrationService(List<DocumentMigrator> migrators) {
        this.migrators = migrators.stream()
                .sorted(Comparator.comparingInt(DocumentMigrator::fromVersion))
                .toList();
    }

    public Document migrateToLatest(Document document) {
        int currentVersion = document.getInteger("schemaVersion", 1);
        for (DocumentMigrator migrator : migrators) {
            if (migrator.fromVersion() == currentVersion) {
                document = migrator.migrate(document);
                currentVersion = migrator.toVersion();
            }
        }
        return document;
    }

    public Document migrateToVersion(Document document, int targetVersion) {
        int currentVersion = document.getInteger("schemaVersion", 1);
        for (DocumentMigrator migrator : migrators) {
            if (migrator.fromVersion() == currentVersion && currentVersion < targetVersion) {
                document = migrator.migrate(document);
                currentVersion = migrator.toVersion();
            }
        }
        return document;
    }
}
