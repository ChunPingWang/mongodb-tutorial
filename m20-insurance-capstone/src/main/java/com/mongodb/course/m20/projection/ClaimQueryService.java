package com.mongodb.course.m20.projection;

import com.mongodb.course.m20.projection.readmodel.ClaimDashboardDocument;
import com.mongodb.course.m20.projection.readmodel.ClaimStatisticsDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClaimQueryService {

    private static final String DASHBOARD = "m20_claim_dashboard";
    private static final String STATISTICS = "m20_claim_statistics";

    private final MongoTemplate mongoTemplate;

    public ClaimQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Optional<ClaimDashboardDocument> findDashboardByClaimId(String claimId) {
        return Optional.ofNullable(
                mongoTemplate.findById(claimId, ClaimDashboardDocument.class, DASHBOARD));
    }

    public List<ClaimDashboardDocument> findDashboardByCategory(String category) {
        var query = Query.query(Criteria.where("category").is(category));
        return mongoTemplate.find(query, ClaimDashboardDocument.class, DASHBOARD);
    }

    public Optional<ClaimStatisticsDocument> findStatisticsByCategory(String category) {
        return Optional.ofNullable(
                mongoTemplate.findById(category, ClaimStatisticsDocument.class, STATISTICS));
    }

    public List<ClaimStatisticsDocument> findAllStatistics() {
        return mongoTemplate.findAll(ClaimStatisticsDocument.class, STATISTICS);
    }

    public int countPaidClaimsByCategory(String category) {
        var stats = findStatisticsByCategory(category);
        return stats.map(ClaimStatisticsDocument::paidCountOrZero).orElse(0);
    }
}
