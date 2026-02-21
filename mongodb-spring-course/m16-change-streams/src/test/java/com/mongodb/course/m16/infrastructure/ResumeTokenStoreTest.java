package com.mongodb.course.m16.infrastructure;

import com.mongodb.course.m16.SharedContainersConfig;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class ResumeTokenStoreTest {

    @Autowired
    private ResumeTokenStore resumeTokenStore;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection("m16_resume_tokens");
    }

    @Test
    void saveAndLoad_roundTrip() {
        var token = new BsonDocument("_data", new BsonString("resume-token-value-1"));

        resumeTokenStore.saveToken("test-listener", token);
        var loaded = resumeTokenStore.loadToken("test-listener");

        assertThat(loaded).isNotNull();
        assertThat(loaded).isEqualTo(token);
    }

    @Test
    void save_updatesExistingToken() {
        var token1 = new BsonDocument("_data", new BsonString("token-v1"));
        var token2 = new BsonDocument("_data", new BsonString("token-v2"));

        resumeTokenStore.saveToken("test-listener", token1);
        resumeTokenStore.saveToken("test-listener", token2);

        var loaded = resumeTokenStore.loadToken("test-listener");
        assertThat(loaded).isEqualTo(token2);
    }

    @Test
    void loadNonExistentToken_returnsNull() {
        var loaded = resumeTokenStore.loadToken("non-existent");
        assertThat(loaded).isNull();
    }
}
