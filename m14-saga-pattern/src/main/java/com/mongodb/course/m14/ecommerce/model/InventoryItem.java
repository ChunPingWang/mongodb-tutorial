package com.mongodb.course.m14.ecommerce.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("m14_inventory")
public record InventoryItem(
        @Id String productId,
        String productName,
        int quantity,
        int reservedQuantity
) {
}
