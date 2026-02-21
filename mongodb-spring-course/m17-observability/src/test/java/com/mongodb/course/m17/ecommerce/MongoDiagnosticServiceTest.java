package com.mongodb.course.m17.ecommerce;

import com.mongodb.course.m17.SharedContainersConfig;
import com.mongodb.course.m17.observability.MongoDiagnosticService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class MongoDiagnosticServiceTest {

    @Autowired
    private MongoDiagnosticService diagnosticService;

    @Autowired
    private ProductService productService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), "m17_products");
    }

    @Test
    void serverStatus_returnsVersion() {
        var report = diagnosticService.getServerStatus();
        assertThat(report.version()).startsWith("8.0");
    }

    @Test
    void serverStatus_hasConnections() {
        var report = diagnosticService.getServerStatus();
        assertThat(report.currentConnections()).isGreaterThan(0);
    }

    @Test
    void databaseStats_collectionsCountPositive() {
        productService.create("Test Product", "electronics", 1000);

        var report = diagnosticService.getDatabaseStats();
        assertThat(report.collections()).isGreaterThan(0);
        assertThat(report.documents()).isGreaterThan(0);
    }

    @Test
    void collectionStats_matchesInsertedCount() {
        for (int i = 1; i <= 5; i++) {
            productService.create("Product " + i, "electronics", i * 100L);
        }

        var report = diagnosticService.getCollectionStats("m17_products");
        assertThat(report.documentCount()).isEqualTo(5);
    }
}
