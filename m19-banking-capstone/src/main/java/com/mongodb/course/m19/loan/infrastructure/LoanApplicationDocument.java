package com.mongodb.course.m19.loan.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document("m19_loan_applications")
public record LoanApplicationDocument(
        @Id String id,
        String applicantName,
        String accountId,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal annualIncome,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal requestedAmount,
        int termMonths,
        String status,
        String rejectionReason
) {
}
