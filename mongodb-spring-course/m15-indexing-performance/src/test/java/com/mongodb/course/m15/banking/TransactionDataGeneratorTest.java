package com.mongodb.course.m15.banking;

import com.mongodb.course.m15.SharedContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class TransactionDataGeneratorTest {

    @Autowired
    private TransactionDataGenerator generator;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), "m15_transactions");
    }

    @Test
    void generateTransactions_insertsCorrectCount() {
        int inserted = generator.generateTransactions(500, 10);

        assertThat(inserted).isEqualTo(500);
        assertThat(mongoTemplate.count(new Query(), "m15_transactions")).isEqualTo(500);
    }
}
