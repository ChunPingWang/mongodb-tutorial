package com.mongodb.course.m18.insurance;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document("m18_policies")
public record Policy(
        @Id String id,
        String policyNumber,
        String holderName,
        String type,
        long premium,
        String status,
        LocalDate effectiveDate,
        LocalDate expirationDate,
        int riskScore,
        String region,
        int schemaVersion
) {}
