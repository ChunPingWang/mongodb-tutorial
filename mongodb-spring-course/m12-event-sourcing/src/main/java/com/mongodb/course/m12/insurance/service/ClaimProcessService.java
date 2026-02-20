package com.mongodb.course.m12.insurance.service;

import com.mongodb.course.m12.infrastructure.EventStore;
import com.mongodb.course.m12.infrastructure.SnapshotDocument;
import com.mongodb.course.m12.insurance.event.ClaimEvent;
import com.mongodb.course.m12.insurance.model.ClaimProcess;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ClaimProcessService {

    static final int SNAPSHOT_THRESHOLD = 5;
    private static final String COLLECTION = "m12_claim_events";
    private static final String AGGREGATE_TYPE = "ClaimProcess";

    private final EventStore eventStore;

    public ClaimProcessService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public ClaimProcess fileClaim(String claimId, String policyId, String claimantName,
                                  BigDecimal claimedAmount, String category) {
        var claim = ClaimProcess.file(claimId, policyId, claimantName, claimedAmount, category);
        eventStore.appendAll(claim.getUncommittedEvents().stream()
                .map(e -> (ClaimEvent) e).toList(), COLLECTION);
        claim.clearUncommittedEvents();
        return claim;
    }

    public ClaimProcess investigate(String claimId, String investigatorName, String findings) {
        var claim = loadClaim(claimId);
        claim.investigate(investigatorName, findings);
        persistAndSnapshot(claim);
        return claim;
    }

    public ClaimProcess assess(String claimId, String assessorName,
                               BigDecimal assessedAmount, String notes) {
        var claim = loadClaim(claimId);
        claim.assess(assessorName, assessedAmount, notes);
        persistAndSnapshot(claim);
        return claim;
    }

    public ClaimProcess approve(String claimId, BigDecimal approvedAmount, String approverName) {
        var claim = loadClaim(claimId);
        claim.approve(approvedAmount, approverName);
        persistAndSnapshot(claim);
        return claim;
    }

    public ClaimProcess reject(String claimId, String reason, String rejectorName) {
        var claim = loadClaim(claimId);
        claim.reject(reason, rejectorName);
        persistAndSnapshot(claim);
        return claim;
    }

    public ClaimProcess pay(String claimId, BigDecimal paidAmount, String paymentReference) {
        var claim = loadClaim(claimId);
        claim.pay(paidAmount, paymentReference);
        persistAndSnapshot(claim);
        return claim;
    }

    public ClaimProcess loadClaim(String claimId) {
        var snapshot = eventStore.loadLatestSnapshot(claimId, AGGREGATE_TYPE);
        if (snapshot.isPresent()) {
            var claim = ClaimProcess.fromSnapshot(snapshot.get().state());
            var incrementalEvents = eventStore.loadEventsAfterVersion(
                    claimId, snapshot.get().version(), ClaimEvent.class, COLLECTION);
            claim.replayAfterSnapshot(incrementalEvents);
            return claim;
        }
        var events = eventStore.loadEvents(claimId, ClaimEvent.class, COLLECTION);
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Claim not found: " + claimId);
        }
        return ClaimProcess.replayFrom(events);
    }

    public List<ClaimEvent> getEventHistory(String claimId) {
        return eventStore.loadEvents(claimId, ClaimEvent.class, COLLECTION);
    }

    private void persistAndSnapshot(ClaimProcess claim) {
        eventStore.appendAll(claim.getUncommittedEvents().stream()
                .map(e -> (ClaimEvent) e).toList(), COLLECTION);
        claim.clearUncommittedEvents();

        long eventCount = eventStore.countEvents(claim.getClaimId(), COLLECTION);
        if (eventCount >= SNAPSHOT_THRESHOLD && eventCount % SNAPSHOT_THRESHOLD == 0) {
            var snapshotDoc = new SnapshotDocument(
                    UUID.randomUUID().toString(),
                    claim.getClaimId(),
                    AGGREGATE_TYPE,
                    claim.getVersion(),
                    Instant.now(),
                    claim.toSnapshot());
            eventStore.saveSnapshot(snapshotDoc);
        }
    }
}
