package com.mongodb.course.m16.banking;

import com.mongodb.course.m16.SharedContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(SharedContainersConfig.class)
class AccountChangeStreamListenerTest {

    @Autowired
    private AccountChangeStreamListener listener;

    @Autowired
    private AccountService accountService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection("m16_accounts");
        mongoTemplate.dropCollection("m16_account_notifications");
    }

    @AfterEach
    void tearDown() {
        listener.stopListening();
    }

    @Test
    void insertAccount_triggersNotification() {
        listener.startListening();

        accountService.create("Alice", 10000);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var notifications = mongoTemplate.findAll(AccountNotification.class);
            assertThat(notifications).hasSize(1);
            assertThat(notifications.getFirst().operationType()).isEqualTo("insert");
            assertThat(notifications.getFirst().accountHolder()).isEqualTo("Alice");
            assertThat(notifications.getFirst().balance()).isEqualTo(10000L);
        });
    }

    @Test
    void updateBalance_capturesFullDocument() {
        var account = accountService.create("Bob", 5000);
        listener.startListening();

        accountService.updateBalance(account.id(), 8000);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var notifications = mongoTemplate.findAll(AccountNotification.class);
            assertThat(notifications).hasSize(1);
            assertThat(notifications.getFirst().operationType()).isEqualTo("update");
            assertThat(notifications.getFirst().accountHolder()).isEqualTo("Bob");
            assertThat(notifications.getFirst().balance()).isEqualTo(8000L);
        });
    }

    @Test
    void deleteAccount_triggersDeleteNotification() {
        var account = accountService.create("Charlie", 3000);
        listener.startListening();

        accountService.delete(account.id());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var notifications = mongoTemplate.findAll(AccountNotification.class);
            assertThat(notifications).hasSize(1);
            assertThat(notifications.getFirst().operationType()).isEqualTo("delete");
            assertThat(notifications.getFirst().accountHolder()).isNull();
            assertThat(notifications.getFirst().balance()).isNull();
        });
    }

    @Test
    void filterByInsertOnly_ignoresUpdates() {
        var account = accountService.create("Diana", 7000);
        listener.startListeningForInsertsOnly();

        accountService.updateBalance(account.id(), 9000);
        accountService.create("Eve", 2000);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var notifications = mongoTemplate.findAll(AccountNotification.class);
            assertThat(notifications).hasSize(1);
            assertThat(notifications.getFirst().accountHolder()).isEqualTo("Eve");
        });
    }

    @Test
    void multipleInserts_allCaptured() {
        listener.startListening();

        accountService.create("User1", 1000);
        accountService.create("User2", 2000);
        accountService.create("User3", 3000);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var notifications = mongoTemplate.findAll(AccountNotification.class);
            assertThat(notifications).hasSize(3);
        });
    }

    @Test
    void stopListening_noMoreNotifications() {
        listener.startListening();

        accountService.create("Frank", 4000);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var notifications = mongoTemplate.findAll(AccountNotification.class);
            assertThat(notifications).hasSize(1);
        });

        listener.stopListening();

        accountService.create("Grace", 5000);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var notifications = mongoTemplate.findAll(AccountNotification.class);
        assertThat(notifications).hasSize(1);
    }
}
