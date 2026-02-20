package com.mongodb.course.m08;

import com.mongodb.course.m08.ecommerce.ProductV1;
import com.mongodb.course.m08.ecommerce.ProductV2;
import com.mongodb.course.m08.service.SchemaEvolutionService;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class SchemaEvolutionMigrationTest {

    private static final String COLLECTION = "m08_product_versions";

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    SchemaEvolutionService schemaEvolutionService;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(COLLECTION);
    }

    @Test
    void insertV1Product_hasSchemaVersion1() {
        ProductV1 product = new ProductV1("Laptop", new BigDecimal("999"), true);
        schemaEvolutionService.insertV1Product(product);

        List<Document> docs = schemaEvolutionService.findBySchemaVersion(1);
        assertThat(docs).hasSize(1);
        assertThat(docs.getFirst().getString("name")).isEqualTo("Laptop");
        assertThat(docs.getFirst().getInteger("schemaVersion")).isEqualTo(1);
    }

    @Test
    void insertV2Product_hasNewFields() {
        ProductV2 product = new ProductV2("Laptop", new BigDecimal("999"), true,
                "Electronics", List.of("tech", "computer"));
        schemaEvolutionService.insertV2Product(product);

        List<Document> docs = schemaEvolutionService.findBySchemaVersion(2);
        assertThat(docs).hasSize(1);
        assertThat(docs.getFirst().getString("category")).isEqualTo("Electronics");
        assertThat(docs.getFirst().getList("tags", String.class)).containsExactly("tech", "computer");
    }

    @Test
    void migrateV1ToV2_addsCategoryAndTags() {
        schemaEvolutionService.insertV1Product(new ProductV1("Laptop", new BigDecimal("999"), true));
        schemaEvolutionService.insertV1Product(new ProductV1("Mouse", new BigDecimal("29"), true));

        int migrated = schemaEvolutionService.migrateToVersion(2);
        assertThat(migrated).isEqualTo(2);

        List<Document> v2Docs = schemaEvolutionService.findBySchemaVersion(2);
        assertThat(v2Docs).hasSize(2);
        for (Document doc : v2Docs) {
            assertThat(doc.getString("category")).isEqualTo("Uncategorized");
            assertThat(doc.getList("tags", String.class)).isEmpty();
            assertThat(doc.getInteger("schemaVersion")).isEqualTo(2);
        }
    }

    @Test
    void migrateV2ToV3_addsRatingAndDescription() {
        ProductV2 product = new ProductV2("Laptop", new BigDecimal("999"), true,
                "Electronics", List.of("tech"));
        schemaEvolutionService.insertV2Product(product);

        int migrated = schemaEvolutionService.migrateToVersion(3);
        assertThat(migrated).isEqualTo(1);

        List<Document> v3Docs = schemaEvolutionService.findBySchemaVersion(3);
        assertThat(v3Docs).hasSize(1);
        Document doc = v3Docs.getFirst();
        assertThat(doc.getDouble("rating")).isEqualTo(0.0);
        assertThat(doc.getString("description")).isEmpty();
        assertThat(doc.get("specifications")).isInstanceOf(Document.class);
    }

    @Test
    void fullMigrationChain_V1ToV3() {
        schemaEvolutionService.insertV1Product(new ProductV1("Laptop", new BigDecimal("999"), true));

        int migrated = schemaEvolutionService.migrateToVersion(3);
        assertThat(migrated).isEqualTo(1);

        List<Document> v3Docs = schemaEvolutionService.findBySchemaVersion(3);
        assertThat(v3Docs).hasSize(1);
        Document doc = v3Docs.getFirst();
        assertThat(doc.getString("name")).isEqualTo("Laptop");
        assertThat(doc.getString("category")).isEqualTo("Uncategorized");
        assertThat(doc.getList("tags", String.class)).isEmpty();
        assertThat(doc.getDouble("rating")).isEqualTo(0.0);
        assertThat(doc.getString("description")).isEmpty();
        assertThat(doc.getInteger("schemaVersion")).isEqualTo(3);
    }

    @Test
    void migrationIsIdempotent() {
        schemaEvolutionService.insertV1Product(new ProductV1("Laptop", new BigDecimal("999"), true));

        schemaEvolutionService.migrateToVersion(3);
        int secondRun = schemaEvolutionService.migrateToVersion(3);

        assertThat(secondRun).isEqualTo(0);
    }

    @Test
    void moderateSchema_allowsMixedVersionDocs() {
        // Insert V1 and V2 docs first (before schema)
        schemaEvolutionService.insertV1Product(new ProductV1("Laptop", new BigDecimal("999"), true));
        schemaEvolutionService.insertV2Product(new ProductV2("Mouse", new BigDecimal("29"), true,
                "Accessories", List.of("peripheral")));

        // Apply V3 schema with moderate validation
        schemaEvolutionService.applyVersionedSchemaModerate();

        // V1 and V2 docs still exist and can be queried
        Map<Integer, Long> counts = schemaEvolutionService.countPerVersion();
        assertThat(counts).containsEntry(1, 1L);
        assertThat(counts).containsEntry(2, 1L);

        long total = mongoTemplate.getCollection(COLLECTION).countDocuments();
        assertThat(total).isEqualTo(2);
    }
}
