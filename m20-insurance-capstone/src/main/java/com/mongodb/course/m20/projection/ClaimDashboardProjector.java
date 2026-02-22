package com.mongodb.course.m20.projection;

import com.mongodb.course.m20.claim.event.*;
import com.mongodb.course.m20.projection.readmodel.ClaimDashboardDocument;
import com.mongodb.course.m20.projection.readmodel.TimelineEntry;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClaimDashboardProjector {

    private static final String COLLECTION = "m20_claim_dashboard";

    private final MongoTemplate mongoTemplate;

    public ClaimDashboardProjector(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void project(ClaimEvent event) {
        switch (event) {
            case ClaimFiled e -> {
                var timeline = new TimelineEntry("FILED", e.occurredAt(),
                        "Claim filed: " + e.description());
                var doc = new ClaimDashboardDocument(
                        e.aggregateId(), e.policyId(), e.category(),
                        e.claimantName(), e.category(), "FILED",
                        e.claimedAmount(), null, null, null,
                        null, List.of(timeline),
                        e.occurredAt(), e.version());
                mongoTemplate.insert(doc, COLLECTION);
            }
            case ClaimInvestigated e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var timeline = new TimelineEntry("INVESTIGATED", e.occurredAt(),
                        "Investigation: " + e.findings() + " (risk: " + e.fraudRisk() + ")");
                var update = new Update()
                        .set("status", "UNDER_INVESTIGATION")
                        .set("fraudRisk", e.fraudRisk())
                        .push("timeline", timeline)
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case ClaimAssessed e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var timeline = new TimelineEntry("ASSESSED", e.occurredAt(),
                        "Assessed: " + e.assessedAmount() + " (" + e.assessmentNotes() + ")");
                var update = new Update()
                        .set("status", "ASSESSED")
                        .set("assessedAmount", new Decimal128(e.assessedAmount()))
                        .push("timeline", timeline)
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case ClaimApproved e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var timeline = new TimelineEntry("APPROVED", e.occurredAt(),
                        "Approved: " + e.approvedAmount());
                var update = new Update()
                        .set("status", "APPROVED")
                        .set("approvedAmount", new Decimal128(e.approvedAmount()))
                        .push("timeline", timeline)
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case ClaimRejected e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var timeline = new TimelineEntry("REJECTED", e.occurredAt(),
                        "Rejected: " + e.rejectionReason());
                var update = new Update()
                        .set("status", "REJECTED")
                        .push("timeline", timeline)
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case ClaimPaid e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var timeline = new TimelineEntry("PAID", e.occurredAt(),
                        "Paid: " + e.paidAmount() + " (ref: " + e.paymentReference() + ")");
                var update = new Update()
                        .set("status", "PAID")
                        .set("paidAmount", new Decimal128(e.paidAmount()))
                        .push("timeline", timeline)
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
        }
    }
}
