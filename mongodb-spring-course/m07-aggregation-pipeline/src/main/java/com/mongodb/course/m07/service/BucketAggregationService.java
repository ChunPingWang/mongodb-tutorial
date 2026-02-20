package com.mongodb.course.m07.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.BucketAutoOperation;
import org.springframework.data.mongodb.core.aggregation.BucketOperation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class BucketAggregationService {

    private final MongoTemplate mongoTemplate;

    public BucketAggregationService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<Map> balanceDistribution() {
        BucketOperation bucket = bucket("balance")
                .withBoundaries(0, 20000, 50000, 100000, Integer.MAX_VALUE)
                .withDefaultBucket("other")
                .andOutput("balance").count().as("count");

        Aggregation aggregation = newAggregation(bucket);
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", Map.class);
        return results.getMappedResults();
    }

    public List<Map> premiumBucketAuto(int buckets) {
        BucketAutoOperation bucketAuto = bucketAuto("premium", buckets)
                .andOutput("premium").count().as("count")
                .andOutput("premium").avg().as("avgPremium");

        Aggregation aggregation = newAggregation(bucketAuto);
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_insurance_policies", Map.class);
        return results.getMappedResults();
    }

    public List<Map> priceTierDistribution() {
        BucketOperation bucket = bucket("price")
                .withBoundaries(0, 50, 100, 200, 500, Integer.MAX_VALUE)
                .withDefaultBucket("other")
                .andOutput("price").count().as("count")
                .andOutput("price").avg().as("avgPrice");

        Aggregation aggregation = newAggregation(bucket);
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_products", Map.class);
        return results.getMappedResults();
    }

    public List<Map> ratingDistribution() {
        BucketOperation bucket = bucket("rating")
                .withBoundaries(1.0, 2.0, 3.0, 4.0, 5.01)
                .withDefaultBucket("other")
                .andOutput("rating").count().as("count")
                .andOutput("rating").avg().as("avgRating");

        Aggregation aggregation = newAggregation(bucket);
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_products", Map.class);
        return results.getMappedResults();
    }

    public List<Map> bucketWithAccumulator() {
        BucketOperation bucket = bucket("balance")
                .withBoundaries(0, 30000, 60000, Integer.MAX_VALUE)
                .withDefaultBucket("other")
                .andOutput("balance").count().as("count")
                .andOutput("balance").avg().as("avgBalance")
                .andOutput("balance").sum().as("totalBalance");

        Aggregation aggregation = newAggregation(bucket);
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", Map.class);
        return results.getMappedResults();
    }

    public List<Map> bucketAutoGranularity(int buckets) {
        BucketAutoOperation bucketAuto = bucketAuto("balance", buckets)
                .withGranularity(BucketAutoOperation.Granularities.POWERSOF2)
                .andOutput("balance").count().as("count");

        Aggregation aggregation = newAggregation(bucketAuto);
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", Map.class);
        return results.getMappedResults();
    }
}
