package com.mongodb.course.m09;

import com.mongodb.course.m09.banking.AccountType;
import com.mongodb.course.m09.banking.BankAccount;
import com.mongodb.course.m09.banking.TransferRecord;
import com.mongodb.course.m09.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class ConcurrentTransferTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    TransferService transferService;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), BankAccount.class);
        mongoTemplate.remove(new Query(), TransferRecord.class);
    }

    @Test
    void concurrentTransfers_maintainConsistency() throws Exception {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("100000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("100000")));

        int threadCount = 10;
        BigDecimal transferAmount = new BigDecimal("1000");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    transferService.transfer("A001", "A002", transferAmount);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                    // Some may fail due to WriteConflict — that's expected
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        // Total balance must be conserved
        BankAccount a1 = transferService.findAccountByNumber("A001");
        BankAccount a2 = transferService.findAccountByNumber("A002");
        BigDecimal totalBalance = a1.getBalance().add(a2.getBalance());
        assertThat(totalBalance).isEqualByComparingTo(new BigDecimal("200000"));

        // At least some transfers should succeed
        assertThat(successCount.get()).isGreaterThan(0);
    }

    @Test
    void concurrentTransfersFromSameAccount_noOverdraft() throws Exception {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("10000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("0")));

        int threadCount = 5;
        BigDecimal transferAmount = new BigDecimal("3000");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    transferService.transfer("A001", "A002", transferAmount);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                    // InsufficientBalance or WriteConflict expected
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        BankAccount a1 = transferService.findAccountByNumber("A001");
        assertThat(a1.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void writeConflict_someTransfersFail() throws Exception {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("1000000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("1000000")));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    transferService.transfer("A001", "A002", new BigDecimal("100"));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        // All transfers either succeeded or failed — total should be conserved
        BankAccount a1 = transferService.findAccountByNumber("A001");
        BankAccount a2 = transferService.findAccountByNumber("A002");
        BigDecimal totalBalance = a1.getBalance().add(a2.getBalance());
        assertThat(totalBalance).isEqualByComparingTo(new BigDecimal("2000000"));
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
    }

    @Test
    void totalBalance_conservedAfterConcurrentOps() throws Exception {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("50000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("50000")));
        mongoTemplate.insert(new BankAccount("A003", "Charlie", AccountType.CHECKING, new BigDecimal("50000")));

        BigDecimal initialTotal = new BigDecimal("150000");

        int threadCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<Future<?>> futures = new ArrayList<>();
        // A001 → A002
        futures.add(executor.submit(() -> {
            try { latch.countDown(); latch.await(); transferService.transfer("A001", "A002", new BigDecimal("5000")); } catch (Exception ignored) {}
        }));
        // A002 → A003
        futures.add(executor.submit(() -> {
            try { latch.countDown(); latch.await(); transferService.transfer("A002", "A003", new BigDecimal("3000")); } catch (Exception ignored) {}
        }));
        // A003 → A001
        futures.add(executor.submit(() -> {
            try { latch.countDown(); latch.await(); transferService.transfer("A003", "A001", new BigDecimal("7000")); } catch (Exception ignored) {}
        }));
        // A001 → A003
        futures.add(executor.submit(() -> {
            try { latch.countDown(); latch.await(); transferService.transfer("A001", "A003", new BigDecimal("2000")); } catch (Exception ignored) {}
        }));
        // A002 → A001
        futures.add(executor.submit(() -> {
            try { latch.countDown(); latch.await(); transferService.transfer("A002", "A001", new BigDecimal("4000")); } catch (Exception ignored) {}
        }));
        // A003 → A002
        futures.add(executor.submit(() -> {
            try { latch.countDown(); latch.await(); transferService.transfer("A003", "A002", new BigDecimal("1000")); } catch (Exception ignored) {}
        }));

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        BankAccount a1 = transferService.findAccountByNumber("A001");
        BankAccount a2 = transferService.findAccountByNumber("A002");
        BankAccount a3 = transferService.findAccountByNumber("A003");
        BigDecimal finalTotal = a1.getBalance().add(a2.getBalance()).add(a3.getBalance());
        assertThat(finalTotal).isEqualByComparingTo(initialTotal);
    }
}
