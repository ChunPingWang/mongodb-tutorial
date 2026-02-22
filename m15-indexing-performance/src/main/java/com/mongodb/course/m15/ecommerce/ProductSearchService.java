package com.mongodb.course.m15.ecommerce;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductSearchService {

    private static final String COLLECTION = "m15_products";

    private final MongoTemplate mongoTemplate;

    public ProductSearchService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<Product> textSearch(String keyword) {
        var criteria = TextCriteria.forDefaultLanguage().matching(keyword);
        var query = TextQuery.queryText(criteria).sortByScore();
        return mongoTemplate.find(query, Product.class, COLLECTION);
    }

    public List<Product> findByCategoryAndPriceRange(String category, long minPrice, long maxPrice) {
        var query = Query.query(
                Criteria.where("category").is(category)
                        .and("price").gte(minPrice).lte(maxPrice)
        );
        return mongoTemplate.find(query, Product.class, COLLECTION);
    }

    public List<Product> findByTag(String tag) {
        var query = Query.query(Criteria.where("tags").is(tag));
        return mongoTemplate.find(query, Product.class, COLLECTION);
    }

    public List<Product> findInStockByCategory(String category) {
        var query = Query.query(
                Criteria.where("inStock").is(true)
                        .and("category").is(category)
        );
        return mongoTemplate.find(query, Product.class, COLLECTION);
    }

    public List<Product> findInStockSortedByPrice(int limit) {
        var query = Query.query(Criteria.where("inStock").is(true))
                .with(Sort.by(Sort.Direction.ASC, "price"))
                .limit(limit);
        return mongoTemplate.find(query, Product.class, COLLECTION);
    }
}
