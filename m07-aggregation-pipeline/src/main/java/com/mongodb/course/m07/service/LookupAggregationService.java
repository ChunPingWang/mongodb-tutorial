package com.mongodb.course.m07.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class LookupAggregationService {

    private final MongoTemplate mongoTemplate;

    public LookupAggregationService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<Map> ordersWithProductDetails() {
        Aggregation aggregation = newAggregation(
                unwind("items"),
                lookup("m07_products", "items.sku", "sku", "productDetails"),
                project("orderNumber", "customerName", "items", "productDetails")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_orders", Map.class);
        return results.getMappedResults();
    }

    public List<Map> ordersWithProductCount() {
        Aggregation aggregation = newAggregation(
                unwind("items"),
                lookup("m07_products", "items.sku", "sku", "matchedProducts"),
                group("_id")
                        .first("orderNumber").as("orderNumber")
                        .first("customerName").as("customerName")
                        .sum(ConditionalOperators
                                .when(Criteria.where("matchedProducts").ne(List.of()))
                                .then(1).otherwise(0))
                        .as("matchedProductCount")
                        .count().as("totalItems")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_orders", Map.class);
        return results.getMappedResults();
    }

    public List<Map> customerOrderSummary() {
        Aggregation aggregation = newAggregation(
                group("customerName")
                        .count().as("orderCount")
                        .sum("totalAmount").as("totalSpent"),
                project("orderCount", "totalSpent").and("_id").as("customerName"),
                sort(Sort.Direction.DESC, "totalSpent")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_orders", Map.class);
        return results.getMappedResults();
    }

    public List<Map> pipelineLookup() {
        Aggregation aggregation = newAggregation(
                unwind("items"),
                lookup()
                        .from("m07_products")
                        .localField("items.sku")
                        .foreignField("sku")
                        .as("productInfo"),
                unwind("productInfo", true),
                project("orderNumber", "customerName")
                        .and("items.sku").as("sku")
                        .and("items.quantity").as("quantity")
                        .and("items.unitPrice").as("unitPrice")
                        .and("productInfo.name").as("productName")
                        .and("productInfo.category").as("productCategory")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_orders", Map.class);
        return results.getMappedResults();
    }

    public List<Map> lookupAndProject() {
        Aggregation aggregation = newAggregation(
                lookup("m07_products", "items.sku", "sku", "matchedProducts"),
                project("orderNumber", "customerName", "totalAmount", "status")
                        .and("matchedProducts.name").as("productNames")
                        .and("matchedProducts.category").as("productCategories")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_orders", Map.class);
        return results.getMappedResults();
    }

    public List<Map> lookupWithUnwind() {
        Aggregation aggregation = newAggregation(
                unwind("items"),
                lookup("m07_products", "items.sku", "sku", "product"),
                unwind("product", true),
                project("orderNumber", "customerName")
                        .and("items.sku").as("sku")
                        .and("items.quantity").as("quantity")
                        .and("product.name").as("productName")
                        .and("product.price").as("productPrice")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_orders", Map.class);
        return results.getMappedResults();
    }
}
