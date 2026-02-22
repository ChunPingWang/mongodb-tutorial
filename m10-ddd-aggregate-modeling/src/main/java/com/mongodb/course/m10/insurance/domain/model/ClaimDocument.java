package com.mongodb.course.m10.insurance.domain.model;

import java.time.Instant;

public record ClaimDocument(String fileName, String documentType, Instant uploadedAt) {

    public ClaimDocument {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be blank");
        }
        if (documentType == null || documentType.isBlank()) {
            throw new IllegalArgumentException("Document type cannot be blank");
        }
        if (uploadedAt == null) {
            throw new IllegalArgumentException("Upload time cannot be null");
        }
    }
}
