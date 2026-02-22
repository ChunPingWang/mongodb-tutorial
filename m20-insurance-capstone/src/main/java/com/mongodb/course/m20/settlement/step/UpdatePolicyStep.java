package com.mongodb.course.m20.settlement.step;

import com.mongodb.course.m20.infrastructure.saga.SagaContext;
import com.mongodb.course.m20.infrastructure.saga.SagaStep;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class UpdatePolicyStep implements SagaStep {

    private static final String COLLECTION = "m20_policies";

    private final MongoTemplate mongoTemplate;

    public UpdatePolicyStep(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String name() {
        return "UPDATE_POLICY";
    }

    @Override
    public void execute(SagaContext context) {
        String policyId = context.get("policyId", String.class);
        BigDecimal amount = new BigDecimal(context.get("approvedAmount", String.class));

        // Read raw document to determine policy type
        var rawDoc = mongoTemplate.findById(policyId, Document.class, COLLECTION);
        if (rawDoc == null) {
            throw new IllegalStateException("Policy not found: " + policyId);
        }

        String typeAlias = rawDoc.getString("_class");
        var query = Query.query(Criteria.where("_id").is(policyId));
        var update = new Update().inc("totalClaimsPaid", new Decimal128(amount));

        // Polymorphic switch: type-specific counter update
        switch (typeAlias) {
            case "AutoPolicy" -> update.inc("accidentCount", 1);
            case "HealthPolicy" -> update.inc("claimsThisYear", 1);
            case "LifePolicy" -> { /* no type-specific counter for life */ }
            default -> throw new IllegalStateException("Unknown policy type: " + typeAlias);
        }

        mongoTemplate.updateFirst(query, update, COLLECTION);
        context.put("policyTypeAlias", typeAlias);
    }

    @Override
    public void compensate(SagaContext context) {
        String policyId = context.get("policyId", String.class);
        BigDecimal amount = new BigDecimal(context.get("approvedAmount", String.class));
        String typeAlias = context.get("policyTypeAlias", String.class);

        var query = Query.query(Criteria.where("_id").is(policyId));
        var update = new Update().inc("totalClaimsPaid", new Decimal128(amount.negate()));

        if (typeAlias != null) {
            switch (typeAlias) {
                case "AutoPolicy" -> update.inc("accidentCount", -1);
                case "HealthPolicy" -> update.inc("claimsThisYear", -1);
                default -> { }
            }
        }

        mongoTemplate.updateFirst(query, update, COLLECTION);
    }
}
