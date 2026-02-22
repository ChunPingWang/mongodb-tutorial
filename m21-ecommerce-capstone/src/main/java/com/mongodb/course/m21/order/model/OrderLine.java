package com.mongodb.course.m21.order.model;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

public record OrderLine(
        String productId,
        String productName,
        String category,
        int quantity,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal unitPrice
) {
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
