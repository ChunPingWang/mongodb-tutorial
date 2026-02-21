package com.mongodb.course.m18.bdd;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class CucumberHooks {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Before
    public void cleanUp() {
        mongoTemplate.remove(new Query(), "m18_policies");
        mongoTemplate.remove(new Query(), "m18_customers");
    }
}
