package com.mongodb.course.m01.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface CustomerMongoRepository extends MongoRepository<CustomerDocument, String> {

    List<CustomerDocument> findByName(String name);

    /** Nested query: find customers who ordered a specific product */
    @Query("{ 'orders.items.productName': ?0 }")
    List<CustomerDocument> findByOrderedProduct(String productName);
}
