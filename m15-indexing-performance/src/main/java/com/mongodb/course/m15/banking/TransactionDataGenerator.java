package com.mongodb.course.m15.banking;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Service
public class TransactionDataGenerator {

    private static final String[] CATEGORIES = {
            "salary", "utilities", "groceries", "entertainment", "transfer"
    };
    private static final TransactionType[] TYPES = TransactionType.values();

    private final MongoTemplate mongoTemplate;

    public TransactionDataGenerator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public int generateTransactions(int totalCount, int accountCount) {
        var transactions = new ArrayList<Transaction>(totalCount);
        var baseDate = Instant.parse("2024-01-01T00:00:00Z");

        for (int i = 0; i < totalCount; i++) {
            String accountId = String.format("ACC-%06d", (i % accountCount) + 1);
            var type = TYPES[i % TYPES.length];
            long amount = ((i % 50) + 1) * 100L;
            var date = baseDate.plus(i % 365, ChronoUnit.DAYS)
                    .plus(i % 24, ChronoUnit.HOURS);
            String category = CATEGORIES[i % CATEGORIES.length];
            String description = type.name().toLowerCase() + " transaction #" + i;

            transactions.add(Transaction.of(accountId, date, type, amount, description, category));
        }

        var inserted = mongoTemplate.insertAll(transactions);
        return inserted.size();
    }

    public int generateTemporaryTransactions(int count) {
        var transactions = new ArrayList<Transaction>(count);
        var now = Instant.now();

        for (int i = 0; i < count; i++) {
            String accountId = String.format("TEMP-%06d", i + 1);
            transactions.add(Transaction.ofWithCreatedAt(
                    accountId, now, TransactionType.FEE, 100L,
                    "temporary transaction #" + i, "temp", now));
        }

        var inserted = mongoTemplate.insertAll(transactions);
        return inserted.size();
    }
}
