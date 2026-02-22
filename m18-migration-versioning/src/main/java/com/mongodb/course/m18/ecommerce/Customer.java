package com.mongodb.course.m18.ecommerce;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("m18_customers")
public record Customer(
        @Id String id,
        String name,
        String email,
        String phone,
        Address address,
        String loyaltyTier,
        Instant registeredAt,
        int schemaVersion
) {}
