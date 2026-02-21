package com.mongodb.course.m15.ecommerce;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("m15_products")
public record Product(
        @Id String id,
        String name,
        String description,
        String category,
        long price,
        List<String> tags,
        boolean inStock,
        int stockQuantity,
        Instant createdAt
) {
    public static Product of(String name, String description, String category,
                              long price, List<String> tags,
                              boolean inStock, int stockQuantity) {
        return new Product(null, name, description, category, price, tags,
                inStock, stockQuantity, Instant.now());
    }
}
