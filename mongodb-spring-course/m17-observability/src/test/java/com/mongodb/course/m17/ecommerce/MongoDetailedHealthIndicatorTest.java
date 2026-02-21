package com.mongodb.course.m17.ecommerce;

import com.mongodb.course.m17.SharedContainersConfig;
import com.mongodb.course.m17.observability.MongoDetailedHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class MongoDetailedHealthIndicatorTest {

    @Autowired
    private MongoDetailedHealthIndicator healthIndicator;

    @Autowired
    private ProductService productService;

    @Test
    void health_returnsUpWithDetails() {
        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("version");
        assertThat(health.getDetails()).containsKey("databaseName");
    }

    @Test
    void health_detailsContainCollectionsCount() {
        productService.create("Health Check Product", "test", 500);

        var health = healthIndicator.health();

        assertThat(health.getDetails()).containsKey("collections");
        assertThat((int) health.getDetails().get("collections")).isGreaterThan(0);
    }
}
