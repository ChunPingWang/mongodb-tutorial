package com.mongodb.course.m13.insurance.query;

import com.mongodb.course.m13.insurance.readmodel.ClaimDashboardDocument;
import com.mongodb.course.m13.insurance.readmodel.ClaimStatisticsDocument;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ClaimQueryService {

    private static final String DASHBOARDS = "m13_claim_dashboards";
    private static final String STATISTICS = "m13_claim_statistics";

    private final MongoTemplate mongoTemplate;

    public ClaimQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Optional<ClaimDashboardDocument> getClaimDashboard(String claimId) {
        return Optional.ofNullable(
                mongoTemplate.findById(claimId, ClaimDashboardDocument.class, DASHBOARDS));
    }

    public List<ClaimDashboardDocument> getClaimsByStatus(String status) {
        var query = Query.query(Criteria.where("currentStatus").is(status))
                .with(Sort.by(Sort.Direction.DESC, "lastUpdatedAt"));
        return mongoTemplate.find(query, ClaimDashboardDocument.class, DASHBOARDS);
    }

    public List<ClaimDashboardDocument> getClaimsByCategory(String category) {
        var query = Query.query(Criteria.where("category").is(category))
                .with(Sort.by(Sort.Direction.DESC, "lastUpdatedAt"));
        return mongoTemplate.find(query, ClaimDashboardDocument.class, DASHBOARDS);
    }

    public Optional<ClaimStatisticsDocument> getStatisticsByCategory(String category) {
        return Optional.ofNullable(
                mongoTemplate.findById(category, ClaimStatisticsDocument.class, STATISTICS));
    }

    public List<ClaimStatisticsDocument> getAllStatistics() {
        return mongoTemplate.findAll(ClaimStatisticsDocument.class, STATISTICS);
    }

    public BigDecimal getTotalApprovedAmount() {
        var aggregation = Aggregation.newAggregation(
                Aggregation.group().sum("totalApprovedAmount").as("total"));
        var result = mongoTemplate.aggregate(aggregation, STATISTICS, java.util.Map.class);
        var mapped = result.getMappedResults();
        if (mapped.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(mapped.getFirst().get("total").toString());
    }

    public List<ClaimDashboardDocument> getHighValueClaims(BigDecimal threshold) {
        var query = Query.query(Criteria.where("claimedAmount").gte(new Decimal128(threshold)))
                .with(Sort.by(Sort.Direction.DESC, "claimedAmount"));
        return mongoTemplate.find(query, ClaimDashboardDocument.class, DASHBOARDS);
    }
}
