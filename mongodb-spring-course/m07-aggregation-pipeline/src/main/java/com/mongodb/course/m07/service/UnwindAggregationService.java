package com.mongodb.course.m07.service;

import com.mongodb.course.m07.dto.TagCount;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class UnwindAggregationService {

    private final MongoTemplate mongoTemplate;

    public UnwindAggregationService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<TagCount> countByTag() {
        Aggregation aggregation = newAggregation(
                unwind("tags"),
                group("tags").count().as("count"),
                project("count").and("_id").as("tag"),
                sort(Sort.Direction.DESC, "count")
        );
        AggregationResults<TagCount> results = mongoTemplate.aggregate(
                aggregation, "m07_products", TagCount.class);
        return results.getMappedResults();
    }

    public List<Map> avgPriceByTag() {
        Aggregation aggregation = newAggregation(
                unwind("tags"),
                group("tags").avg("price").as("avgPrice"),
                project("avgPrice").and("_id").as("tag"),
                sort(Sort.Direction.DESC, "avgPrice")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_products", Map.class);
        return results.getMappedResults();
    }

    public List<TagCount> topTags(int n) {
        Aggregation aggregation = newAggregation(
                unwind("tags"),
                group("tags").count().as("count"),
                project("count").and("_id").as("tag"),
                sort(Sort.Direction.DESC, "count"),
                limit(n)
        );
        AggregationResults<TagCount> results = mongoTemplate.aggregate(
                aggregation, "m07_products", TagCount.class);
        return results.getMappedResults();
    }

    public List<Map> unwindPreserveEmpty() {
        Aggregation aggregation = newAggregation(
                unwind("tags", true),
                project("name", "tags", "category")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_products", Map.class);
        return results.getMappedResults();
    }

    public List<Map> itemCountByOrder() {
        Aggregation aggregation = newAggregation(
                unwind("items"),
                group("orderNumber").count().as("itemCount"),
                project("itemCount").and("_id").as("orderNumber"),
                sort(Sort.Direction.ASC, "orderNumber")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_orders", Map.class);
        return results.getMappedResults();
    }

    public List<Map> tagsByCategory() {
        Aggregation aggregation = newAggregation(
                unwind("tags"),
                group("tags").addToSet("category").as("categories"),
                project("categories").and("_id").as("tag"),
                sort(Sort.Direction.ASC, "tag")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_products", Map.class);
        return results.getMappedResults();
    }
}
