package com.mongodb.course.m13.projection;

import com.mongodb.course.m13.banking.event.AccountEvent;
import com.mongodb.course.m13.banking.projection.AccountSummaryProjector;
import com.mongodb.course.m13.banking.projection.TransactionHistoryProjector;
import com.mongodb.course.m13.infrastructure.EventStore;
import com.mongodb.course.m13.insurance.event.ClaimEvent;
import com.mongodb.course.m13.insurance.projection.ClaimDashboardProjector;
import com.mongodb.course.m13.insurance.projection.ClaimStatisticsProjector;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class ProjectionRebuildService {

    private static final String ACCOUNT_EVENTS = "m13_account_events";
    private static final String CLAIM_EVENTS = "m13_claim_events";

    private final EventStore eventStore;
    private final MongoTemplate mongoTemplate;
    private final AccountSummaryProjector accountSummaryProjector;
    private final TransactionHistoryProjector transactionHistoryProjector;
    private final ClaimDashboardProjector claimDashboardProjector;
    private final ClaimStatisticsProjector claimStatisticsProjector;

    public ProjectionRebuildService(EventStore eventStore,
                                    MongoTemplate mongoTemplate,
                                    AccountSummaryProjector accountSummaryProjector,
                                    TransactionHistoryProjector transactionHistoryProjector,
                                    ClaimDashboardProjector claimDashboardProjector,
                                    ClaimStatisticsProjector claimStatisticsProjector) {
        this.eventStore = eventStore;
        this.mongoTemplate = mongoTemplate;
        this.accountSummaryProjector = accountSummaryProjector;
        this.transactionHistoryProjector = transactionHistoryProjector;
        this.claimDashboardProjector = claimDashboardProjector;
        this.claimStatisticsProjector = claimStatisticsProjector;
    }

    public void rebuildBankingProjections() {
        var events = eventStore.loadAllEvents(AccountEvent.class, ACCOUNT_EVENTS);
        accountSummaryProjector.rebuildAll(events);
        transactionHistoryProjector.rebuildAll(events);
    }

    public void rebuildInsuranceProjections() {
        var events = eventStore.loadAllEvents(ClaimEvent.class, CLAIM_EVENTS);
        claimDashboardProjector.rebuildAll(events);
        claimStatisticsProjector.rebuildAll(events);
    }

    public void rebuildAll() {
        rebuildBankingProjections();
        rebuildInsuranceProjections();
    }

    public void clearBankingReadModels() {
        mongoTemplate.remove(new Query(), "m13_account_summaries");
        mongoTemplate.remove(new Query(), "m13_transaction_history");
    }

    public void clearInsuranceReadModels() {
        mongoTemplate.remove(new Query(), "m13_claim_dashboards");
        mongoTemplate.remove(new Query(), "m13_claim_statistics");
    }
}
