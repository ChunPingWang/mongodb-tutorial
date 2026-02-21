package com.mongodb.course.m15.index;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class ExplainAnalyzer {

    private final MongoTemplate mongoTemplate;

    public ExplainAnalyzer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public ExplainResult explain(String collectionName, Document filter) {
        return explain(collectionName, filter, null);
    }

    public ExplainResult explain(String collectionName, Document filter, Document projection) {
        var findCommand = new Document("find", collectionName)
                .append("filter", filter);
        if (projection != null) {
            findCommand.append("projection", projection);
        }

        var explainCommand = new Document("explain", findCommand)
                .append("verbosity", "executionStats");

        Document result = mongoTemplate.getDb().runCommand(explainCommand);
        return ExplainResult.from(result);
    }
}
