package com.mongodb.course.m21.product;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductCatalogService {

    private static final String COLLECTION = "m21_products";

    private final MongoTemplate mongoTemplate;

    public ProductCatalogService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Product save(Product product) {
        return mongoTemplate.save(product, COLLECTION);
    }

    public Optional<Product> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, Product.class, COLLECTION));
    }

    public Optional<Product> findBySku(String sku) {
        var query = Query.query(Criteria.where("sku").is(sku));
        return Optional.ofNullable(mongoTemplate.findOne(query, Product.class, COLLECTION));
    }

    public <T extends Product> List<T> findByType(Class<T> type) {
        var alias = type.getAnnotation(org.springframework.data.annotation.TypeAlias.class);
        var query = Query.query(Criteria.where("_class").is(alias.value()));
        return mongoTemplate.find(query, type, COLLECTION);
    }

    public void updateStock(String productId, int quantityDelta) {
        var query = Query.query(Criteria.where("_id").is(productId));
        var update = new Update().inc("stockQuantity", quantityDelta);
        mongoTemplate.updateFirst(query, update, COLLECTION);
    }
}
