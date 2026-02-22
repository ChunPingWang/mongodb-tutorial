package com.mongodb.course.m08.migration;

import org.bson.Document;

public interface DocumentMigrator {

    int fromVersion();

    int toVersion();

    Document migrate(Document document);
}
