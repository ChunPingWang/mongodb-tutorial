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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class TransferServiceTest {

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
    void successfulTransfer_debitsAndCredits() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("50000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("10000")));

        transferService.transfer("A001", "A002", new BigDecimal("20000"));

        BankAccount a1 = transferService.findAccountByNumber("A001");
        BankAccount a2 = transferService.findAccountByNumber("A002");
        assertThat(a1.getBalance()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(a2.getBalance()).isEqualByComparingTo(new BigDecimal("30000"));
    }

    @Test
    void successfulTransfer_createsTransferRecord() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("50000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("10000")));

        TransferRecord record = transferService.transfer("A001", "A002", new BigDecimal("20000"));

        assertThat(record.getId()).isNotNull();
        assertThat(record.getFromAccountNumber()).isEqualTo("A001");
        assertThat(record.getToAccountNumber()).isEqualTo("A002");
        assertThat(record.getAmount()).isEqualByComparingTo(new BigDecimal("20000"));
        assertThat(record.getStatus()).isEqualTo(TransferRecord.TransferStatus.SUCCESS);
    }

    @Test
    void transferBetweenSameAccount_rejected() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("50000")));

        assertThatThrownBy(() -> transferService.transfer("A001", "A001", new BigDecimal("1000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same account");
    }

    @Test
    void zeroAmountTransfer_rejected() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("50000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("10000")));

        assertThatThrownBy(() -> transferService.transfer("A001", "A002", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void negativeAmountTransfer_rejected() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("50000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("10000")));

        assertThatThrownBy(() -> transferService.transfer("A001", "A002", new BigDecimal("-1000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void transferToNonExistentAccount_throwsException() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("50000")));

        assertThatThrownBy(() -> transferService.transfer("A001", "A999", new BigDecimal("1000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target account not found");

        BankAccount a1 = transferService.findAccountByNumber("A001");
        assertThat(a1.getBalance()).isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    void multipleSequentialTransfers_correctFinalBalances() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("100000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("50000")));
        mongoTemplate.insert(new BankAccount("A003", "Charlie", AccountType.CHECKING, new BigDecimal("30000")));

        transferService.transfer("A001", "A002", new BigDecimal("20000"));
        transferService.transfer("A002", "A003", new BigDecimal("15000"));
        transferService.transfer("A003", "A001", new BigDecimal("5000"));

        BankAccount a1 = transferService.findAccountByNumber("A001");
        BankAccount a2 = transferService.findAccountByNumber("A002");
        BankAccount a3 = transferService.findAccountByNumber("A003");
        assertThat(a1.getBalance()).isEqualByComparingTo(new BigDecimal("85000"));
        assertThat(a2.getBalance()).isEqualByComparingTo(new BigDecimal("55000"));
        assertThat(a3.getBalance()).isEqualByComparingTo(new BigDecimal("40000"));
    }

    @Test
    void transferHistory_recordsBothDirections() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("100000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("100000")));

        transferService.transfer("A001", "A002", new BigDecimal("10000"));
        transferService.transfer("A002", "A001", new BigDecimal("5000"));

        List<TransferRecord> historyA001 = transferService.getTransferHistory("A001");
        assertThat(historyA001).hasSize(2);

        List<TransferRecord> historyA002 = transferService.getTransferHistory("A002");
        assertThat(historyA002).hasSize(2);
    }
}
