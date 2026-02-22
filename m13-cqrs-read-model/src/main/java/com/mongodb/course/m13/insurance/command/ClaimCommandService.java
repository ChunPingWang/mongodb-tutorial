package com.mongodb.course.m13.insurance.command;

import com.mongodb.course.m13.infrastructure.EventStore;
import com.mongodb.course.m13.insurance.event.ClaimEvent;
import com.mongodb.course.m13.insurance.projection.ClaimDashboardProjector;
import com.mongodb.course.m13.insurance.projection.ClaimStatisticsProjector;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClaimCommandService {

    private static final String COLLECTION = "m13_claim_events";

    private final EventStore eventStore;
    private final ClaimDashboardProjector claimDashboardProjector;
    private final ClaimStatisticsProjector claimStatisticsProjector;

    public ClaimCommandService(EventStore eventStore,
                               ClaimDashboardProjector claimDashboardProjector,
                               ClaimStatisticsProjector claimStatisticsProjector) {
        this.eventStore = eventStore;
        this.claimDashboardProjector = claimDashboardProjector;
        this.claimStatisticsProjector = claimStatisticsProjector;
    }

    public ClaimProcess fileClaim(String claimId, String policyId, String claimantName,
                                  BigDecimal claimedAmount, String category) {
        var claim = ClaimProcess.file(claimId, policyId, claimantName, claimedAmount, category);
        var events = persistEvents(claim);
        events.forEach(this::projectEvent);
        return claim;
    }

    public ClaimProcess investigate(String claimId, String investigatorName, String findings) {
        var claim = loadClaim(claimId);
        claim.investigate(investigatorName, findings);
        var events = persistEvents(claim);
        events.forEach(this::projectEvent);
        return claim;
    }

    public ClaimProcess assess(String claimId, String assessorName, BigDecimal assessedAmount, String notes) {
        var claim = loadClaim(claimId);
        claim.assess(assessorName, assessedAmount, notes);
        var events = persistEvents(claim);
        events.forEach(this::projectEvent);
        return claim;
    }

    public ClaimProcess approve(String claimId, BigDecimal approvedAmount, String approverName) {
        var claim = loadClaim(claimId);
        claim.approve(approvedAmount, approverName);
        var events = persistEvents(claim);
        events.forEach(this::projectEvent);
        return claim;
    }

    public ClaimProcess reject(String claimId, String reason, String rejectorName) {
        var claim = loadClaim(claimId);
        claim.reject(reason, rejectorName);
        var events = persistEvents(claim);
        events.forEach(this::projectEvent);
        return claim;
    }

    public ClaimProcess pay(String claimId, BigDecimal paidAmount, String paymentReference) {
        var claim = loadClaim(claimId);
        claim.pay(paidAmount, paymentReference);
        var events = persistEvents(claim);
        events.forEach(this::projectEvent);
        return claim;
    }

    private ClaimProcess loadClaim(String claimId) {
        var events = eventStore.loadEvents(claimId, ClaimEvent.class, COLLECTION);
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Claim not found: " + claimId);
        }
        return ClaimProcess.replayFrom(events);
    }

    private List<ClaimEvent> persistEvents(ClaimProcess claim) {
        var events = List.copyOf(claim.getUncommittedEvents());
        eventStore.appendAll(events, COLLECTION);
        claim.clearUncommittedEvents();
        return events;
    }

    private void projectEvent(ClaimEvent event) {
        claimDashboardProjector.project(event);
        claimStatisticsProjector.project(event);
    }
}
