package com.mongodb.course.m16.bdd;

import com.mongodb.course.m16.banking.AccountChangeStreamListener;
import com.mongodb.course.m16.ecommerce.OrderChangeStreamWatcher;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

public class CucumberHooks {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AccountChangeStreamListener listener;

    @Autowired
    private OrderChangeStreamWatcher watcher;

    @Before
    public void setUp() {
        mongoTemplate.dropCollection("m16_accounts");
        mongoTemplate.dropCollection("m16_account_notifications");
        mongoTemplate.dropCollection("m16_orders");
        mongoTemplate.dropCollection("m16_order_audit_log");
        mongoTemplate.dropCollection("m16_resume_tokens");
    }

    @After
    public void tearDown() {
        listener.stopListening();
        watcher.stopWatching();
    }
}
