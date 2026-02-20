package com.mongodb.course.m06;

import com.mongodb.course.m06.banking.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LAB-01: Criteria API 基礎查詢 — Banking domain
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CriteriaApiBasicTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BankAccountQueryService queryService;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(BankAccount.class);

        // 測試資料：6 個帳戶
        BankAccount a1 = new BankAccount("ACC-001", "Alice Wang", AccountType.SAVINGS, new BigDecimal("50000"));
        a1.setStatus(AccountStatus.ACTIVE);

        BankAccount a2 = new BankAccount("ACC-002", "Bob Chen", AccountType.CHECKING, new BigDecimal("15000"));
        a2.setStatus(AccountStatus.ACTIVE);

        BankAccount a3 = new BankAccount("ACC-003", "Charlie Lin", AccountType.SAVINGS, new BigDecimal("80000"));
        a3.setStatus(AccountStatus.FROZEN);

        BankAccount a4 = new BankAccount("ACC-004", "Alice Lee", AccountType.CHECKING, new BigDecimal("3000"));
        a4.setStatus(AccountStatus.CLOSED);

        BankAccount a5 = new BankAccount("ACC-005", "David Wu", AccountType.SAVINGS, new BigDecimal("120000"));
        a5.setStatus(AccountStatus.ACTIVE);

        BankAccount a6 = new BankAccount("ACC-006", "Amy Chang", AccountType.CHECKING, new BigDecimal("25000"));
        a6.setStatus(AccountStatus.ACTIVE);

        mongoTemplate.insertAll(List.of(a1, a2, a3, a4, a5, a6));
    }

    @Test
    @Order(1)
    void findByStatus_singleCriteria() {
        List<BankAccount> activeAccounts = queryService.findByStatus(AccountStatus.ACTIVE);
        assertThat(activeAccounts).hasSize(4);
        assertThat(activeAccounts).allMatch(a -> a.getStatus() == AccountStatus.ACTIVE);
    }

    @Test
    @Order(2)
    void findByTypeAndStatus_chainedAnd() {
        List<BankAccount> results = queryService.findByTypeAndStatus(AccountType.SAVINGS, AccountStatus.ACTIVE);
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(a -> a.getType() == AccountType.SAVINGS && a.getStatus() == AccountStatus.ACTIVE);
    }

    @Test
    @Order(3)
    void findByBalanceRange_gteAndLte() {
        List<BankAccount> results = queryService.findByBalanceRange(
                new BigDecimal("10000"), new BigDecimal("60000"));
        assertThat(results).hasSize(3); // 50000, 15000, 25000
        assertThat(results).allMatch(a ->
                a.getBalance().compareTo(new BigDecimal("10000")) >= 0 &&
                        a.getBalance().compareTo(new BigDecimal("60000")) <= 0);
    }

    @Test
    @Order(4)
    void findByHolderNameRegex() {
        List<BankAccount> results = queryService.findByHolderNameRegex(".*Wang.*");
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getHolderName()).isEqualTo("Alice Wang");
    }

    @Test
    @Order(5)
    void findByHolderNameStartingWith() {
        List<BankAccount> results = queryService.findByHolderNameStartingWith("A");
        assertThat(results).hasSize(3); // Alice Wang, Alice Lee, Amy Chang
    }

    @Test
    @Order(6)
    void countByStatusAndType() {
        long count = queryService.countByStatusAndType(AccountStatus.ACTIVE, AccountType.SAVINGS);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @Order(7)
    void existsCheck_withCriteria() {
        // existsByPolicyNumber is on Insurance, so we use count as exists proxy
        long count = queryService.countByStatusAndType(AccountStatus.FROZEN, AccountType.SAVINGS);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @Order(8)
    void findWithSortByBalance() {
        List<BankAccount> results = queryService.findActiveAccountsSorted("balance", true);
        assertThat(results).hasSize(4);
        // Ascending: 15000, 25000, 50000, 120000
        assertThat(results.get(0).getBalance()).isEqualByComparingTo(new BigDecimal("15000"));
        assertThat(results.get(3).getBalance()).isEqualByComparingTo(new BigDecimal("120000"));
    }

    @Test
    @Order(9)
    void findWithPagination() {
        // 4 active accounts, page size 2, page 0
        List<BankAccount> page0 = queryService.findActiveAccountsPaged(0, 2);
        assertThat(page0).hasSize(2);

        List<BankAccount> page1 = queryService.findActiveAccountsPaged(1, 2);
        assertThat(page1).hasSize(2);

        List<BankAccount> page2 = queryService.findActiveAccountsPaged(2, 2);
        assertThat(page2).isEmpty();
    }

    @Test
    @Order(10)
    void findActiveAccountsSortedAndPaged() {
        // Sort by balance DESC, take first page of 2
        List<BankAccount> sorted = queryService.findActiveAccountsSorted("balance", false);
        assertThat(sorted).hasSize(4);
        assertThat(sorted.get(0).getBalance()).isEqualByComparingTo(new BigDecimal("120000"));

        List<BankAccount> paged = queryService.findActiveAccountsPaged(0, 2);
        assertThat(paged).hasSize(2);
    }
}
