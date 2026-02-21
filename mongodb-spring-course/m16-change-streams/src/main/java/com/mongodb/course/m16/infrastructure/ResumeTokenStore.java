package com.mongodb.course.m16.infrastructure;

import org.bson.BsonDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;

@Service
public class ResumeTokenStore {

    private final MongoTemplate mongoTemplate;

    public ResumeTokenStore(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void saveToken(String listenerName, BsonDocument token) {
        var query = Query.query(Criteria.where("_id").is(listenerName));
        var update = new Update()
                .set("tokenJson", token.toJson())
                .set("savedAt", Instant.now());
        mongoTemplate.upsert(query, update, ResumeTokenDocument.class);
    }

    public BsonDocument loadToken(String listenerName) {
        var doc = mongoTemplate.findById(listenerName, ResumeTokenDocument.class);
        if (doc == null) {
            return null;
        }
        return BsonDocument.parse(doc.tokenJson());
    }
}
