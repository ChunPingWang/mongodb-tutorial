package com.mongodb.course.m13.insurance.command;

import com.mongodb.course.m13.insurance.event.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class ClaimProcess {

    private String claimId;
    private String policyId;
    private String claimantName;
    private BigDecimal claimedAmount;
    private String category;
    private ClaimStatus status;
    private BigDecimal assessedAmount;
    private BigDecimal approvedAmount;
    private BigDecimal paidAmount;
    private long version;

    private final List<ClaimEvent> uncommittedEvents = new ArrayList<>();

    private ClaimProcess() {
    }

    public static ClaimProcess file(String claimId, String policyId, String claimantName,
                                    BigDecimal claimedAmount, String category) {
        var process = new ClaimProcess();
        var event = new ClaimFiled(
                UUID.randomUUID().toString(), claimId, 1, Instant.now(),
                policyId, claimantName, claimedAmount, category);
        process.apply(event);
        process.uncommittedEvents.add(event);
        return process;
    }

    public ClaimEvent investigate(String investigatorName, String findings) {
        if (status != ClaimStatus.FILED) {
            throw new IllegalStateException("Cannot investigate claim in status: " + status);
        }
        var event = new ClaimInvestigated(
                UUID.randomUUID().toString(), claimId, version + 1, Instant.now(),
                investigatorName, findings);
        apply(event);
        uncommittedEvents.add(event);
        return event;
    }

    public ClaimEvent assess(String assessorName, BigDecimal assessedAmount, String notes) {
        if (status != ClaimStatus.UNDER_INVESTIGATION) {
            throw new IllegalStateException("Cannot assess claim in status: " + status);
        }
        var event = new ClaimAssessed(
                UUID.randomUUID().toString(), claimId, version + 1, Instant.now(),
                assessorName, assessedAmount, notes);
        apply(event);
        uncommittedEvents.add(event);
        return event;
    }

    public ClaimEvent approve(BigDecimal approvedAmount, String approverName) {
        if (status != ClaimStatus.ASSESSED) {
            throw new IllegalStateException("Cannot approve claim in status: " + status);
        }
        var event = new ClaimApproved(
                UUID.randomUUID().toString(), claimId, version + 1, Instant.now(),
                approvedAmount, approverName);
        apply(event);
        uncommittedEvents.add(event);
        return event;
    }

    public ClaimEvent reject(String reason, String rejectorName) {
        if (status == ClaimStatus.PAID || status == ClaimStatus.REJECTED) {
            throw new IllegalStateException("Cannot reject claim in status: " + status);
        }
        var event = new ClaimRejected(
                UUID.randomUUID().toString(), claimId, version + 1, Instant.now(),
                reason, rejectorName);
        apply(event);
        uncommittedEvents.add(event);
        return event;
    }

    public ClaimEvent pay(BigDecimal paidAmount, String paymentReference) {
        if (status != ClaimStatus.APPROVED) {
            throw new IllegalStateException("Cannot pay claim in status: " + status);
        }
        var event = new ClaimPaid(
                UUID.randomUUID().toString(), claimId, version + 1, Instant.now(),
                paidAmount, paymentReference);
        apply(event);
        uncommittedEvents.add(event);
        return event;
    }

    public static ClaimProcess replayFrom(List<ClaimEvent> events) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Cannot replay from empty event list");
        }
        var process = new ClaimProcess();
        events.forEach(process::apply);
        return process;
    }

    private void apply(ClaimEvent event) {
        switch (event) {
            case ClaimFiled e -> {
                this.claimId = e.aggregateId();
                this.policyId = e.policyId();
                this.claimantName = e.claimantName();
                this.claimedAmount = e.claimedAmount();
                this.category = e.category();
                this.status = ClaimStatus.FILED;
            }
            case ClaimInvestigated _ -> this.status = ClaimStatus.UNDER_INVESTIGATION;
            case ClaimAssessed e -> {
                this.assessedAmount = e.assessedAmount();
                this.status = ClaimStatus.ASSESSED;
            }
            case ClaimApproved e -> {
                this.approvedAmount = e.approvedAmount();
                this.status = ClaimStatus.APPROVED;
            }
            case ClaimRejected _ -> this.status = ClaimStatus.REJECTED;
            case ClaimPaid e -> {
                this.paidAmount = e.paidAmount();
                this.status = ClaimStatus.PAID;
            }
        }
        this.version = event.version();
    }

    public List<ClaimEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }

    public String getClaimId() { return claimId; }
    public String getPolicyId() { return policyId; }
    public String getClaimantName() { return claimantName; }
    public BigDecimal getClaimedAmount() { return claimedAmount; }
    public String getCategory() { return category; }
    public ClaimStatus getStatus() { return status; }
    public BigDecimal getAssessedAmount() { return assessedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public long getVersion() { return version; }
}
