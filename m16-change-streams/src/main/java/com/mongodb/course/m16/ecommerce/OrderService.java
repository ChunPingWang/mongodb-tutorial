package com.mongodb.course.m16.ecommerce;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OrderService {

    private final MongoTemplate mongoTemplate;

    public OrderService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Order create(String customerId, long totalAmount) {
        return mongoTemplate.insert(Order.of(customerId, totalAmount));
    }

    public Order transitionStatus(String id, OrderStatus newStatus) {
        var query = Query.query(Criteria.where("_id").is(id));
        var update = new Update().set("status", newStatus).set("updatedAt", Instant.now());
        mongoTemplate.updateFirst(query, update, Order.class);
        return mongoTemplate.findById(id, Order.class);
    }

    public void updateTotalAmount(String id, long newAmount) {
        var query = Query.query(Criteria.where("_id").is(id));
        var update = new Update().set("totalAmount", newAmount).set("updatedAt", Instant.now());
        mongoTemplate.updateFirst(query, update, Order.class);
    }
}
