package com.mongodb.course.m08.migration;

import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class ProductV2ToV3Migrator implements DocumentMigrator {

    @Override
    public int fromVersion() {
        return 2;
    }

    @Override
    public int toVersion() {
        return 3;
    }

    @Override
    public Document migrate(Document document) {
        document.put("rating", 0.0);
        document.put("description", "");
        document.put("specifications", new Document());
        document.put("schemaVersion", 3);
        return document;
    }
}
