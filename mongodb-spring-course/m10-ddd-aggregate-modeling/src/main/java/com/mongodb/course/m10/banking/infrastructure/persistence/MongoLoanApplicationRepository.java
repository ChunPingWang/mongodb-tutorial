package com.mongodb.course.m10.banking.infrastructure.persistence;

import com.mongodb.course.m10.banking.domain.model.LoanApplication;
import com.mongodb.course.m10.banking.domain.model.LoanStatus;
import com.mongodb.course.m10.banking.domain.port.LoanApplicationRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoLoanApplicationRepository implements LoanApplicationRepository {

    private final MongoTemplate mongoTemplate;
    private final LoanApplicationMapper mapper;

    public MongoLoanApplicationRepository(MongoTemplate mongoTemplate, LoanApplicationMapper mapper) {
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public LoanApplication save(LoanApplication application) {
        LoanApplicationDocument doc = mapper.toDocument(application);
        LoanApplicationDocument saved = mongoTemplate.save(doc);
        application.setId(saved.getId());
        application.clearDomainEvents();
        return application;
    }

    @Override
    public Optional<LoanApplication> findById(String id) {
        LoanApplicationDocument doc = mongoTemplate.findById(id, LoanApplicationDocument.class);
        return Optional.ofNullable(doc).map(mapper::toDomain);
    }

    @Override
    public List<LoanApplication> findByStatus(LoanStatus status) {
        Query query = Query.query(Criteria.where("status").is(status.name()));
        return mongoTemplate.find(query, LoanApplicationDocument.class)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<LoanApplication> findByApplicantName(String name) {
        Query query = Query.query(Criteria.where("applicantName").is(name));
        return mongoTemplate.find(query, LoanApplicationDocument.class)
                .stream().map(mapper::toDomain).toList();
    }
}
