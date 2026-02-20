package com.mongodb.course.m07.service;

import com.mongodb.course.m07.banking.AccountStatus;
import com.mongodb.course.m07.dto.CategoryStats;
import com.mongodb.course.m07.dto.TypeCount;
import com.mongodb.course.m07.dto.TypeStats;
import com.mongodb.course.m07.dto.TypeSum;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class BasicAggregationService {

    private final MongoTemplate mongoTemplate;

    public BasicAggregationService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<TypeCount> countByType(AccountStatus status) {
        Aggregation aggregation = newAggregation(
                match(Criteria.where("status").is(status)),
                group("type").count().as("count"),
                project("count").and("_id").as("type")
        );
        AggregationResults<TypeCount> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", TypeCount.class);
        return results.getMappedResults();
    }

    public List<TypeSum> sumBalanceByType() {
        Aggregation aggregation = newAggregation(
                group("type").sum("balance").as("total"),
                project("total").and("_id").as("type")
        );
        AggregationResults<TypeSum> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", TypeSum.class);
        return results.getMappedResults();
    }

    public List<Map> averageBalanceByType() {
        Aggregation aggregation = newAggregation(
                group("type").avg("balance").as("avgBalance"),
                project("avgBalance").and("_id").as("type")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", Map.class);
        return results.getMappedResults();
    }

    public List<TypeStats> statsPerType() {
        Aggregation aggregation = newAggregation(
                group("type")
                        .count().as("count")
                        .sum("balance").as("total")
                        .avg("balance").as("avg")
                        .min("balance").as("min")
                        .max("balance").as("max"),
                project("count", "total", "avg", "min", "max").and("_id").as("type")
        );
        AggregationResults<TypeStats> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", TypeStats.class);
        return results.getMappedResults();
    }

    public List<CategoryStats> sortedCategoryCounts() {
        Aggregation aggregation = newAggregation(
                group("category").count().as("count")
                        .avg("price").as("avgPrice")
                        .avg("rating").as("avgRating"),
                project("count", "avgPrice", "avgRating").and("_id").as("category"),
                sort(Sort.Direction.DESC, "count")
        );
        AggregationResults<CategoryStats> results = mongoTemplate.aggregate(
                aggregation, "m07_products", CategoryStats.class);
        return results.getMappedResults();
    }

    public List<CategoryStats> topNCategories(int n) {
        Aggregation aggregation = newAggregation(
                group("category").count().as("count")
                        .avg("price").as("avgPrice")
                        .avg("rating").as("avgRating"),
                project("count", "avgPrice", "avgRating").and("_id").as("category"),
                sort(Sort.Direction.DESC, "count"),
                limit(n)
        );
        AggregationResults<CategoryStats> results = mongoTemplate.aggregate(
                aggregation, "m07_products", CategoryStats.class);
        return results.getMappedResults();
    }

    public List<Map> projectComputedField() {
        Aggregation aggregation = newAggregation(
                match(Criteria.where("status").is(AccountStatus.ACTIVE)),
                project("accountNumber", "holderName", "balance")
                        .andExpression("cond(balance >= 50000, 'HIGH', 'NORMAL')").as("balanceTier")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", Map.class);
        return results.getMappedResults();
    }

    public long countDocuments(AccountStatus status) {
        Aggregation aggregation = newAggregation(
                match(Criteria.where("status").is(status)),
                count().as("total")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", Map.class);
        List<Map> mapped = results.getMappedResults();
        if (mapped.isEmpty()) {
            return 0;
        }
        return ((Number) mapped.getFirst().get("total")).longValue();
    }
}
