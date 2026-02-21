package com.mongodb.course.m16.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("m16_resume_tokens")
public record ResumeTokenDocument(
        @Id String listenerName,
        String tokenJson,
        Instant savedAt
) {
}
