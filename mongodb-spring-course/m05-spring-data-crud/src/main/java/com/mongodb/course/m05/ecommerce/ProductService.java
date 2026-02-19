package com.mongodb.course.m05.ecommerce;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class ProductService {

    private final MongoTemplate mongoTemplate;

    public ProductService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public UpdateResult addTag(String id, String tag) {
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update().push("tags", tag).set("updatedAt", Instant.now());
        return mongoTemplate.updateFirst(query, update, Product.class);
    }

    public UpdateResult removeTag(String id, String tag) {
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update().pull("tags", tag).set("updatedAt", Instant.now());
        return mongoTemplate.updateFirst(query, update, Product.class);
    }

    public UpdateResult updatePrice(String id, BigDecimal newPrice) {
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update().set("price", newPrice).set("updatedAt", Instant.now());
        return mongoTemplate.updateFirst(query, update, Product.class);
    }

    public UpdateResult addSpecification(String id, String key, String value) {
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update().set("specifications." + key, value).set("updatedAt", Instant.now());
        return mongoTemplate.updateFirst(query, update, Product.class);
    }

    public UpdateResult markOutOfStock(String id) {
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update().set("inStock", false).set("updatedAt", Instant.now());
        return mongoTemplate.updateFirst(query, update, Product.class);
    }

    public UpdateResult upsertProduct(String sku, String name, String category, BigDecimal price) {
        Query query = Query.query(Criteria.where("sku").is(sku));
        Update update = new Update()
                .set("name", name)
                .set("category", category)
                .set("price", price)
                .set("inStock", true)
                .set("updatedAt", Instant.now())
                .setOnInsert("sku", sku)
                .setOnInsert("createdAt", Instant.now());
        return mongoTemplate.upsert(query, update, Product.class);
    }
}
