package com.mongodb.course.m10.insurance.domain.model;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.insurance.domain.event.ClaimApproved;
import com.mongodb.course.m10.insurance.domain.event.ClaimFiled;
import com.mongodb.course.m10.insurance.domain.event.ClaimRejected;
import com.mongodb.course.m10.insurance.domain.specification.ClaimAmountWithinCoverageSpec;
import com.mongodb.course.m10.shared.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root — Claim.
 * Pure domain class with ZERO Spring/MongoDB dependencies.
 */
public class Claim {

    private String id;
    private PolicyReference policyRef;
    private ClaimantReference claimantRef;
    private List<ClaimItem> items;
    private Assessment assessment;
    private List<ClaimDocument> documents;
    private ClaimStatus status;
    private Money totalClaimedAmount;
    private Money deductible;
    private Money policyCoverage;
    private Instant filedAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Claim() {
    }

    // ── Business factory ────────────────────────────────────────────────
    public static Claim file(PolicyReference policyRef, ClaimantReference claimantRef,
                             List<ClaimItem> items, Money policyCoverage, Money deductible) {
        Money totalClaimed = items.stream()
                .map(ClaimItem::amount)
                .reduce(Money.twd(0), Money::add);

        if (totalClaimed.isGreaterThan(policyCoverage)) {
            throw new IllegalArgumentException(
                    "Total claimed amount exceeds policy coverage");
        }

        var claim = new Claim();
        claim.policyRef = policyRef;
        claim.claimantRef = claimantRef;
        claim.items = new ArrayList<>(items);
        claim.policyCoverage = policyCoverage;
        claim.deductible = deductible;
        claim.totalClaimedAmount = totalClaimed;
        claim.status = ClaimStatus.FILED;
        claim.documents = new ArrayList<>();
        claim.filedAt = Instant.now();
        claim.registerEvent(new ClaimFiled(
                null, policyRef.policyId(), totalClaimed, Instant.now()));
        return claim;
    }

    // ── Persistence reconstitution ──────────────────────────────────────
    public static Claim reconstitute(String id, PolicyReference policyRef, ClaimantReference claimantRef,
                                     List<ClaimItem> items, Assessment assessment,
                                     List<ClaimDocument> documents, ClaimStatus status,
                                     Money totalClaimedAmount, Money deductible, Money policyCoverage,
                                     Instant filedAt) {
        var claim = new Claim();
        claim.id = id;
        claim.policyRef = policyRef;
        claim.claimantRef = claimantRef;
        claim.items = new ArrayList<>(items);
        claim.assessment = assessment;
        claim.documents = new ArrayList<>(documents);
        claim.status = status;
        claim.totalClaimedAmount = totalClaimedAmount;
        claim.deductible = deductible;
        claim.policyCoverage = policyCoverage;
        claim.filedAt = filedAt;
        return claim;
    }

    // ── Behavior ────────────────────────────────────────────────────────
    public void assess(String assessorName, Money approvedAmount, String notes) {
        if (status != ClaimStatus.FILED && status != ClaimStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Can only assess claims in FILED or UNDER_REVIEW status, current: " + status);
        }
        var spec = new ClaimAmountWithinCoverageSpec();
        if (!spec.isSatisfiedBy(approvedAmount, totalClaimedAmount, deductible)) {
            throw new IllegalArgumentException(
                    "Approved amount exceeds claimed amount minus deductible");
        }
        this.assessment = new Assessment(assessorName, approvedAmount, notes);
        this.status = ClaimStatus.APPROVED;
        registerEvent(new ClaimApproved(id, approvedAmount, Instant.now()));
    }

    public void reject(String reason) {
        if (status != ClaimStatus.FILED && status != ClaimStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Can only reject claims in FILED or UNDER_REVIEW status, current: " + status);
        }
        this.status = ClaimStatus.REJECTED;
        registerEvent(new ClaimRejected(id, reason, Instant.now()));
    }

    public void addDocument(ClaimDocument doc) {
        if (status != ClaimStatus.FILED && status != ClaimStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Can only add documents when FILED or UNDER_REVIEW, current: " + status);
        }
        documents.add(doc);
    }

    // ── Domain events ───────────────────────────────────────────────────
    private void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // ── Accessors ───────────────────────────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public PolicyReference getPolicyRef() { return policyRef; }
    public ClaimantReference getClaimantRef() { return claimantRef; }
    public List<ClaimItem> getItems() { return Collections.unmodifiableList(items); }
    public Assessment getAssessment() { return assessment; }
    public List<ClaimDocument> getDocuments() { return Collections.unmodifiableList(documents); }
    public ClaimStatus getStatus() { return status; }
    public Money getTotalClaimedAmount() { return totalClaimedAmount; }
    public Money getDeductible() { return deductible; }
    public Money getPolicyCoverage() { return policyCoverage; }
    public Instant getFiledAt() { return filedAt; }
}
