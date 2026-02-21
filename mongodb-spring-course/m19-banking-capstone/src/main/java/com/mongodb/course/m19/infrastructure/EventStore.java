package com.mongodb.course.m19.infrastructure;

import com.mongodb.course.m19.shared.DomainEvent;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventStore {

    private static final String ACCOUNT_EVENTS = "m19_account_events";
    private static final String SNAPSHOTS = "m19_snapshots";

    private final MongoTemplate mongoTemplate;

    public EventStore(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    void init() {
        ensureIndexes(ACCOUNT_EVENTS);
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
    public <T extends DomainEvent> List<T> loadEventsAfterVersion(
            String aggregateId, long afterVersion, Class<T> type, String collection) {
        var query = Query.query(
                Criteria.where("aggregateId").is(aggregateId)
                        .and("version").gt(afterVersion))
                .with(Sort.by(Sort.Direction.ASC, "version"));
        return (List<T>) mongoTemplate.find(query, type, collection);
    }

    public long countEvents(String aggregateId, String collection) {
        var query = Query.query(Criteria.where("aggregateId").is(aggregateId));
        return mongoTemplate.count(query, collection);
    }

    public SnapshotDocument saveSnapshot(SnapshotDocument snapshot) {
        return mongoTemplate.insert(snapshot);
    }

    public Optional<SnapshotDocument> loadLatestSnapshot(String aggregateId, String aggregateType) {
        var query = Query.query(
                Criteria.where("aggregateId").is(aggregateId)
                        .and("aggregateType").is(aggregateType))
                .with(Sort.by(Sort.Direction.DESC, "version"))
                .limit(1);
        return Optional.ofNullable(mongoTemplate.findOne(query, SnapshotDocument.class));
    }

    public void ensureIndexes(String collection) {
        var indexDef = new CompoundIndexDefinition(
                new org.bson.Document("aggregateId", 1).append("version", 1))
                .unique();
        mongoTemplate.indexOps(collection).ensureIndex(indexDef);
    }
}
