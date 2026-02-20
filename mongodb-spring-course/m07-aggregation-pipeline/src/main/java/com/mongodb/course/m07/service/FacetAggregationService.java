package com.mongodb.course.m07.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class FacetAggregationService {

    private final MongoTemplate mongoTemplate;

    public FacetAggregationService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Map productSearchFacet(String category) {
        FacetOperation facet = facet(
                count().as("total")
        ).as("totalCount")
                .and(
                        sort(Sort.Direction.ASC, "name"),
                        skip(0L),
                        limit(10)
                ).as("data")
                .and(
                        group()
                                .avg("price").as("avgPrice")
                                .min("price").as("minPrice")
                                .max("price").as("maxPrice")
                ).as("stats");

        Aggregation aggregation;
        if (category != null) {
            aggregation = newAggregation(
                    match(Criteria.where("category").is(category)),
                    facet
            );
        } else {
            aggregation = newAggregation(facet);
        }

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_products", Map.class);
        return results.getMappedResults().isEmpty() ? Map.of() : results.getMappedResults().getFirst();
    }

    public Map bankingDashboard() {
        FacetOperation facet = facet(
                group("status").count().as("count"),
                project("count").and("_id").as("status")
        ).as("statusCounts")
                .and(
                        group("type").count().as("count"),
                        project("count").and("_id").as("type")
                ).as("typeCounts")
                .and(
                        group()
                                .sum("balance").as("totalBalance")
                                .avg("balance").as("avgBalance")
                                .min("balance").as("minBalance")
                                .max("balance").as("maxBalance")
                ).as("balanceStats");

        Aggregation aggregation = newAggregation(facet);
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", Map.class);
        return results.getMappedResults().isEmpty() ? Map.of() : results.getMappedResults().getFirst();
    }

    public Map insuranceOverview() {
        FacetOperation facet = facet(
                group("policyType").count().as("count"),
                project("count").and("_id").as("policyType")
        ).as("byType")
                .and(
                        group("status").count().as("count"),
                        project("count").and("_id").as("status")
                ).as("byStatus")
                .and(
                        group()
                                .sum("premium").as("totalPremium")
                                .avg("premium").as("avgPremium")
                                .min("premium").as("minPremium")
                                .max("premium").as("maxPremium")
                ).as("premiumRange");

        Aggregation aggregation = newAggregation(facet);
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_insurance_policies", Map.class);
        return results.getMappedResults().isEmpty() ? Map.of() : results.getMappedResults().getFirst();
    }

    public Map orderAnalytics() {
        FacetOperation facet = facet(
                group("status").count().as("count"),
                project("count").and("_id").as("status")
        ).as("byStatus")
                .and(
                        group("customerName")
                                .count().as("orderCount")
                                .sum("totalAmount").as("totalSpent"),
                        project("orderCount", "totalSpent").and("_id").as("customerName"),
                        sort(Sort.Direction.DESC, "totalSpent"),
                        limit(3)
                ).as("topCustomers");

        Aggregation aggregation = newAggregation(facet);
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_orders", Map.class);
        return results.getMappedResults().isEmpty() ? Map.of() : results.getMappedResults().getFirst();
    }

    public Map facetWithMatch(String category) {
        FacetOperation facet = facet(
                count().as("total")
        ).as("totalCount")
                .and(
                        sort(Sort.Direction.ASC, "name"),
                        limit(5)
                ).as("data")
                .and(
                        group()
                                .avg("price").as("avgPrice")
                                .min("price").as("minPrice")
                                .max("price").as("maxPrice")
                ).as("priceStats");

        Aggregation aggregation = newAggregation(
                match(Criteria.where("category").is(category)),
                facet
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_products", Map.class);
        return results.getMappedResults().isEmpty() ? Map.of() : results.getMappedResults().getFirst();
    }

    public Map facetCombinedSearch(String category, boolean inStockOnly, int page, int pageSize) {
        Criteria criteria = Criteria.where("category").is(category);
        if (inStockOnly) {
            criteria = criteria.and("inStock").is(true);
        }

        FacetOperation facet = facet(
                count().as("total")
        ).as("totalCount")
                .and(
                        sort(Sort.Direction.ASC, "price"),
                        skip((long) page * pageSize),
                        limit(pageSize)
                ).as("data")
                .and(
                        group()
                                .count().as("count")
                                .avg("price").as("avgPrice")
                                .min("price").as("minPrice")
                                .max("price").as("maxPrice")
                                .avg("rating").as("avgRating")
                ).as("stats");

        Aggregation aggregation = newAggregation(
                match(criteria),
                facet
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_products", Map.class);
        return results.getMappedResults().isEmpty() ? Map.of() : results.getMappedResults().getFirst();
    }
}
