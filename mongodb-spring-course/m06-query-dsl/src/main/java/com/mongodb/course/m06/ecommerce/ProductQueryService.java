package com.mongodb.course.m06.ecommerce;

import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductQueryService {

    private final MongoTemplate mongoTemplate;

    public ProductQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // TextCriteria.forDefaultLanguage().matchingAny()
    public List<Product> textSearch(String... terms) {
        TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingAny(terms);
        Query query = TextQuery.queryText(textCriteria);
        return mongoTemplate.find(query, Product.class);
    }

    // matchingPhrase()
    public List<Product> textSearchPhrase(String phrase) {
        TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingPhrase(phrase);
        Query query = TextQuery.queryText(textCriteria);
        return mongoTemplate.find(query, Product.class);
    }

    // TextQuery.sortByScore()
    public List<Product> textSearchWithScoreSort(String... terms) {
        TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingAny(terms);
        Query query = TextQuery.queryText(textCriteria).sortByScore();
        return mongoTemplate.find(query, Product.class);
    }

    // TextCriteria + regular Criteria
    public List<Product> textSearchInCategory(String term, String category) {
        TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching(term);
        Query query = TextQuery.queryText(textCriteria)
                .addCriteria(Criteria.where("category").is(category));
        return mongoTemplate.find(query, Product.class);
    }

    // TextCriteria.notMatching()
    public List<Product> textSearchExcluding(String includeTerm, String excludeTerm) {
        TextCriteria textCriteria = TextCriteria.forDefaultLanguage()
                .matching(includeTerm)
                .notMatching(excludeTerm);
        Query query = TextQuery.queryText(textCriteria);
        return mongoTemplate.find(query, Product.class);
    }

    // Criteria.where("tags").all()
    public List<Product> findByTagsContainingAll(List<String> tags) {
        Query query = new Query(Criteria.where("tags").all(tags));
        return mongoTemplate.find(query, Product.class);
    }

    // .size()
    public List<Product> findByTagsSize(int size) {
        Query query = new Query(Criteria.where("tags").size(size));
        return mongoTemplate.find(query, Product.class);
    }

    // Dot-notation: specifications.key
    public List<Product> findBySpecificationEntry(String key, String value) {
        Query query = new Query(Criteria.where("specifications." + key).is(value));
        return mongoTemplate.find(query, Product.class);
    }

    // Multi-criteria + boolean — Decimal128 fields require explicit conversion
    public List<Product> findByPriceRangeAndInStock(BigDecimal min, BigDecimal max) {
        Query query = new Query(
                Criteria.where("price").gte(new Decimal128(min)).lte(new Decimal128(max))
                        .and("inStock").is(true)
        );
        return mongoTemplate.find(query, Product.class);
    }

    // findDistinct("category")
    public List<String> findDistinctCategories() {
        return mongoTemplate.findDistinct(new Query(), "category", Product.class, String.class);
    }

    // regex(pattern, "i")
    public List<Product> findByNameRegex(String pattern) {
        Query query = new Query(Criteria.where("name").regex(pattern, "i"));
        return mongoTemplate.find(query, Product.class);
    }
}
