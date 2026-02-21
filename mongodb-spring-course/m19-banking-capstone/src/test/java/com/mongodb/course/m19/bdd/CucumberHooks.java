package com.mongodb.course.m19.bdd;

import com.mongodb.course.m19.notification.TransferNotificationListener;
import com.mongodb.course.m19.observability.SlowQueryDetector;
import io.cucumber.java.Before;
import io.cucumber.java.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class CucumberHooks {

    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private SlowQueryDetector slowQueryDetector;
    @Autowired private TransferNotificationListener notificationListener;

    @Before
    public void cleanUp() {
        mongoTemplate.remove(new Query(), "m19_account_events");
        mongoTemplate.remove(new Query(), "m19_snapshots");
        mongoTemplate.remove(new Query(), "m19_account_summaries");
        mongoTemplate.remove(new Query(), "m19_transaction_ledger");
        mongoTemplate.remove(new Query(), "m19_transfer_saga_logs");
        mongoTemplate.remove(new Query(), "m19_transfer_notifications");
        mongoTemplate.remove(new Query(), "m19_loan_applications");
        slowQueryDetector.clear();
        notificationListener.clear();
    }

    @After
    public void tearDown() {
        notificationListener.stop();
    }
}
