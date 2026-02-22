package com.mongodb.course.m21.listing;

import com.mongodb.course.m21.SharedContainersConfig;
import com.mongodb.course.m21.config.SchemaValidationConfig;
import com.mongodb.course.m21.listing.model.ListingStatus;
import com.mongodb.course.m21.listing.service.ProductListingService;
import com.mongodb.course.m21.order.model.OrderLine;
import com.mongodb.course.m21.order.model.ShippingAddress;
import com.mongodb.course.m21.order.service.OrderCommandService;
import com.mongodb.course.m21.product.ProductCatalogService;
import org.bson.Document;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class ProductListingServiceTest {

    @Autowired private ProductListingService listingService;
    @Autowired private OrderCommandService orderCommandService;
    @Autowired private ProductCatalogService productCatalogService;
    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private SchemaValidationConfig schemaValidationConfig;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.remove(new Query(), "m21_order_events");
        mongoTemplate.remove(new Query(), "m21_snapshots");
        mongoTemplate.remove(new Query(), "m21_order_dashboard");
        mongoTemplate.remove(new Query(), "m21_sales_statistics");
        mongoTemplate.remove(new Query(), "m21_fulfillment_saga_logs");
        if (mongoTemplate.collectionExists("m21_products")) {
            mongoTemplate.dropCollection("m21_products");
        }
        schemaValidationConfig.init();
    }

    @Test
    void approvedWhenProfitable() {
        var address = new ShippingAddress("Test", "St", "Taipei", "100");
        for (int i = 1; i <= 10; i++) {
            var lines = List.of(new OrderLine("P" + i, "Item", "Electronics", 1, new BigDecimal("2000")));
            orderCommandService.placeOrder("ORD-PL-" + i, "CUST-" + i, lines, address);
        }

        var listing = listingService.submit("NEW-001", "New Product", "Electronics",
                "Electronics", new BigDecimal("1500"), 50);
        listing = listingService.review(listing);

        assertThat(listing.getStatus()).isEqualTo(ListingStatus.APPROVED);
        var product = productCatalogService.findBySku("NEW-001");
        assertThat(product).isPresent();
    }

    @Test
    void rejectedWhenUnprofitable() {
        var address = new ShippingAddress("Test", "St", "Taipei", "100");
        for (int i = 1; i <= 3; i++) {
            var lines = List.of(new OrderLine("F" + i, "Food Item", "Food", 1, new BigDecimal("100")));
            orderCommandService.placeOrder("ORD-FD-" + i, "CUST-" + i, lines, address);
        }
        // Cancel 2 of 3 orders to get high cancel rate
        orderCommandService.cancel("ORD-FD-1", "Test cancel");
        orderCommandService.cancel("ORD-FD-2", "Test cancel");

        var listing = listingService.submit("FOOD-001", "Snack", "Food",
                "Food", new BigDecimal("50"), 100);
        listing = listingService.review(listing);

        assertThat(listing.getStatus()).isEqualTo(ListingStatus.REJECTED);
    }

    @Test
    void schemaValidationRejectsInvalid() {
        var invalidDoc = new Document("name", "Incomplete Product");
        // Missing required fields: sku, category, price, stockQuantity

        assertThatThrownBy(() -> mongoTemplate.insert(invalidDoc, "m21_products"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
