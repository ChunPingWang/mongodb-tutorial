package com.mongodb.course.m11.banking.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document("m11_financial_products")
@TypeAlias("insurance_product")
public record InsuranceProduct(
        @Id String id,
        String name,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal value,
        int premiumYears,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal coverage
) implements FinancialProduct {
}
