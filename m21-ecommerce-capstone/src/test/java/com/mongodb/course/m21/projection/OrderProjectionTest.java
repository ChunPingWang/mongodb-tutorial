package com.mongodb.course.m21.projection;

import com.mongodb.course.m21.SharedContainersConfig;
import com.mongodb.course.m21.order.model.OrderLine;
import com.mongodb.course.m21.order.model.PaymentInfo;
import com.mongodb.course.m21.order.model.ShippingAddress;
import com.mongodb.course.m21.order.service.OrderCommandService;
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

@SpringBootTest
@Import(SharedContainersConfig.class)
class OrderProjectionTest {

    @Autowired private OrderCommandService orderCommandService;
    @Autowired private OrderQueryService orderQueryService;
    @Autowired private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.remove(new Query(), "m21_order_events");
        mongoTemplate.remove(new Query(), "m21_snapshots");
        mongoTemplate.remove(new Query(), "m21_order_dashboard");
        mongoTemplate.remove(new Query(), "m21_sales_statistics");
    }

    @Test
    void dashboardReflectsProgress() {
        var lines = List.of(
                new OrderLine("P1", "Laptop", "Electronics", 1, new BigDecimal("35000"))
        );
        var address = new ShippingAddress("Alice", "Main St", "Taipei", "100");

        orderCommandService.placeOrder("ORD-PROJ1", "CUST-001", lines, address);
        orderCommandService.reserveInventory("ORD-PROJ1", List.of("P1"));
        orderCommandService.processPayment("ORD-PROJ1",
                new PaymentInfo("TXN-001", "CREDIT_CARD", "1234"), new BigDecimal("35000"));

        var dashboard = orderQueryService.findDashboardByOrderId("ORD-PROJ1").orElseThrow();
        assertThat(dashboard.status()).isEqualTo("PAYMENT_PROCESSED");
        assertThat(dashboard.timeline()).hasSize(3);
        assertThat(dashboard.timeline().getFirst().action()).isEqualTo("PLACED");
    }

    @Test
    void statisticsAggregateByCategory() {
        var electronicsLines1 = List.of(
                new OrderLine("P1", "Laptop", "Electronics", 1, new BigDecimal("35000"))
        );
        var electronicsLines2 = List.of(
                new OrderLine("P2", "Phone", "Electronics", 2, new BigDecimal("25000"))
        );
        var clothingLines = List.of(
                new OrderLine("P3", "Shirt", "Clothing", 3, new BigDecimal("500"))
        );

        var address = new ShippingAddress("Bob", "2nd Ave", "Taipei", "110");
        orderCommandService.placeOrder("ORD-ST1", "CUST-002", electronicsLines1, address);
        orderCommandService.placeOrder("ORD-ST2", "CUST-002", electronicsLines2, address);
        orderCommandService.placeOrder("ORD-ST3", "CUST-003", clothingLines, address);

        var electronicsStat = orderQueryService.findStatisticsByCategory("Electronics").orElseThrow();
        assertThat(electronicsStat.totalOrders()).isEqualTo(2);
        assertThat(electronicsStat.totalRevenue()).isEqualByComparingTo(new BigDecimal("85000"));

        var clothingStat = orderQueryService.findStatisticsByCategory("Clothing").orElseThrow();
        assertThat(clothingStat.totalOrders()).isEqualTo(1);
    }
}
