package com.mongodb.course.m17.bdd;

import com.mongodb.course.m17.observability.SlowQueryDetector;
import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class CucumberHooks {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SlowQueryDetector slowQueryDetector;

    @Before
    public void cleanUp() {
        mongoTemplate.remove(new Query(), "m17_transactions");
        mongoTemplate.remove(new Query(), "m17_products");
        slowQueryDetector.clear();
    }
}
