package com.mongodb.course.m01.rdb;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * RDB representation of an insurance policy.
 * Adding a new field requires ALTER TABLE migration.
 */
@Entity
@Table(name = "insurance_policies")
public class InsurancePolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String policyNumber;

    @Column(nullable = false)
    private String holderName;

    @Column(nullable = false)
    private String policyType;

    @Column(nullable = false)
    private BigDecimal premium;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    // This field represents a schema evolution — in RDB, it requires ALTER TABLE
    @Column(name = "additional_clause")
    private String additionalClause;

    protected InsurancePolicyEntity() {}

    public InsurancePolicyEntity(String policyNumber, String holderName, String policyType,
                                  BigDecimal premium, LocalDate startDate, LocalDate endDate) {
        this.policyNumber = policyNumber;
        this.holderName = holderName;
        this.policyType = policyType;
        this.premium = premium;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() { return id; }
    public String getPolicyNumber() { return policyNumber; }
    public String getHolderName() { return holderName; }
    public String getPolicyType() { return policyType; }
    public BigDecimal getPremium() { return premium; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getAdditionalClause() { return additionalClause; }

    public void setAdditionalClause(String additionalClause) {
        this.additionalClause = additionalClause;
    }
}
