package com.mongodb.course.m15.index;

import com.mongodb.client.model.Indexes;
import com.mongodb.course.m15.SharedContainersConfig;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class IndexManagementServiceTest {

    private static final String COLLECTION = "m15_test_indexes";

    @Autowired
    private IndexManagementService indexManagementService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        if (mongoTemplate.collectionExists(COLLECTION)) {
            mongoTemplate.dropCollection(COLLECTION);
        }
        mongoTemplate.createCollection(COLLECTION);
    }

    @Test
    void createAndListIndexes_allTypesCreated() {
        indexManagementService.createSingleFieldIndex(COLLECTION, "field1", Sort.Direction.ASC);

        var compoundKeys = new LinkedHashMap<String, Sort.Direction>();
        compoundKeys.put("field2", Sort.Direction.ASC);
        compoundKeys.put("field3", Sort.Direction.DESC);
        indexManagementService.createCompoundIndex(COLLECTION, compoundKeys);

        indexManagementService.createHashedIndex(COLLECTION, "field4");
        indexManagementService.createSparseIndex(COLLECTION, "field5");

        indexManagementService.createTextIndex(COLLECTION, Map.of("field6", 3F, "field7", 1F));

        indexManagementService.createPartialIndex(COLLECTION,
                Indexes.ascending("field8"),
                new Document("active", true));

        indexManagementService.createTtlIndex(COLLECTION, "createdAt", 3600);

        var uniqueKeys = new LinkedHashMap<String, Sort.Direction>();
        uniqueKeys.put("field9", Sort.Direction.ASC);
        indexManagementService.createUniqueIndex(COLLECTION, uniqueKeys);

        var indexes = indexManagementService.listIndexes(COLLECTION);
        // _id + 8 created = 9 total
        assertThat(indexes).hasSize(9);

        var indexNames = indexes.stream()
                .map(doc -> doc.getString("name"))
                .toList();
        assertThat(indexNames).contains("_id_");
    }

    @Test
    void dropIndex_removesSpecificIndex() {
        String indexName = indexManagementService.createSingleFieldIndex(
                COLLECTION, "dropMe", Sort.Direction.ASC);

        assertThat(indexManagementService.listIndexes(COLLECTION)).hasSize(2); // _id + dropMe

        indexManagementService.dropIndex(COLLECTION, indexName);

        assertThat(indexManagementService.listIndexes(COLLECTION)).hasSize(1); // _id only
    }
}
