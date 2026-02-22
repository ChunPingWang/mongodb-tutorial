package com.mongodb.course.m19.loan.infrastructure;

import com.mongodb.course.m19.loan.model.Applicant;
import com.mongodb.course.m19.loan.model.LoanApplication;
import com.mongodb.course.m19.loan.model.LoanStatus;

public final class LoanApplicationMapper {

    private LoanApplicationMapper() {
    }

    public static LoanApplicationDocument toDocument(LoanApplication domain) {
        return new LoanApplicationDocument(
                domain.getId(),
                domain.getApplicant().name(),
                domain.getApplicant().accountId(),
                domain.getApplicant().annualIncome(),
                domain.getRequestedAmount(),
                domain.getTermMonths(),
                domain.getStatus().name(),
                domain.getRejectionReason()
        );
    }

    public static LoanApplication toDomain(LoanApplicationDocument doc) {
        var applicant = new Applicant(doc.applicantName(), doc.accountId(), doc.annualIncome());
        return LoanApplication.reconstitute(
                doc.id(),
                applicant,
                doc.requestedAmount(),
                doc.termMonths(),
                LoanStatus.valueOf(doc.status()),
                doc.rejectionReason()
        );
    }
}
