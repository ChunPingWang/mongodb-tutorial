package com.mongodb.course.m06.bdd;

import com.mongodb.course.m06.geospatial.Store;
import com.mongodb.course.m06.geospatial.StoreQueryService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GeospatialQuerySteps {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private StoreQueryService storeQueryService;

    private GeoResults<Store> geoResults;

    // Taipei 101 coordinates
    private static final double TAIPEI_101_LNG = 121.5654;
    private static final double TAIPEI_101_LAT = 25.0330;

    @Given("系統中有以下商店資料")
    public void setupStores(DataTable table) {
        mongoTemplate.dropCollection(Store.class);
        mongoTemplate.indexOps(Store.class).ensureIndex(
                new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE));

        List<Map<String, String>> rows = table.asMaps();
        for (Map<String, String> row : rows) {
            Store store = new Store(
                    row.get("name"),
                    row.get("city"),
                    row.get("category"),
                    new GeoJsonPoint(
                            Double.parseDouble(row.get("longitude")),
                            Double.parseDouble(row.get("latitude"))
                    ),
                    Boolean.parseBoolean(row.get("open")),
                    4.0
            );
            mongoTemplate.insert(store);
        }
    }

    @When("我查詢台北 {int} 附近 {int} 公里內的商店")
    public void queryNearby(int landmark, int radiusKm) {
        geoResults = storeQueryService.findNearbyStores(
                TAIPEI_101_LNG, TAIPEI_101_LAT, radiusKm * 1000.0);
    }

    @When("我查詢台北 {int} 附近 {int} 公里內營業中的 {string}")
    public void queryNearbyCategoryOpen(int landmark, int radiusKm, String category) {
        // Combined: open=true + category
        geoResults = storeQueryService.findNearbyByCategory(
                TAIPEI_101_LNG, TAIPEI_101_LAT, radiusKm, category);
        // Further filter for open stores
        var filtered = geoResults.getContent().stream()
                .filter(r -> r.getContent().isOpen())
                .toList();
        // Store filtered results for verification
        geoResults = new GeoResults<>(filtered);
    }

    @Then("應該找到至少 {int} 間商店")
    public void verifyMinStores(int min) {
        assertThat(geoResults.getContent()).hasSizeGreaterThanOrEqualTo(min);
    }

    @And("結果不包含 {string}")
    public void verifyNotContains(String storeName) {
        assertThat(geoResults.getContent())
                .noneMatch(r -> r.getContent().getName().equals(storeName));
    }

    @And("結果中所有商店都是營業中的 cafe")
    public void verifyAllOpenCafes() {
        assertThat(geoResults.getContent()).allSatisfy(r -> {
            assertThat(r.getContent().isOpen()).isTrue();
            assertThat(r.getContent().getCategory()).isEqualTo("cafe");
        });
    }
}
