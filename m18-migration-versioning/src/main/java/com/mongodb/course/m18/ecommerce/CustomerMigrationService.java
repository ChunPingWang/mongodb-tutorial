package com.mongodb.course.m18.ecommerce;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerMigrationService {

    private static final String COLLECTION = "m18_customers";

    private final MongoTemplate mongoTemplate;

    public CustomerMigrationService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public int migrateAllToLatest() {
        var query = Query.query(Criteria.where("schemaVersion").lt(3));
        List<Customer> oldCustomers = mongoTemplate.find(query, Customer.class, COLLECTION);
        for (var customer : oldCustomers) {
            mongoTemplate.save(customer, COLLECTION);
        }
        return oldCustomers.size();
    }
}
