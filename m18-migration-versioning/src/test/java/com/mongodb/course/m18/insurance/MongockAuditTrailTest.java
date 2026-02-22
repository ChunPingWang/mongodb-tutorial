package com.mongodb.course.m18.insurance;

import com.mongodb.course.m18.SharedContainersConfig;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
@TestPropertySource(properties = "mongock.enabled=true")
class MongockAuditTrailTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void changeLogContainsExecutedMigrations() {
        var changeLog = mongoTemplate.getCollection("mongockChangeLog");
        List<Document> entries = changeLog.find().into(new java.util.ArrayList<>());

        List<String> changeIds = entries.stream()
                .map(doc -> doc.getString("changeId"))
                .toList();

        assertThat(changeIds).contains("v001-seed-policies", "v002-add-risk-score");
    }

    @Test
    void changeLogRecordsHaveTimestampsAndAuthor() {
        var changeLog = mongoTemplate.getCollection("mongockChangeLog");
        List<Document> entries = changeLog.find(new Document("author", "m18"))
                .into(new java.util.ArrayList<>());

        assertThat(entries).isNotEmpty();
        for (var entry : entries) {
            assertThat(entry.get("timestamp")).isNotNull();
            assertThat(entry.getString("author")).isEqualTo("m18");
        }
    }
}
