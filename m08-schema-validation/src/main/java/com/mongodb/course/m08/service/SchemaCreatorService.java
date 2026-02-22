package com.mongodb.course.m08.service;

import org.bson.Document;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoJsonSchemaCreator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.stereotype.Service;

@Service
public class SchemaCreatorService {

    private final MongoTemplate mongoTemplate;

    public SchemaCreatorService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public MongoJsonSchema generateSchemaFor(Class<?> clazz) {
        MongoJsonSchemaCreator creator = MongoJsonSchemaCreator.create(mongoTemplate.getConverter());
        return creator.createSchemaFor(clazz);
    }

    public void createCollectionWithAutoSchema(Class<?> clazz, String collectionName) {
        MongoJsonSchema schema = generateSchemaFor(clazz);

        // Remove _id from generated schema: MongoJsonSchemaCreator maps @Id String
        // to JSON Schema type "object", but MongoDB stores _id as ObjectId — type mismatch.
        Document schemaDoc = schema.toDocument().get("$jsonSchema", Document.class);
        Document properties = schemaDoc.get("properties", Document.class);
        if (properties != null) {
            properties.remove("_id");
        }
        MongoJsonSchema fixedSchema = MongoJsonSchema.of(schemaDoc);

        CollectionOptions options = CollectionOptions.empty()
                .schema(fixedSchema)
                .strictValidation()
                .failOnValidationError();
        mongoTemplate.createCollection(collectionName, options);
    }

    public Document getSchemaAsDocument(Class<?> clazz) {
        return generateSchemaFor(clazz).toDocument();
    }
}
