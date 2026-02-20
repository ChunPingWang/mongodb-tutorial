package com.mongodb.course.m06;

import com.mongodb.course.m06.geospatial.Store;
import com.mongodb.course.m06.geospatial.StoreQueryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LAB-05: 地理空間查詢 — Store domain (Taipei coordinates)
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GeospatialQueryTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private StoreQueryService queryService;

    // Taipei landmark coordinates (longitude, latitude)
    static final double TAIPEI_101_LNG = 121.5654;
    static final double TAIPEI_101_LAT = 25.0330;

    @BeforeAll
    static void createIndex(@Autowired MongoTemplate mongoTemplate) {
        mongoTemplate.dropCollection(Store.class);
        mongoTemplate.indexOps(Store.class).ensureIndex(
                new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE));
    }

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new org.springframework.data.mongodb.core.query.Query(), Store.class);

        // Xinyi District — ~0.5km from Taipei 101
        Store s1 = new Store("Xinyi Cafe", "Taipei", "cafe",
                new GeoJsonPoint(121.5675, 25.0365), true, 4.5);

        // Da'an District — ~2.5km from Taipei 101
        Store s2 = new Store("Da'an Bookstore", "Taipei", "bookstore",
                new GeoJsonPoint(121.5434, 25.0260), true, 4.2);

        // Zhongshan District — ~5km from Taipei 101
        Store s3 = new Store("Zhongshan Restaurant", "Taipei", "restaurant",
                new GeoJsonPoint(121.5225, 25.0530), false, 4.0);

        // Banqiao — ~11km from Taipei 101
        Store s4 = new Store("Banqiao Mall", "New Taipei", "mall",
                new GeoJsonPoint(121.4722, 25.0145), true, 3.8);

        // Near Taipei 101 — ~0.2km
        Store s5 = new Store("101 Cafe", "Taipei", "cafe",
                new GeoJsonPoint(121.5650, 25.0340), true, 4.7);

        mongoTemplate.insertAll(List.of(s1, s2, s3, s4, s5));
    }

    @Test
    @Order(1)
    void findNearby_withinMaxDistance() {
        // Find stores within 3km of Taipei 101
        GeoResults<Store> results = queryService.findNearbyStores(TAIPEI_101_LNG, TAIPEI_101_LAT, 3000);
        // Should find: 101 Cafe (~0.2km), Xinyi Cafe (~0.5km), Da'an Bookstore (~2.5km)
        assertThat(results.getContent()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results.getContent()).allMatch(r ->
                r.getDistance().getValue() <= 3.0); // distance in km
    }

    @Test
    @Order(2)
    void findWithinRadius_usesWithinSphere() {
        // 6km radius from Taipei 101
        List<Store> results = queryService.findStoresWithinRadius(TAIPEI_101_LNG, TAIPEI_101_LAT, 6);
        // Should find: 101 Cafe, Xinyi Cafe, Da'an Bookstore, Zhongshan Restaurant
        assertThat(results).hasSizeGreaterThanOrEqualTo(3);
        assertThat(results).noneMatch(s -> s.getName().equals("Banqiao Mall")); // ~11km away
    }

    @Test
    @Order(3)
    void findWithinBoundingBox() {
        // Bounding box covering Xinyi + Da'an area
        Point lowerLeft = new Point(121.53, 25.02);
        Point upperRight = new Point(121.58, 25.04);
        List<Store> results = queryService.findStoresInBox(lowerLeft, upperRight);
        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(4)
    void nearQuery_combinedWithCategory() {
        // Cafes within 3km of Taipei 101
        GeoResults<Store> results = queryService.findNearbyByCategory(
                TAIPEI_101_LNG, TAIPEI_101_LAT, 3, "cafe");
        assertThat(results.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(results.getContent()).allMatch(r ->
                r.getContent().getCategory().equals("cafe"));
    }

    @Test
    @Order(5)
    void nearQuery_combinedWithOpenStatus() {
        // Open stores within 6km
        GeoResults<Store> results = queryService.findNearbyOpenStores(
                TAIPEI_101_LNG, TAIPEI_101_LAT, 6);
        assertThat(results.getContent()).allMatch(r -> r.getContent().isOpen());
        // Zhongshan Restaurant is closed, should be excluded
        assertThat(results.getContent()).noneMatch(r ->
                r.getContent().getName().equals("Zhongshan Restaurant"));
    }

    @Test
    @Order(6)
    void geoResults_includeDistance() {
        GeoResults<Store> results = queryService.findNearbyStores(TAIPEI_101_LNG, TAIPEI_101_LAT, 3000);
        assertThat(results.getContent()).allSatisfy(r -> {
            assertThat(r.getDistance()).isNotNull();
            assertThat(r.getDistance().getValue()).isGreaterThan(0);
        });
        // Results should be ordered by distance (nearest first)
        var distances = results.getContent().stream()
                .map(r -> r.getDistance().getValue())
                .toList();
        assertThat(distances).isSorted();
    }
}
