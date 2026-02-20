package com.mongodb.course.m09;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.course.m09.banking.AccountType;
import com.mongodb.course.m09.banking.BankAccount;
import com.mongodb.course.m09.banking.TransferRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class ProgrammaticTransactionTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    MongoTransactionManager transactionManager;

    @Autowired
    MongoClient mongoClient;

    @Autowired
    MongoDatabaseFactory mongoDatabaseFactory;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), BankAccount.class);
        mongoTemplate.remove(new Query(), TransferRecord.class);
    }

    @Test
    void transactionTemplate_commitOnSuccess() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("50000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("10000")));

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                mongoTemplate.updateFirst(
                        Query.query(Criteria.where("accountNumber").is("A001")),
                        new Update().inc("balance", new BigDecimal("-15000")),
                        BankAccount.class
                );
                mongoTemplate.updateFirst(
                        Query.query(Criteria.where("accountNumber").is("A002")),
                        new Update().inc("balance", new BigDecimal("15000")),
                        BankAccount.class
                );
            }
        });

        BankAccount a1 = mongoTemplate.findOne(
                Query.query(Criteria.where("accountNumber").is("A001")), BankAccount.class);
        BankAccount a2 = mongoTemplate.findOne(
                Query.query(Criteria.where("accountNumber").is("A002")), BankAccount.class);
        assertThat(a1.getBalance()).isEqualByComparingTo(new BigDecimal("35000"));
        assertThat(a2.getBalance()).isEqualByComparingTo(new BigDecimal("25000"));
    }

    @Test
    void transactionTemplate_rollbackOnException() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("50000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("10000")));

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        try {
            txTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    mongoTemplate.updateFirst(
                            Query.query(Criteria.where("accountNumber").is("A001")),
                            new Update().inc("balance", new BigDecimal("-15000")),
                            BankAccount.class
                    );
                    // Simulate failure before crediting
                    throw new RuntimeException("Simulated failure");
                }
            });
        } catch (RuntimeException ignored) {
        }

        BankAccount a1 = mongoTemplate.findOne(
                Query.query(Criteria.where("accountNumber").is("A001")), BankAccount.class);
        BankAccount a2 = mongoTemplate.findOne(
                Query.query(Criteria.where("accountNumber").is("A002")), BankAccount.class);
        assertThat(a1.getBalance()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(a2.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void clientSession_manualCommitAndAbort() {
        mongoTemplate.insert(new BankAccount("A001", "Alice", AccountType.SAVINGS, new BigDecimal("50000")));
        mongoTemplate.insert(new BankAccount("A002", "Bob", AccountType.SAVINGS, new BigDecimal("10000")));

        // Test abort: start session, start transaction, make changes, abort
        try (ClientSession session = mongoClient.startSession()) {
            session.startTransaction();
            String dbName = mongoDatabaseFactory.getMongoDatabase().getName();
            var collection = mongoClient.getDatabase(dbName).getCollection("m09_bank_accounts");

            collection.updateOne(
                    session,
                    new org.bson.Document("accountNumber", "A001"),
                    new org.bson.Document("$inc", new org.bson.Document("balance", new org.bson.types.Decimal128(new BigDecimal("-20000"))))
            );

            session.abortTransaction();
        }

        // Verify abort: A001 balance unchanged
        BankAccount a1 = mongoTemplate.findOne(
                Query.query(Criteria.where("accountNumber").is("A001")), BankAccount.class);
        assertThat(a1.getBalance()).isEqualByComparingTo(new BigDecimal("50000"));

        // Test commit: start session, start transaction, make changes, commit
        try (ClientSession session = mongoClient.startSession()) {
            session.startTransaction();
            String dbName = mongoDatabaseFactory.getMongoDatabase().getName();
            var collection = mongoClient.getDatabase(dbName).getCollection("m09_bank_accounts");

            collection.updateOne(
                    session,
                    new org.bson.Document("accountNumber", "A001"),
                    new org.bson.Document("$inc", new org.bson.Document("balance", new org.bson.types.Decimal128(new BigDecimal("-10000"))))
            );
            collection.updateOne(
                    session,
                    new org.bson.Document("accountNumber", "A002"),
                    new org.bson.Document("$inc", new org.bson.Document("balance", new org.bson.types.Decimal128(new BigDecimal("10000"))))
            );

            session.commitTransaction();
        }

        // Verify commit: balances updated
        a1 = mongoTemplate.findOne(
                Query.query(Criteria.where("accountNumber").is("A001")), BankAccount.class);
        BankAccount a2 = mongoTemplate.findOne(
                Query.query(Criteria.where("accountNumber").is("A002")), BankAccount.class);
        assertThat(a1.getBalance()).isEqualByComparingTo(new BigDecimal("40000"));
        assertThat(a2.getBalance()).isEqualByComparingTo(new BigDecimal("20000"));
    }
}
