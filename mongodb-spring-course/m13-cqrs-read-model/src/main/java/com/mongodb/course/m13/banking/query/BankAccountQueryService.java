package com.mongodb.course.m13.banking.query;

import com.mongodb.course.m13.banking.readmodel.AccountSummaryDocument;
import com.mongodb.course.m13.banking.readmodel.TransactionHistoryDocument;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class BankAccountQueryService {

    private static final String SUMMARIES = "m13_account_summaries";
    private static final String TRANSACTIONS = "m13_transaction_history";

    private final MongoTemplate mongoTemplate;

    public BankAccountQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Optional<AccountSummaryDocument> getAccountSummary(String accountId) {
        return Optional.ofNullable(
                mongoTemplate.findById(accountId, AccountSummaryDocument.class, SUMMARIES));
    }

    public List<AccountSummaryDocument> getTopAccountsByBalance(int limit) {
        var query = new Query()
                .with(Sort.by(Sort.Direction.DESC, "currentBalance"))
                .limit(limit);
        return mongoTemplate.find(query, AccountSummaryDocument.class, SUMMARIES);
    }

    public List<TransactionHistoryDocument> getTransactionHistory(String accountId, int page, int pageSize) {
        var query = Query.query(Criteria.where("accountId").is(accountId))
                .with(Sort.by(Sort.Direction.DESC, "occurredAt"))
                .skip((long) page * pageSize)
                .limit(pageSize);
        return mongoTemplate.find(query, TransactionHistoryDocument.class, TRANSACTIONS);
    }

    public List<TransactionHistoryDocument> getTransactionsByType(String accountId, String type) {
        var query = Query.query(
                Criteria.where("accountId").is(accountId)
                        .and("transactionType").is(type))
                .with(Sort.by(Sort.Direction.DESC, "occurredAt"));
        return mongoTemplate.find(query, TransactionHistoryDocument.class, TRANSACTIONS);
    }

    public List<AccountSummaryDocument> getRecentlyActiveAccounts(Instant since) {
        var query = Query.query(Criteria.where("lastActivityAt").gte(since))
                .with(Sort.by(Sort.Direction.DESC, "lastActivityAt"));
        return mongoTemplate.find(query, AccountSummaryDocument.class, SUMMARIES);
    }

    public BigDecimal getTotalBalanceAcrossAccounts() {
        var aggregation = Aggregation.newAggregation(
                Aggregation.group().sum("currentBalance").as("total"));
        var result = mongoTemplate.aggregate(aggregation, SUMMARIES, java.util.Map.class);
        var mapped = result.getMappedResults();
        if (mapped.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(mapped.getFirst().get("total").toString());
    }
}
