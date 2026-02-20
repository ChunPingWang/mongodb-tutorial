package com.mongodb.course.m08.service;

import com.mongodb.course.m08.ecommerce.ProductV1;
import com.mongodb.course.m08.ecommerce.ProductV2;
import com.mongodb.course.m08.migration.MigrationService;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.schema.JsonSchemaProperty.int32;
import static org.springframework.data.mongodb.core.schema.JsonSchemaProperty.number;
import static org.springframework.data.mongodb.core.schema.JsonSchemaProperty.required;
import static org.springframework.data.mongodb.core.schema.JsonSchemaProperty.string;

@Service
public class SchemaEvolutionService {

    private static final String COLLECTION = "m08_product_versions";

    private final MongoTemplate mongoTemplate;
    private final MigrationService migrationService;

    public SchemaEvolutionService(MongoTemplate mongoTemplate, MigrationService migrationService) {
        this.mongoTemplate = mongoTemplate;
        this.migrationService = migrationService;
    }

    public void insertV1Product(ProductV1 product) {
        mongoTemplate.save(product);
    }

    public void insertV2Product(ProductV2 product) {
        mongoTemplate.save(product);
    }

    public List<Document> findBySchemaVersion(int version) {
        Query query = Query.query(Criteria.where("schemaVersion").is(version));
        return mongoTemplate.find(query, Document.class, COLLECTION);
    }

    public int migrateToVersion(int targetVersion) {
        Query query = Query.query(Criteria.where("schemaVersion").lt(targetVersion));
        List<Document> docs = mongoTemplate.find(query, Document.class, COLLECTION);
        int count = 0;
        for (Document doc : docs) {
            Document migrated = migrationService.migrateToVersion(doc, targetVersion);
            mongoTemplate.getCollection(COLLECTION).replaceOne(
                    new Document("_id", doc.get("_id")),
                    migrated
            );
            count++;
        }
        return count;
    }

    public Map<Integer, Long> countPerVersion() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.group("schemaVersion").count().as("count"),
                Aggregation.project("count").and("_id").as("version")
        );
        AggregationResults<Document> results = mongoTemplate.aggregate(agg, COLLECTION, Document.class);
        return results.getMappedResults().stream()
                .collect(Collectors.toMap(
                        d -> d.getInteger("version"),
                        d -> d.get("count", Number.class).longValue()
                ));
    }

    public void applyVersionedSchemaModerate() {
        MongoJsonSchema schema = MongoJsonSchema.builder()
                .properties(
                        required(string("name")),
                        required(number("price")),
                        required(int32("schemaVersion"))
                )
                .build();

        Document command = new Document("collMod", COLLECTION)
                .append("validator", schema.toDocument())
                .append("validationLevel", "moderate")
                .append("validationAction", "error");
        mongoTemplate.getDb().runCommand(command);
    }
}
