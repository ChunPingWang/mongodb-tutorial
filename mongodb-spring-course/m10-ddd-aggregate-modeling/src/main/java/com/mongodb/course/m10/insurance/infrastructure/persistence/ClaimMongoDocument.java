package com.mongodb.course.m10.insurance.infrastructure.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document("m10_claims")
public class ClaimMongoDocument {

    @Id
    private String id;
    private String policyId;
    private String claimantId;
    private List<ItemDoc> items;
    private AssessmentDoc assessment;
    private List<AttachmentDoc> documents;
    private String status;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalClaimedAmount;
    private String totalClaimedAmountCurrency;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal deductible;
    private String deductibleCurrency;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal policyCoverage;
    private String policyCoverageCurrency;
    private Instant filedAt;
    private List<Map<String, Object>> domainEvents;

    public ClaimMongoDocument() {
    }

    // ── Embedded document types ─────────────────────────────────────────
    public static class ItemDoc {
        private String description;
        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal amount;
        private String currency;
        private String category;

        public ItemDoc() {}

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    public static class AssessmentDoc {
        private String assessorName;
        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal approvedAmount;
        private String approvedAmountCurrency;
        private String notes;

        public AssessmentDoc() {}

        public String getAssessorName() { return assessorName; }
        public void setAssessorName(String assessorName) { this.assessorName = assessorName; }
        public BigDecimal getApprovedAmount() { return approvedAmount; }
        public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }
        public String getApprovedAmountCurrency() { return approvedAmountCurrency; }
        public void setApprovedAmountCurrency(String c) { this.approvedAmountCurrency = c; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class AttachmentDoc {
        private String fileName;
        private String documentType;
        private Instant uploadedAt;

        public AttachmentDoc() {}

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public Instant getUploadedAt() { return uploadedAt; }
        public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    }

    // ── Accessors ───────────────────────────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public String getClaimantId() { return claimantId; }
    public void setClaimantId(String claimantId) { this.claimantId = claimantId; }
    public List<ItemDoc> getItems() { return items; }
    public void setItems(List<ItemDoc> items) { this.items = items; }
    public AssessmentDoc getAssessment() { return assessment; }
    public void setAssessment(AssessmentDoc assessment) { this.assessment = assessment; }
    public List<AttachmentDoc> getDocuments() { return documents; }
    public void setDocuments(List<AttachmentDoc> documents) { this.documents = documents; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalClaimedAmount() { return totalClaimedAmount; }
    public void setTotalClaimedAmount(BigDecimal totalClaimedAmount) { this.totalClaimedAmount = totalClaimedAmount; }
    public String getTotalClaimedAmountCurrency() { return totalClaimedAmountCurrency; }
    public void setTotalClaimedAmountCurrency(String c) { this.totalClaimedAmountCurrency = c; }
    public BigDecimal getDeductible() { return deductible; }
    public void setDeductible(BigDecimal deductible) { this.deductible = deductible; }
    public String getDeductibleCurrency() { return deductibleCurrency; }
    public void setDeductibleCurrency(String c) { this.deductibleCurrency = c; }
    public BigDecimal getPolicyCoverage() { return policyCoverage; }
    public void setPolicyCoverage(BigDecimal policyCoverage) { this.policyCoverage = policyCoverage; }
    public String getPolicyCoverageCurrency() { return policyCoverageCurrency; }
    public void setPolicyCoverageCurrency(String c) { this.policyCoverageCurrency = c; }
    public Instant getFiledAt() { return filedAt; }
    public void setFiledAt(Instant filedAt) { this.filedAt = filedAt; }
    public List<Map<String, Object>> getDomainEvents() { return domainEvents; }
    public void setDomainEvents(List<Map<String, Object>> domainEvents) { this.domainEvents = domainEvents; }
}
