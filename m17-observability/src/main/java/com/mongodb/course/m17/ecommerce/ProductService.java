package com.mongodb.course.m17.ecommerce;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final MongoTemplate mongoTemplate;

    public ProductService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Product create(String name, String category, long price) {
        return mongoTemplate.save(Product.of(name, category, price));
    }

    public List<Product> findByCategory(String category) {
        var query = Query.query(Criteria.where("category").is(category));
        return mongoTemplate.find(query, Product.class);
    }

    public List<Product> findAll() {
        return mongoTemplate.findAll(Product.class);
    }
}
