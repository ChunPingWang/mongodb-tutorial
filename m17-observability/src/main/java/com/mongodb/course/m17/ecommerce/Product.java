package com.mongodb.course.m17.ecommerce;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("m17_products")
public record Product(
        @Id String id,
        String name,
        String category,
        long price,
        Instant createdAt
) {
    public static Product of(String name, String category, long price) {
        return new Product(null, name, category, price, Instant.now());
    }
}
