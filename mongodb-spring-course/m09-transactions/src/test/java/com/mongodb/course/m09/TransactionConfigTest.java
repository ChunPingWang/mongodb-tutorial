package com.mongodb.course.m09;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class TransactionConfigTest {

    @Autowired
    ApplicationContext context;

    @Test
    void transactionManagerBeanExists() {
        assertThat(context.getBean(MongoTransactionManager.class)).isNotNull();
    }

    @Test
    void transactionManagerUsesCorrectFactory() {
        MongoTransactionManager txManager = context.getBean(MongoTransactionManager.class);
        MongoDatabaseFactory dbFactory = context.getBean(MongoDatabaseFactory.class);
        assertThat(txManager).isNotNull();
        assertThat(dbFactory).isNotNull();
    }
}
