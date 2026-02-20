package com.mongodb.course.m13.insurance.projection;

import com.mongodb.course.m13.insurance.event.*;
import com.mongodb.course.m13.insurance.readmodel.ClaimDashboardDocument;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClaimStatisticsProjector {

    private static final String COLLECTION = "m13_claim_statistics";
    private static final String DASHBOARD_COLLECTION = "m13_claim_dashboards";

    private final MongoTemplate mongoTemplate;

    public ClaimStatisticsProjector(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void project(ClaimEvent event) {
        switch (event) {
            case ClaimFiled e -> {
                var query = Query.query(Criteria.where("_id").is(e.category()));
                var update = new Update()
                        .inc("totalClaims", 1)
                        .inc("filedCount", 1)
                        .inc("totalClaimedAmount", new Decimal128(e.claimedAmount()))
                        .setOnInsert("totalApprovedAmount", new Decimal128(BigDecimal.ZERO))
                        .setOnInsert("totalPaidAmount", new Decimal128(BigDecimal.ZERO))
                        .setOnInsert("approvedCount", 0)
                        .setOnInsert("rejectedCount", 0)
                        .setOnInsert("paidCount", 0)
                        .set("lastUpdatedAt", e.occurredAt());
                mongoTemplate.upsert(query, update, COLLECTION);
            }
            case ClaimApproved e -> {
                var category = lookupCategory(e.aggregateId());
                var query = Query.query(Criteria.where("_id").is(category));
                var update = new Update()
                        .inc("approvedCount", 1)
                        .inc("totalApprovedAmount", new Decimal128(e.approvedAmount()))
                        .set("lastUpdatedAt", e.occurredAt());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case ClaimRejected e -> {
                var category = lookupCategory(e.aggregateId());
                var query = Query.query(Criteria.where("_id").is(category));
                var update = new Update()
                        .inc("rejectedCount", 1)
                        .set("lastUpdatedAt", e.occurredAt());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case ClaimPaid e -> {
                var category = lookupCategory(e.aggregateId());
                var query = Query.query(Criteria.where("_id").is(category));
                var update = new Update()
                        .inc("paidCount", 1)
                        .inc("totalPaidAmount", new Decimal128(e.paidAmount()))
                        .set("lastUpdatedAt", e.occurredAt());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case ClaimInvestigated _, ClaimAssessed _ -> {
                // No statistics impact
            }
        }
    }

    public void rebuildAll(List<ClaimEvent> events) {
        mongoTemplate.remove(new Query(), COLLECTION);
        events.forEach(this::project);
    }

    private String lookupCategory(String claimId) {
        var query = Query.query(Criteria.where("_id").is(claimId));
        var dashboard = mongoTemplate.findOne(query, ClaimDashboardDocument.class, DASHBOARD_COLLECTION);
        if (dashboard == null) {
            throw new IllegalStateException("Dashboard not found for claim: " + claimId);
        }
        return dashboard.category();
    }
}
