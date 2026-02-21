package com.mongodb.course.m15.index;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.HashedIndex;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IndexManagementService {

    private final MongoTemplate mongoTemplate;

    public IndexManagementService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public String createSingleFieldIndex(String collection, String field, Sort.Direction direction) {
        var index = new Index().on(field, direction);
        return mongoTemplate.indexOps(collection).ensureIndex(index);
    }

    public String createCompoundIndex(String collection, Map<String, Sort.Direction> keys) {
        var index = new Index();
        keys.forEach(index::on);
        return mongoTemplate.indexOps(collection).ensureIndex(index);
    }

    public String createTextIndex(String collection, Map<String, Float> fieldWeights) {
        var builder = TextIndexDefinition.builder();
        fieldWeights.forEach(builder::onField);
        return mongoTemplate.indexOps(collection).ensureIndex(builder.build());
    }

    public String createPartialIndex(String collection, Bson keys, Document partialFilter) {
        var options = new IndexOptions().partialFilterExpression(partialFilter);
        return mongoTemplate.getCollection(collection).createIndex(keys, options);
    }

    public String createTtlIndex(String collection, String field, long expireAfterSeconds) {
        var options = new IndexOptions().expireAfter(expireAfterSeconds, java.util.concurrent.TimeUnit.SECONDS);
        return mongoTemplate.getCollection(collection).createIndex(Indexes.ascending(field), options);
    }

    public String createUniqueIndex(String collection, Map<String, Sort.Direction> keys) {
        var index = new Index();
        keys.forEach(index::on);
        index.unique();
        return mongoTemplate.indexOps(collection).ensureIndex(index);
    }

    public String createHashedIndex(String collection, String field) {
        var index = HashedIndex.hashed(field);
        return mongoTemplate.indexOps(collection).ensureIndex(index);
    }

    public String createSparseIndex(String collection, String field) {
        var index = new Index().on(field, Sort.Direction.ASC).sparse();
        return mongoTemplate.indexOps(collection).ensureIndex(index);
    }

    public void dropIndex(String collection, String indexName) {
        mongoTemplate.indexOps(collection).dropIndex(indexName);
    }

    public void dropAllIndexes(String collection) {
        mongoTemplate.indexOps(collection).dropAllIndexes();
    }

    public List<Document> listIndexes(String collection) {
        var indexes = new ArrayList<Document>();
        mongoTemplate.getCollection(collection).listIndexes().into(indexes);
        return indexes;
    }
}
