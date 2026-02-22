package com.mongodb.course.m15.ecommerce;

import com.mongodb.client.model.Indexes;
import com.mongodb.course.m15.SharedContainersConfig;
import com.mongodb.course.m15.index.ExplainAnalyzer;
import com.mongodb.course.m15.index.IndexManagementService;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class ProductSearchServiceTest {

    private static final String COLLECTION = "m15_products";

    @Autowired
    private ProductSearchService searchService;

    @Autowired
    private ProductDataGenerator dataGenerator;

    @Autowired
    private IndexManagementService indexManagementService;

    @Autowired
    private ExplainAnalyzer explainAnalyzer;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), COLLECTION);
        mongoTemplate.indexOps(COLLECTION).dropAllIndexes();
        dataGenerator.generateProducts(500);
    }

    @Test
    void textSearch_usesTextIndex() {
        indexManagementService.createTextIndex(COLLECTION,
                Map.of("name", 3F, "description", 1F));

        var results = searchService.textSearch("wireless");
        assertThat(results).isNotEmpty();
    }

    @Test
    void findByCategoryAndPriceRange_usesCompoundIndex() {
        var keys = new LinkedHashMap<String, Sort.Direction>();
        keys.put("category", Sort.Direction.ASC);
        keys.put("price", Sort.Direction.ASC);
        indexManagementService.createCompoundIndex(COLLECTION, keys);

        var results = searchService.findByCategoryAndPriceRange("Electronics", 1000, 5000);
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(p -> p.category().equals("Electronics"));
        assertThat(results).allMatch(p -> p.price() >= 1000 && p.price() <= 5000);

        var explain = explainAnalyzer.explain(COLLECTION,
                new Document("category", "Electronics")
                        .append("price", new Document("$gte", 1000L).append("$lte", 5000L)));
        assertThat(explain.stage()).isEqualTo("IXSCAN");
    }

    @Test
    void findByTag_usesMultikeyIndex() {
        indexManagementService.createSingleFieldIndex(COLLECTION, "tags", Sort.Direction.ASC);

        var results = searchService.findByTag("portable");
        assertThat(results).isNotEmpty();

        var explain = explainAnalyzer.explain(COLLECTION,
                new Document("tags", "portable"));
        assertThat(explain.stage()).isEqualTo("IXSCAN");
    }

    @Test
    void findInStockByCategory_usesPartialIndex() {
        indexManagementService.createPartialIndex(COLLECTION,
                Indexes.ascending("category"),
                new Document("inStock", true));

        var results = searchService.findInStockByCategory("Books");
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(Product::inStock);
        assertThat(results).allMatch(p -> p.category().equals("Books"));

        var explain = explainAnalyzer.explain(COLLECTION,
                new Document("inStock", true).append("category", "Books"));
        assertThat(explain.stage()).isEqualTo("IXSCAN");
    }

    @Test
    void findInStockSortedByPrice_returnsAscendingOrder() {
        var results = searchService.findInStockSortedByPrice(10);
        assertThat(results).hasSize(10);
        assertThat(results).allMatch(Product::inStock);

        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).price()).isLessThanOrEqualTo(results.get(i + 1).price());
        }
    }
}
