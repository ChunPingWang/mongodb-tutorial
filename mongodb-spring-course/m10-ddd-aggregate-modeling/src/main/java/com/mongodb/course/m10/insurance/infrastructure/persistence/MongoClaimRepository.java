package com.mongodb.course.m10.insurance.infrastructure.persistence;

import com.mongodb.course.m10.insurance.domain.model.Claim;
import com.mongodb.course.m10.insurance.domain.port.ClaimRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MongoClaimRepository implements ClaimRepository {

    private final MongoTemplate mongoTemplate;
    private final ClaimMapper mapper;

    public MongoClaimRepository(MongoTemplate mongoTemplate, ClaimMapper mapper) {
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public Claim save(Claim claim) {
        ClaimMongoDocument doc = mapper.toDocument(claim);
        ClaimMongoDocument saved = mongoTemplate.save(doc);
        claim.setId(saved.getId());
        claim.clearDomainEvents();
        return claim;
    }

    @Override
    public Optional<Claim> findById(String id) {
        ClaimMongoDocument doc = mongoTemplate.findById(id, ClaimMongoDocument.class);
        return Optional.ofNullable(doc).map(mapper::toDomain);
    }
}
