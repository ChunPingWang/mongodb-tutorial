package com.mongodb.course.m21.bdd;

import com.mongodb.course.m21.config.SchemaValidationConfig;
import com.mongodb.course.m21.notification.OrderNotificationListener;
import com.mongodb.course.m21.observability.SlowQueryDetector;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class CucumberHooks {

    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private SlowQueryDetector slowQueryDetector;
    @Autowired private OrderNotificationListener notificationListener;
    @Autowired private SchemaValidationConfig schemaValidationConfig;

    @Before
    public void cleanUp() {
        mongoTemplate.remove(new Query(), "m21_order_events");
        mongoTemplate.remove(new Query(), "m21_snapshots");
        mongoTemplate.remove(new Query(), "m21_order_dashboard");
        mongoTemplate.remove(new Query(), "m21_sales_statistics");
        mongoTemplate.remove(new Query(), "m21_fulfillment_saga_logs");
        mongoTemplate.remove(new Query(), "m21_order_notifications");
        if (mongoTemplate.collectionExists("m21_products")) {
            mongoTemplate.dropCollection("m21_products");
        }
        schemaValidationConfig.init();
        slowQueryDetector.clear();
        notificationListener.clear();
    }

    @After
    public void tearDown() {
        notificationListener.stop();
    }
}
