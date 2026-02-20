package com.mongodb.course.m09;

import com.mongodb.course.m09.banking.AccountStatus;
import com.mongodb.course.m09.banking.AccountType;
import com.mongodb.course.m09.banking.BankAccount;
import com.mongodb.course.m09.banking.InsufficientBalanceException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class TransferRollbackTest {

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
    void insufficientBalance_rollsBackAllChanges() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("5000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("10000")));

        assertThatThrownBy(() -> transferService.transfer("A001", "A002", new BigDecimal("20000")))
                .isInstanceOf(InsufficientBalanceException.class);

        BankAccount a1 = transferService.findAccountByNumber("A001");
        BankAccount a2 = transferService.findAccountByNumber("A002");
        assertThat(a1.getBalance()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(a2.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void insufficientBalance_noTransferRecordCreated() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("5000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("10000")));

        assertThatThrownBy(() -> transferService.transfer("A001", "A002", new BigDecimal("20000")))
                .isInstanceOf(InsufficientBalanceException.class);

        long recordCount = mongoTemplate.count(new Query(), TransferRecord.class);
        assertThat(recordCount).isZero();
    }

    @Test
    void frozenAccount_rollsBack() {
        BankAccount frozen = new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("50000"));
        frozen.setStatus(AccountStatus.FROZEN);
        mongoTemplate.insert(frozen);
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("10000")));

        assertThatThrownBy(() -> transferService.transfer("A001", "A002", new BigDecimal("10000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("frozen");

        BankAccount a1 = transferService.findAccountByNumber("A001");
        BankAccount a2 = transferService.findAccountByNumber("A002");
        assertThat(a1.getBalance()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(a2.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void closedAccount_rollsBack() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("50000")));
        BankAccount closed = new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("10000"));
        closed.setStatus(AccountStatus.CLOSED);
        mongoTemplate.insert(closed);

        assertThatThrownBy(() -> transferService.transfer("A001", "A002", new BigDecimal("10000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");

        BankAccount a1 = transferService.findAccountByNumber("A001");
        BankAccount a2 = transferService.findAccountByNumber("A002");
        assertThat(a1.getBalance()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(a2.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void runtimeException_triggersRollback() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("50000")));
        // No A002 → will throw IllegalArgumentException (target not found)

        assertThatThrownBy(() -> transferService.transfer("A001", "A002", new BigDecimal("10000")))
                .isInstanceOf(IllegalArgumentException.class);

        BankAccount a1 = transferService.findAccountByNumber("A001");
        assertThat(a1.getBalance()).isEqualByComparingTo(new BigDecimal("50000"));
        long recordCount = mongoTemplate.count(new Query(), TransferRecord.class);
        assertThat(recordCount).isZero();
    }

    @Test
    void sourceNotFound_rollsBack() {
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("10000")));

        assertThatThrownBy(() -> transferService.transfer("A999", "A002", new BigDecimal("1000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source account not found");

        BankAccount a2 = transferService.findAccountByNumber("A002");
        assertThat(a2.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
    }
}
