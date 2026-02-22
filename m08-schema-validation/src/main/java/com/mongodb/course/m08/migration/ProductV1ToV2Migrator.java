package com.mongodb.course.m08.migration;

import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductV1ToV2Migrator implements DocumentMigrator {

    @Override
    public int fromVersion() {
        return 1;
    }

    @Override
    public int toVersion() {
        return 2;
    }

    @Override
    public Document migrate(Document document) {
        document.put("category", "Uncategorized");
        document.put("tags", List.of());
        document.put("schemaVersion", 2);
        return document;
    }
}
