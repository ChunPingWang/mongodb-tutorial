package com.mongodb.course.m18.ecommerce;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private static final String COLLECTION = "m18_customers";

    private final MongoTemplate mongoTemplate;

    public CustomerService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Customer findByName(String name) {
        var query = Query.query(Criteria.where("name").is(name));
        return mongoTemplate.findOne(query, Customer.class, COLLECTION);
    }

    public List<Customer> findAll() {
        return mongoTemplate.findAll(Customer.class, COLLECTION);
    }

    public Customer save(Customer customer) {
        return mongoTemplate.save(customer, COLLECTION);
    }
}
