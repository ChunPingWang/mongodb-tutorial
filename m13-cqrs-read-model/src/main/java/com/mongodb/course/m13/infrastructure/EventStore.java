package com.mongodb.course.m13.infrastructure;

import com.mongodb.course.m13.shared.DomainEvent;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventStore {

    private static final String ACCOUNT_EVENTS = "m13_account_events";
    private static final String CLAIM_EVENTS = "m13_claim_events";

    private final MongoTemplate mongoTemplate;

    public EventStore(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    void init() {
        ensureIndexes(ACCOUNT_EVENTS);
        ensureIndexes(CLAIM_EVENTS);
    }

    public <T extends DomainEvent> T append(T event, String collection) {
        return mongoTemplate.insert(event, collection);
    }

    public <T extends DomainEvent> List<T> appendAll(List<T> events, String collection) {
        return events.stream()
                .map(e -> append(e, collection))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> List<T> loadEvents(String aggregateId, Class<T> type, String collection) {
        var query = Query.query(Criteria.where("aggregateId").is(aggregateId))
                .with(Sort.by(Sort.Direction.ASC, "version"));
        return (List<T>) mongoTemplate.find(query, type, collection);
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> List<T> loadAllEvents(Class<T> type, String collection) {
        var query = new Query().with(Sort.by(Sort.Direction.ASC, "occurredAt"));
        return (List<T>) mongoTemplate.find(query, type, collection);
    }

    public void ensureIndexes(String collection) {
        var indexDef = new CompoundIndexDefinition(
                new org.bson.Document("aggregateId", 1).append("version", 1))
                .unique();
        mongoTemplate.indexOps(collection).ensureIndex(indexDef);
    }
}
