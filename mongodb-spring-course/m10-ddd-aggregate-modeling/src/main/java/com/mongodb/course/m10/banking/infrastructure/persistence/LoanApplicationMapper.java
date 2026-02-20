package com.mongodb.course.m10.banking.infrastructure.persistence;

import com.mongodb.course.m10.banking.domain.model.Applicant;
import com.mongodb.course.m10.banking.domain.model.LoanApplication;
import com.mongodb.course.m10.banking.domain.model.LoanStatus;
import com.mongodb.course.m10.banking.domain.model.LoanTerm;
import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.shared.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LoanApplicationMapper {

    public LoanApplicationDocument toDocument(LoanApplication domain) {
        var doc = new LoanApplicationDocument();
        doc.setId(domain.getId());
        doc.setApplicantName(domain.getApplicant().name());
        doc.setApplicantNationalId(domain.getApplicant().nationalId());
        doc.setApplicantAnnualIncome(domain.getApplicant().annualIncome().amount());
        doc.setApplicantAnnualIncomeCurrency(domain.getApplicant().annualIncome().currency());
        doc.setApplicantEmployer(domain.getApplicant().employer());
        doc.setRequestedAmount(domain.getRequestedAmount().amount());
        doc.setRequestedAmountCurrency(domain.getRequestedAmount().currency());
        doc.setTermYears(domain.getTerm().years());
        doc.setAnnualInterestRate(domain.getTerm().annualInterestRate());
        doc.setStatus(domain.getStatus().name());
        doc.setReviewResult(domain.getReviewResult());
        doc.setCreatedAt(domain.getCreatedAt());
        doc.setUpdatedAt(domain.getUpdatedAt());
        doc.setDomainEvents(serializeEvents(domain.getDomainEvents()));
        return doc;
    }

    public LoanApplication toDomain(LoanApplicationDocument doc) {
        var applicant = new Applicant(
                doc.getApplicantName(),
                doc.getApplicantNationalId(),
                new Money(doc.getApplicantAnnualIncome(), doc.getApplicantAnnualIncomeCurrency()),
                doc.getApplicantEmployer()
        );
        var requestedAmount = new Money(doc.getRequestedAmount(), doc.getRequestedAmountCurrency());
        var term = new LoanTerm(doc.getTermYears(), doc.getAnnualInterestRate());
        var status = LoanStatus.valueOf(doc.getStatus());

        return LoanApplication.reconstitute(
                doc.getId(), applicant, requestedAmount, term,
                status, doc.getReviewResult(),
                doc.getCreatedAt(), doc.getUpdatedAt()
        );
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
