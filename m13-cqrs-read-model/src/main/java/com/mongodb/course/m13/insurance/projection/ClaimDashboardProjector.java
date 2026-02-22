package com.mongodb.course.m13.insurance.projection;

import com.mongodb.course.m13.insurance.event.*;
import com.mongodb.course.m13.insurance.readmodel.ClaimDashboardDocument;
import com.mongodb.course.m13.insurance.readmodel.ClaimDashboardDocument.TimelineEntry;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClaimDashboardProjector {

    private static final String COLLECTION = "m13_claim_dashboards";

    private final MongoTemplate mongoTemplate;

    public ClaimDashboardProjector(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void project(ClaimEvent event) {
        switch (event) {
            case ClaimFiled e -> {
                var timeline = List.of(new TimelineEntry("ClaimFiled", e.occurredAt(),
                        "Claim filed by " + e.claimantName() + " for amount " + e.claimedAmount()));
                var doc = new ClaimDashboardDocument(
                        e.aggregateId(),
                        e.policyId(),
                        e.claimantName(),
                        e.claimedAmount(),
                        e.category(),
                        "FILED",
                        null, null, null,
                        null, null, null,
                        e.occurredAt(),
                        e.occurredAt(),
                        1,
                        e.version(),
                        timeline);
                mongoTemplate.insert(doc, COLLECTION);
            }
            case ClaimInvestigated e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var entry = new TimelineEntry("ClaimInvestigated", e.occurredAt(),
                        "Investigated by " + e.investigatorName());
                var update = new Update()
                        .set("currentStatus", "UNDER_INVESTIGATION")
                        .set("investigatorName", e.investigatorName())
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version())
                        .inc("eventCount", 1)
                        .push("timeline", entry);
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case ClaimAssessed e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var entry = new TimelineEntry("ClaimAssessed", e.occurredAt(),
                        "Assessed by " + e.assessorName() + " for amount " + e.assessedAmount());
                var update = new Update()
                        .set("currentStatus", "ASSESSED")
                        .set("assessorName", e.assessorName())
                        .set("assessedAmount", e.assessedAmount())
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version())
                        .inc("eventCount", 1)
                        .push("timeline", entry);
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case ClaimApproved e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var entry = new TimelineEntry("ClaimApproved", e.occurredAt(),
                        "Approved by " + e.approverName() + " for amount " + e.approvedAmount());
                var update = new Update()
                        .set("currentStatus", "APPROVED")
                        .set("approverName", e.approverName())
                        .set("approvedAmount", e.approvedAmount())
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version())
                        .inc("eventCount", 1)
                        .push("timeline", entry);
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case ClaimRejected e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var entry = new TimelineEntry("ClaimRejected", e.occurredAt(),
                        "Rejected: " + e.reason());
                var update = new Update()
                        .set("currentStatus", "REJECTED")
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version())
                        .inc("eventCount", 1)
                        .push("timeline", entry);
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case ClaimPaid e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var entry = new TimelineEntry("ClaimPaid", e.occurredAt(),
                        "Paid amount " + e.paidAmount() + " ref: " + e.paymentReference());
                var update = new Update()
                        .set("currentStatus", "PAID")
                        .set("paidAmount", e.paidAmount())
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version())
                        .inc("eventCount", 1)
                        .push("timeline", entry);
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
        }
    }

    public void rebuildAll(List<ClaimEvent> events) {
        mongoTemplate.remove(new Query(), COLLECTION);
        events.forEach(this::project);
    }
}
