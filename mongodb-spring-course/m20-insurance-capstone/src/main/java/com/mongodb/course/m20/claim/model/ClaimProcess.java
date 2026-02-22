package com.mongodb.course.m20.claim.model;

import com.mongodb.course.m20.claim.event.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class ClaimProcess {

    private String claimId;
    private String policyId;
    private String claimantName;
    private String category;
    private BigDecimal claimedAmount;
    private BigDecimal assessedAmount;
    private BigDecimal approvedAmount;
    private BigDecimal paidAmount;
    private String fraudRisk;
    private ClaimStatus status;
    private long version;

    private final List<ClaimEvent> uncommittedEvents = new ArrayList<>();

    private ClaimProcess() {
    }

    public static ClaimProcess file(String claimId, String policyId, String claimantName,
                                    String category, BigDecimal claimedAmount, String description) {
        if (claimedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Claimed amount must be positive");
        }
        var process = new ClaimProcess();
        var event = new ClaimFiled(
                UUID.randomUUID().toString(), claimId, 1, Instant.now(),
                policyId, claimantName, category, claimedAmount, description);
        process.apply(event);
        process.uncommittedEvents.add(event);
        return process;
    }

    public void investigate(String investigatorName, String findings, String fraudRisk) {
        if (status != ClaimStatus.FILED) {
            throw new IllegalStateException("Can only investigate FILED claims, current: " + status);
        }
        var event = new ClaimInvestigated(
                UUID.randomUUID().toString(), claimId, version + 1, Instant.now(),
                investigatorName, findings, fraudRisk);
        apply(event);
        uncommittedEvents.add(event);
    }

    public void assess(BigDecimal assessedAmount, String notes) {
        if (status != ClaimStatus.UNDER_INVESTIGATION) {
            throw new IllegalStateException("Can only assess UNDER_INVESTIGATION claims, current: " + status);
        }
        if (assessedAmount.compareTo(claimedAmount) > 0) {
            throw new IllegalStateException("Assessed amount " + assessedAmount
                    + " exceeds claimed amount " + claimedAmount);
        }
        var event = new ClaimAssessed(
                UUID.randomUUID().toString(), claimId, version + 1, Instant.now(),
                assessedAmount, notes);
        apply(event);
        uncommittedEvents.add(event);
    }

    public void approve(BigDecimal approvedAmount) {
        if (status != ClaimStatus.ASSESSED) {
            throw new IllegalStateException("Can only approve ASSESSED claims, current: " + status);
        }
        if ("HIGH".equals(fraudRisk)) {
            throw new IllegalStateException("Cannot approve claim with HIGH fraud risk");
        }
        var event = new ClaimApproved(
                UUID.randomUUID().toString(), claimId, version + 1, Instant.now(),
                approvedAmount);
        apply(event);
        uncommittedEvents.add(event);
    }

    public void reject(String reason) {
        if (status != ClaimStatus.FILED && status != ClaimStatus.UNDER_INVESTIGATION
                && status != ClaimStatus.ASSESSED) {
            throw new IllegalStateException("Can only reject FILED/UNDER_INVESTIGATION/ASSESSED claims, current: " + status);
        }
        var event = new ClaimRejected(
                UUID.randomUUID().toString(), claimId, version + 1, Instant.now(),
                reason);
        apply(event);
        uncommittedEvents.add(event);
    }

    public void pay(BigDecimal paidAmount, String paymentReference) {
        if (status != ClaimStatus.APPROVED) {
            throw new IllegalStateException("Can only pay APPROVED claims, current: " + status);
        }
        var event = new ClaimPaid(
                UUID.randomUUID().toString(), claimId, version + 1, Instant.now(),
                paidAmount, paymentReference);
        apply(event);
        uncommittedEvents.add(event);
    }

    public static ClaimProcess replayFrom(List<ClaimEvent> events) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Cannot replay from empty event list");
        }
        var process = new ClaimProcess();
        events.forEach(process::apply);
        return process;
    }

    public void replayAfterSnapshot(List<ClaimEvent> events) {
        events.forEach(this::apply);
    }

    public Map<String, Object> toSnapshot() {
        var state = new LinkedHashMap<String, Object>();
        state.put("claimId", claimId);
        state.put("policyId", policyId);
        state.put("claimantName", claimantName);
        state.put("category", category);
        state.put("claimedAmount", claimedAmount.toPlainString());
        state.put("assessedAmount", assessedAmount != null ? assessedAmount.toPlainString() : null);
        state.put("approvedAmount", approvedAmount != null ? approvedAmount.toPlainString() : null);
        state.put("paidAmount", paidAmount != null ? paidAmount.toPlainString() : null);
        state.put("fraudRisk", fraudRisk);
        state.put("status", status.name());
        state.put("version", version);
        return state;
    }

    public static ClaimProcess fromSnapshot(Map<String, Object> state) {
        var process = new ClaimProcess();
        process.claimId = (String) state.get("claimId");
        process.policyId = (String) state.get("policyId");
        process.claimantName = (String) state.get("claimantName");
        process.category = (String) state.get("category");
        process.claimedAmount = new BigDecimal((String) state.get("claimedAmount"));
        process.assessedAmount = state.get("assessedAmount") != null
                ? new BigDecimal((String) state.get("assessedAmount")) : null;
        process.approvedAmount = state.get("approvedAmount") != null
                ? new BigDecimal((String) state.get("approvedAmount")) : null;
        process.paidAmount = state.get("paidAmount") != null
                ? new BigDecimal((String) state.get("paidAmount")) : null;
        process.fraudRisk = (String) state.get("fraudRisk");
        process.status = ClaimStatus.valueOf((String) state.get("status"));
        process.version = ((Number) state.get("version")).longValue();
        return process;
    }

    private void apply(ClaimEvent event) {
        switch (event) {
            case ClaimFiled e -> {
                this.claimId = e.aggregateId();
                this.policyId = e.policyId();
                this.claimantName = e.claimantName();
                this.category = e.category();
                this.claimedAmount = e.claimedAmount();
                this.status = ClaimStatus.FILED;
            }
            case ClaimInvestigated e -> {
                this.fraudRisk = e.fraudRisk();
                this.status = ClaimStatus.UNDER_INVESTIGATION;
            }
            case ClaimAssessed e -> {
                this.assessedAmount = e.assessedAmount();
                this.status = ClaimStatus.ASSESSED;
            }
            case ClaimApproved e -> {
                this.approvedAmount = e.approvedAmount();
                this.status = ClaimStatus.APPROVED;
            }
            case ClaimRejected e -> {
                this.status = ClaimStatus.REJECTED;
            }
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
    public String getCategory() { return category; }
    public BigDecimal getClaimedAmount() { return claimedAmount; }
    public BigDecimal getAssessedAmount() { return assessedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public String getFraudRisk() { return fraudRisk; }
    public ClaimStatus getStatus() { return status; }
    public long getVersion() { return version; }
}
