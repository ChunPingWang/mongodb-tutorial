package com.mongodb.course.m20.projection;

import com.mongodb.course.m20.claim.event.*;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class ClaimStatisticsProjector {

    private static final String COLLECTION = "m20_claim_statistics";

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
                        .inc("totalClaimedAmount", new Decimal128(e.claimedAmount()));
                mongoTemplate.upsert(query, update, COLLECTION);
            }
            case ClaimInvestigated e -> {
                String category = getCategoryForClaim(e.aggregateId());
                var query = Query.query(Criteria.where("_id").is(category));
                var update = new Update().inc("investigatedCount", 1);
                mongoTemplate.upsert(query, update, COLLECTION);
            }
            case ClaimAssessed e -> {
                String category = getCategoryForClaim(e.aggregateId());
                var query = Query.query(Criteria.where("_id").is(category));
                var update = new Update().inc("assessedCount", 1);
                mongoTemplate.upsert(query, update, COLLECTION);
            }
            case ClaimApproved e -> {
                String category = getCategoryForClaim(e.aggregateId());
                var query = Query.query(Criteria.where("_id").is(category));
                var update = new Update()
                        .inc("approvedCount", 1)
                        .inc("totalApprovedAmount", new Decimal128(e.approvedAmount()));
                mongoTemplate.upsert(query, update, COLLECTION);
            }
            case ClaimRejected e -> {
                String category = getCategoryForClaim(e.aggregateId());
                var query = Query.query(Criteria.where("_id").is(category));
                var update = new Update().inc("rejectedCount", 1);
                mongoTemplate.upsert(query, update, COLLECTION);
            }
            case ClaimPaid e -> {
                String category = getCategoryForClaim(e.aggregateId());
                var query = Query.query(Criteria.where("_id").is(category));
                var update = new Update()
                        .inc("paidCount", 1)
                        .inc("totalPaidAmount", new Decimal128(e.paidAmount()));
                mongoTemplate.upsert(query, update, COLLECTION);
            }
        }
    }

    private String getCategoryForClaim(String claimId) {
        var query = Query.query(Criteria.where("_id").is(claimId));
        var dashboard = mongoTemplate.findOne(query,
                org.bson.Document.class, "m20_claim_dashboard");
        return dashboard != null ? dashboard.getString("category") : "UNKNOWN";
    }
}
