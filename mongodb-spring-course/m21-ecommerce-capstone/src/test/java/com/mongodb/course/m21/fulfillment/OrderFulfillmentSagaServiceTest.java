package com.mongodb.course.m21.fulfillment;

import com.mongodb.course.m21.SharedContainersConfig;
import com.mongodb.course.m21.config.SchemaValidationConfig;
import com.mongodb.course.m21.infrastructure.saga.SagaLogRepository;
import com.mongodb.course.m21.infrastructure.saga.SagaStatus;
import com.mongodb.course.m21.order.model.OrderLine;
import com.mongodb.course.m21.order.model.ShippingAddress;
import com.mongodb.course.m21.order.service.OrderCommandService;
import com.mongodb.course.m21.product.ElectronicsProduct;
import com.mongodb.course.m21.product.ProductCatalogService;
import com.mongodb.course.m21.projection.readmodel.OrderDashboardDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class OrderFulfillmentSagaServiceTest {

    @Autowired private OrderFulfillmentSagaService sagaService;
    @Autowired private OrderCommandService orderCommandService;
    @Autowired private ProductCatalogService productCatalogService;
    @Autowired private SagaLogRepository sagaLogRepository;
    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private SchemaValidationConfig schemaValidationConfig;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.remove(new Query(), "m21_order_events");
        mongoTemplate.remove(new Query(), "m21_snapshots");
        mongoTemplate.remove(new Query(), "m21_order_dashboard");
        mongoTemplate.remove(new Query(), "m21_sales_statistics");
        mongoTemplate.remove(new Query(), "m21_fulfillment_saga_logs");
        mongoTemplate.remove(new Query(), "m21_order_notifications");
        if (mongoTemplate.collectionExists("m21_products")) {
            mongoTemplate.dropCollection("m21_products");
        }
        schemaValidationConfig.init();
    }

    @Test
    void successfulFulfillment() {
        var product = new ElectronicsProduct("PHONE-01", "PHONE-001", "Phone", "Electronics",
                new BigDecimal("25000"), 20, "Samsung", 24);
        productCatalogService.save(product);

        var lines = List.of(new OrderLine("PHONE-01", "Phone", "Electronics", 2, new BigDecimal("25000")));
        var address = new ShippingAddress("Alice", "Main St", "Taipei", "100");
        var order = orderCommandService.placeOrder("ORD-SF01", "CUST-001", lines, address);

        String sagaId = sagaService.executeFulfillment(order);
        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();

        assertThat(sagaLog.status()).isEqualTo(SagaStatus.COMPLETED);
        var updatedProduct = productCatalogService.findById("PHONE-01").orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(18);
    }

    @Test
    void insufficientStockCompensation() {
        var product = new ElectronicsProduct("TABLET-01", "TABLET-001", "Tablet", "Electronics",
                new BigDecimal("15000"), 5, "Apple", 12);
        productCatalogService.save(product);

        var lines = List.of(new OrderLine("TABLET-01", "Tablet", "Electronics", 10, new BigDecimal("15000")));
        var address = new ShippingAddress("Bob", "2nd Ave", "Taipei", "110");
        var order = orderCommandService.placeOrder("ORD-SF02", "CUST-002", lines, address);

        String sagaId = sagaService.executeFulfillment(order);
        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();

        assertThat(sagaLog.status()).isEqualTo(SagaStatus.COMPENSATED);
        var updatedProduct = productCatalogService.findById("TABLET-01").orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void bulkPurchaseBlocked() {
        var product = new ElectronicsProduct("CAM-01", "CAM-001", "Camera", "Electronics",
                new BigDecimal("30000"), 100, "Canon", 24);
        productCatalogService.save(product);

        // Pre-insert 4 orders for same customer + category in last 24h (via dashboard)
        for (int i = 1; i <= 4; i++) {
            var doc = new OrderDashboardDocument(
                    "ORD-BULK-" + i, "CUST-BULK", new BigDecimal("30000"),
                    "PLACED", 1, List.of("Electronics"),
                    "Taipei", null, List.of(), Instant.now(), 1);
            mongoTemplate.insert(doc, "m21_order_dashboard");
        }

        var lines = List.of(new OrderLine("CAM-01", "Camera", "Electronics", 1, new BigDecimal("30000")));
        var address = new ShippingAddress("Charlie", "3rd Rd", "Taipei", "105");
        var order = orderCommandService.placeOrder("ORD-SF03", "CUST-BULK", lines, address);

        String sagaId = sagaService.executeFulfillment(order);
        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();

        assertThat(sagaLog.status()).isEqualTo(SagaStatus.COMPENSATED);
    }
}
