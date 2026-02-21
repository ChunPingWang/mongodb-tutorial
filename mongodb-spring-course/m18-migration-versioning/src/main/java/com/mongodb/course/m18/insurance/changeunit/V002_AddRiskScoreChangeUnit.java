package com.mongodb.course.m18.insurance.changeunit;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Map;

@ChangeUnit(id = "v002-add-risk-score", order = "002", author = "m18")
public class V002_AddRiskScoreChangeUnit {

    private static final String COLLECTION = "m18_policies";
    private static final Map<String, Integer> RISK_SCORES = Map.of(
            "AUTO", 50,
            "HOME", 30,
            "LIFE", 20
    );

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        var collection = mongoTemplate.getCollection(COLLECTION);
        for (var entry : RISK_SCORES.entrySet()) {
            collection.updateMany(
                    Filters.eq("type", entry.getKey()),
                    Updates.combine(
                            Updates.set("riskScore", entry.getValue()),
                            Updates.set("region", "UNKNOWN"),
                            Updates.set("schemaVersion", 2)
                    )
            );
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        var collection = mongoTemplate.getCollection(COLLECTION);
        collection.updateMany(
                Filters.exists("riskScore"),
                Updates.combine(
                        Updates.unset("riskScore"),
                        Updates.unset("region"),
                        Updates.set("schemaVersion", 1)
                )
        );
    }
}
