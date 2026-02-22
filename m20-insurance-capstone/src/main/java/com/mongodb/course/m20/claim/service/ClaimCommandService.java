package com.mongodb.course.m20.claim.service;

import com.mongodb.course.m20.claim.event.ClaimEvent;
import com.mongodb.course.m20.claim.model.ClaimProcess;
import com.mongodb.course.m20.infrastructure.EventStore;
import com.mongodb.course.m20.infrastructure.SnapshotDocument;
import com.mongodb.course.m20.projection.ClaimDashboardProjector;
import com.mongodb.course.m20.projection.ClaimStatisticsProjector;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ClaimCommandService {

    private static final String CLAIM_EVENTS = "m20_claim_events";
    private static final String AGGREGATE_TYPE = "ClaimProcess";
    private static final int SNAPSHOT_INTERVAL = 10;

    private final EventStore eventStore;
    private final ClaimDashboardProjector dashboardProjector;
    private final ClaimStatisticsProjector statisticsProjector;

    public ClaimCommandService(EventStore eventStore,
                               ClaimDashboardProjector dashboardProjector,
                               ClaimStatisticsProjector statisticsProjector) {
        this.eventStore = eventStore;
        this.dashboardProjector = dashboardProjector;
        this.statisticsProjector = statisticsProjector;
    }

    public ClaimProcess fileClaim(String claimId, String policyId, String claimantName,
                                  String category, BigDecimal claimedAmount, String description) {
        var claim = ClaimProcess.file(claimId, policyId, claimantName, category, claimedAmount, description);
        saveAndProject(claim);
        return claim;
    }

    public ClaimProcess investigate(String claimId, String investigatorName,
                                    String findings, String fraudRisk) {
        var claim = loadClaim(claimId);
        claim.investigate(investigatorName, findings, fraudRisk);
        saveAndProject(claim);
        return claim;
    }

    public ClaimProcess assess(String claimId, BigDecimal assessedAmount, String notes) {
        var claim = loadClaim(claimId);
        claim.assess(assessedAmount, notes);
        saveAndProject(claim);
        return claim;
    }

    public ClaimProcess approve(String claimId, BigDecimal approvedAmount) {
        var claim = loadClaim(claimId);
        claim.approve(approvedAmount);
        saveAndProject(claim);
        return claim;
    }

    public ClaimProcess reject(String claimId, String reason) {
        var claim = loadClaim(claimId);
        claim.reject(reason);
        saveAndProject(claim);
        return claim;
    }

    public ClaimProcess pay(String claimId, BigDecimal paidAmount, String paymentReference) {
        var claim = loadClaim(claimId);
        claim.pay(paidAmount, paymentReference);
        saveAndProject(claim);
        return claim;
    }

    public ClaimProcess loadClaim(String claimId) {
        var snapshotOpt = eventStore.loadLatestSnapshot(claimId, AGGREGATE_TYPE);
        if (snapshotOpt.isPresent()) {
            var snapshot = snapshotOpt.get();
            var claim = ClaimProcess.fromSnapshot(snapshot.state());
            var newEvents = eventStore.loadEventsAfterVersion(
                    claimId, snapshot.version(), ClaimEvent.class, CLAIM_EVENTS);
            claim.replayAfterSnapshot(newEvents);
            return claim;
        }
        var events = eventStore.loadEvents(claimId, ClaimEvent.class, CLAIM_EVENTS);
        return ClaimProcess.replayFrom(events);
    }

    private void saveAndProject(ClaimProcess claim) {
        var events = List.copyOf(claim.getUncommittedEvents());
        claim.clearUncommittedEvents();

        eventStore.appendAll(events, CLAIM_EVENTS);

        for (var event : events) {
            dashboardProjector.project(event);
            statisticsProjector.project(event);
        }

        if (claim.getVersion() % SNAPSHOT_INTERVAL == 0) {
            var snapshot = new SnapshotDocument(
                    UUID.randomUUID().toString(),
                    claim.getClaimId(),
                    AGGREGATE_TYPE,
                    claim.getVersion(),
                    Instant.now(),
                    claim.toSnapshot());
            eventStore.saveSnapshot(snapshot);
        }
    }
}
