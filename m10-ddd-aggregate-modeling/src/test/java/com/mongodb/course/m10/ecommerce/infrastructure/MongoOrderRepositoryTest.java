package com.mongodb.course.m10.ecommerce.infrastructure;

import com.mongodb.course.m10.SharedContainersConfig;
import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.ecommerce.domain.model.*;
import com.mongodb.course.m10.ecommerce.domain.port.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class MongoOrderRepositoryTest {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final ShippingAddress ADDRESS =
            new ShippingAddress("Alice", "123 Main St", "Taipei", "100");

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection("m10_orders");
    }

    @Test
    void save_persistsOrderWithAllEmbeddedObjects() {
        var lines = List.of(
                new OrderLine("P001", "手機", 1, Money.twd(30_000)),
                new OrderLine("P002", "保護殼", 2, Money.twd(500))
        );
        Order order = Order.create("ORD-001", "CUST-001", lines, ADDRESS);

        Order saved = repository.save(order);
        assertThat(saved.getId()).isNotNull();

        Optional<Order> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getLines()).hasSize(2);
        assertThat(found.get().getTotalAmount().amount())
                .isEqualByComparingTo("31000");
    }

    @Test
    void save_preservesOrderStatusTransitions() {
        var lines = List.of(new OrderLine("P001", "商品", 1, Money.twd(50_000)));
        Order order = Order.create("ORD-002", "CUST-002", lines, ADDRESS);
        repository.save(order);

        order.pay(new PaymentInfo("CREDIT_CARD", "TXN-001", Instant.now()));
        repository.save(order);

        Optional<Order> found = repository.findById(order.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(found.get().getPaymentInfo().transactionId()).isEqualTo("TXN-001");
    }

    @Test
    void findById_reconstructsDomainModel() {
        var lines = List.of(
                new OrderLine("P001", "筆電", 1, Money.twd(45_000)),
                new OrderLine("P002", "滑鼠", 1, Money.twd(800))
        );
        Order order = Order.create("ORD-003", "CUST-003", lines, ADDRESS);
        repository.save(order);

        Optional<Order> found = repository.findById(order.getId());
        assertThat(found).isPresent();
        Order loaded = found.get();
        assertThat(loaded.getOrderNumber()).isEqualTo("ORD-003");
        assertThat(loaded.getCustomerId()).isEqualTo("CUST-003");
        assertThat(loaded.getShippingAddress().city()).isEqualTo("Taipei");
        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.CREATED);
    }
}
