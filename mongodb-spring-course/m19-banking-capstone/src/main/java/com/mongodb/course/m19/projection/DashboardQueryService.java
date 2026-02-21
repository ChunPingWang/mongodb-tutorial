package com.mongodb.course.m19.projection;

import com.mongodb.course.m19.projection.readmodel.AccountSummaryDocument;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DashboardQueryService {

    private static final String SUMMARIES = "m19_account_summaries";
    private static final String LEDGER = "m19_transaction_ledger";

    private final MongoTemplate mongoTemplate;

    public DashboardQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<AccountSummaryDocument> topAccountsByBalance(int limit) {
        var query = new Query()
                .with(Sort.by(Sort.Direction.DESC, "currentBalance"))
                .limit(limit);
        return mongoTemplate.find(query, AccountSummaryDocument.class, SUMMARIES);
    }

    public AccountSummaryDocument getAccountSummary(String accountId) {
        return mongoTemplate.findById(accountId, AccountSummaryDocument.class, SUMMARIES);
    }

    public BigDecimal getAccountBalance(String accountId) {
        var summary = getAccountSummary(accountId);
        return summary != null ? summary.currentBalance() : BigDecimal.ZERO;
    }
}
