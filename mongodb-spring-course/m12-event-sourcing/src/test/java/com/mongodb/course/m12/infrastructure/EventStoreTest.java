package com.mongodb.course.m12.infrastructure;

import com.mongodb.course.m12.SharedContainersConfig;
import com.mongodb.course.m12.banking.event.AccountEvent;
import com.mongodb.course.m12.banking.event.AccountOpened;
import com.mongodb.course.m12.banking.event.FundsDeposited;
import com.mongodb.course.m12.banking.event.FundsWithdrawn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class EventStoreTest {

    private static final String COLLECTION = "m12_account_events";

    @Autowired
    EventStore eventStore;

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.remove(new Query(), COLLECTION);
        mongoTemplate.remove(new Query(), "m12_snapshots");
    }

    @Test
    void append_and_loadEvents_roundTrip() {
        String aggregateId = "ACC-TEST-1";
        var opened = new AccountOpened(UUID.randomUUID().toString(), aggregateId, 1, Instant.now(),
                "測試用戶", new BigDecimal("10000"), "TWD");
        var deposited = new FundsDeposited(UUID.randomUUID().toString(), aggregateId, 2, Instant.now(),
                new BigDecimal("5000"), "存款");
        var withdrawn = new FundsWithdrawn(UUID.randomUUID().toString(), aggregateId, 3, Instant.now(),
                new BigDecimal("2000"), "提款");

        eventStore.append(opened, COLLECTION);
        eventStore.append(deposited, COLLECTION);
        eventStore.append(withdrawn, COLLECTION);

        var events = eventStore.loadEvents(aggregateId, AccountEvent.class, COLLECTION);
        assertThat(events).hasSize(3);
        assertThat(events.get(0)).isInstanceOf(AccountOpened.class);
        assertThat(events.get(1)).isInstanceOf(FundsDeposited.class);
        assertThat(events.get(2)).isInstanceOf(FundsWithdrawn.class);
        assertThat(events.get(0).version()).isEqualTo(1);
        assertThat(events.get(2).version()).isEqualTo(3);
    }

    @Test
    void duplicateVersion_throwsDuplicateKeyException() {
        String aggregateId = "ACC-TEST-2";
        var event1 = new AccountOpened(UUID.randomUUID().toString(), aggregateId, 1, Instant.now(),
                "測試用戶", new BigDecimal("10000"), "TWD");
        var event2 = new FundsDeposited(UUID.randomUUID().toString(), aggregateId, 1, Instant.now(),
                new BigDecimal("5000"), "存款");

        eventStore.append(event1, COLLECTION);

        assertThatThrownBy(() -> eventStore.append(event2, COLLECTION))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void saveSnapshot_and_loadLatestSnapshot_roundTrip() {
        String aggregateId = "ACC-TEST-3";
        var snapshot1 = new SnapshotDocument(
                UUID.randomUUID().toString(), aggregateId, "BankAccount", 3, Instant.now(),
                Map.of("accountId", aggregateId, "balance", "13000", "version", 3L));
        var snapshot2 = new SnapshotDocument(
                UUID.randomUUID().toString(), aggregateId, "BankAccount", 6, Instant.now(),
                Map.of("accountId", aggregateId, "balance", "20000", "version", 6L));

        eventStore.saveSnapshot(snapshot1);
        eventStore.saveSnapshot(snapshot2);

        var latest = eventStore.loadLatestSnapshot(aggregateId, "BankAccount");
        assertThat(latest).isPresent();
        assertThat(latest.get().version()).isEqualTo(6);
        assertThat(latest.get().state().get("balance")).isEqualTo("20000");
    }
}
