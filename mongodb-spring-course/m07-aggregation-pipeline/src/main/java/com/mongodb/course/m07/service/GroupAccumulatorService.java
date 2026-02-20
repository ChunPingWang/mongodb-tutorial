package com.mongodb.course.m07.service;

import com.mongodb.course.m07.dto.CategoryStats;
import com.mongodb.course.m07.dto.TypeStats;
import com.mongodb.course.m07.dto.TypeSum;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class GroupAccumulatorService {

    private final MongoTemplate mongoTemplate;

    public GroupAccumulatorService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<TypeStats> premiumStatsByType() {
        Aggregation aggregation = newAggregation(
                group("policyType")
                        .count().as("count")
                        .sum("premium").as("total")
                        .avg("premium").as("avg")
                        .min("premium").as("min")
                        .max("premium").as("max"),
                project("count", "total", "avg", "min", "max").and("_id").as("type")
        );
        AggregationResults<TypeStats> results = mongoTemplate.aggregate(
                aggregation, "m07_insurance_policies", TypeStats.class);
        return results.getMappedResults();
    }

    public List<Map> collectHolderNamesByType() {
        Aggregation aggregation = newAggregation(
                group("type").push("holderName").as("holderNames"),
                project("holderNames").and("_id").as("type")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", Map.class);
        return results.getMappedResults();
    }

    public List<Map> collectDistinctCategoriesByTag() {
        Aggregation aggregation = newAggregation(
                unwind("tags"),
                group("tags").addToSet("category").as("categories"),
                project("categories").and("_id").as("tag")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_products", Map.class);
        return results.getMappedResults();
    }

    public List<Map> firstAndLastByType() {
        Aggregation aggregation = newAggregation(
                sort(Sort.Direction.ASC, "balance"),
                group("type")
                        .first("holderName").as("firstHolder")
                        .last("holderName").as("lastHolder")
                        .first("balance").as("minBalance")
                        .last("balance").as("maxBalance"),
                project("firstHolder", "lastHolder", "minBalance", "maxBalance")
                        .and("_id").as("type")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", Map.class);
        return results.getMappedResults();
    }

    public List<CategoryStats> categoryStats() {
        Aggregation aggregation = newAggregation(
                group("category")
                        .count().as("count")
                        .avg("price").as("avgPrice")
                        .avg("rating").as("avgRating"),
                project("count", "avgPrice", "avgRating").and("_id").as("category")
        );
        AggregationResults<CategoryStats> results = mongoTemplate.aggregate(
                aggregation, "m07_products", CategoryStats.class);
        return results.getMappedResults();
    }

    public List<TypeSum> totalRevenueByStatus() {
        Aggregation aggregation = newAggregation(
                group("status").sum("totalAmount").as("total"),
                project("total").and("_id").as("type")
        );
        AggregationResults<TypeSum> results = mongoTemplate.aggregate(
                aggregation, "m07_orders", TypeSum.class);
        return results.getMappedResults();
    }

    public List<Map> groupByMultipleFields() {
        Aggregation aggregation = newAggregation(
                group("type", "status").count().as("count"),
                project("count")
                        .and("_id.type").as("type")
                        .and("_id.status").as("status")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", Map.class);
        return results.getMappedResults();
    }

    public Map grandTotal() {
        Aggregation aggregation = newAggregation(
                group()
                        .count().as("totalAccounts")
                        .sum("balance").as("totalBalance")
                        .avg("balance").as("avgBalance")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", Map.class);
        List<Map> mapped = results.getMappedResults();
        return mapped.isEmpty() ? Map.of() : mapped.getFirst();
    }
}
