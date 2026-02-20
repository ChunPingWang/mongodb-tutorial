package com.mongodb.course.m10.insurance.infrastructure.persistence;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.insurance.domain.model.*;
import com.mongodb.course.m10.shared.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ClaimMapper {

    public ClaimMongoDocument toDocument(Claim domain) {
        var doc = new ClaimMongoDocument();
        doc.setId(domain.getId());
        doc.setPolicyId(domain.getPolicyRef().policyId());
        doc.setClaimantId(domain.getClaimantRef().claimantId());
        doc.setItems(domain.getItems().stream().map(this::toItemDoc).toList());
        doc.setAssessment(domain.getAssessment() != null ? toAssessmentDoc(domain.getAssessment()) : null);
        doc.setDocuments(domain.getDocuments().stream().map(this::toAttachmentDoc).toList());
        doc.setStatus(domain.getStatus().name());
        doc.setTotalClaimedAmount(domain.getTotalClaimedAmount().amount());
        doc.setTotalClaimedAmountCurrency(domain.getTotalClaimedAmount().currency());
        doc.setDeductible(domain.getDeductible().amount());
        doc.setDeductibleCurrency(domain.getDeductible().currency());
        doc.setPolicyCoverage(domain.getPolicyCoverage().amount());
        doc.setPolicyCoverageCurrency(domain.getPolicyCoverage().currency());
        doc.setFiledAt(domain.getFiledAt());
        doc.setDomainEvents(serializeEvents(domain.getDomainEvents()));
        return doc;
    }

    public Claim toDomain(ClaimMongoDocument doc) {
        var items = doc.getItems().stream()
                .map(i -> new ClaimItem(i.getDescription(),
                        new Money(i.getAmount(), i.getCurrency()), i.getCategory()))
                .toList();

        Assessment assessment = null;
        if (doc.getAssessment() != null) {
            var a = doc.getAssessment();
            assessment = new Assessment(a.getAssessorName(),
                    new Money(a.getApprovedAmount(), a.getApprovedAmountCurrency()),
                    a.getNotes());
        }

        var documents = doc.getDocuments() != null
                ? doc.getDocuments().stream()
                    .map(d -> new ClaimDocument(d.getFileName(), d.getDocumentType(), d.getUploadedAt()))
                    .toList()
                : List.<ClaimDocument>of();

        return Claim.reconstitute(
                doc.getId(),
                new PolicyReference(doc.getPolicyId()),
                new ClaimantReference(doc.getClaimantId()),
                items, assessment, documents,
                ClaimStatus.valueOf(doc.getStatus()),
                new Money(doc.getTotalClaimedAmount(), doc.getTotalClaimedAmountCurrency()),
                new Money(doc.getDeductible(), doc.getDeductibleCurrency()),
                new Money(doc.getPolicyCoverage(), doc.getPolicyCoverageCurrency()),
                doc.getFiledAt()
        );
    }

    private ClaimMongoDocument.ItemDoc toItemDoc(ClaimItem item) {
        var doc = new ClaimMongoDocument.ItemDoc();
        doc.setDescription(item.description());
        doc.setAmount(item.amount().amount());
        doc.setCurrency(item.amount().currency());
        doc.setCategory(item.category());
        return doc;
    }

    private ClaimMongoDocument.AssessmentDoc toAssessmentDoc(Assessment a) {
        var doc = new ClaimMongoDocument.AssessmentDoc();
        doc.setAssessorName(a.assessorName());
        doc.setApprovedAmount(a.approvedAmount().amount());
        doc.setApprovedAmountCurrency(a.approvedAmount().currency());
        doc.setNotes(a.notes());
        return doc;
    }

    private ClaimMongoDocument.AttachmentDoc toAttachmentDoc(ClaimDocument d) {
        var doc = new ClaimMongoDocument.AttachmentDoc();
        doc.setFileName(d.fileName());
        doc.setDocumentType(d.documentType());
        doc.setUploadedAt(d.uploadedAt());
        return doc;
    }

    private List<Map<String, Object>> serializeEvents(List<DomainEvent> events) {
        return events.stream().map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("type", e.getClass().getSimpleName());
            map.put("aggregateId", e.aggregateId());
            map.put("occurredAt", e.occurredAt().toString());
            return map;
        }).toList();
    }
}
