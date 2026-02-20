package com.mongodb.course.m11.insurance;

import com.mongodb.course.m11.SharedContainersConfig;
import com.mongodb.course.m11.insurance.model.AutoPolicy;
import com.mongodb.course.m11.insurance.model.HealthPolicy;
import com.mongodb.course.m11.insurance.model.LifePolicy;
import com.mongodb.course.m11.insurance.model.Policy;
import com.mongodb.course.m11.insurance.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class PolicyServiceTest {

    private static final String COLLECTION = "m11_policies";

    @Autowired
    private PolicyService service;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(COLLECTION);
    }

    @Test
    void save_autoPolicy_persistsWithTypeAlias() {
        var policy = new AutoPolicy(null, "POL-A01", "王小明",
                BigDecimal.valueOf(12000), "sedan", 30);

        AutoPolicy saved = service.save(policy);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPolicyNumber()).isEqualTo("POL-A01");

        // Verify TypeAlias in raw BSON
        var raw = mongoTemplate.getCollection(COLLECTION).find().first();
        assertThat(raw).isNotNull();
        assertThat(raw.getString("_class")).isEqualTo("auto");
    }

    @Test
    void save_lifePolicy_persistsCorrectly() {
        var policy = new LifePolicy(null, "POL-L01", "李小花",
                BigDecimal.valueOf(20000), 40, 20, BigDecimal.valueOf(3000000));

        LifePolicy saved = service.save(policy);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getInsuredAge()).isEqualTo(40);
    }

    @Test
    void save_healthPolicy_persistsCorrectly() {
        var policy = new HealthPolicy(null, "POL-H01", "張大山",
                BigDecimal.valueOf(8000), true, true, 5000);

        HealthPolicy saved = service.save(policy);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isHasDentalCoverage()).isTrue();
    }

    @Test
    void findAll_returnsAllPolicyTypes() {
        service.save(new AutoPolicy(null, "POL-A01", "王小明",
                BigDecimal.valueOf(12000), "sedan", 30));
        service.save(new LifePolicy(null, "POL-L01", "李小花",
                BigDecimal.valueOf(20000), 40, 20, BigDecimal.valueOf(3000000)));
        service.save(new HealthPolicy(null, "POL-H01", "張大山",
                BigDecimal.valueOf(8000), true, true, 5000));

        List<Policy> all = service.findAll();

        assertThat(all).hasSize(3);
        assertThat(all).hasAtLeastOneElementOfType(AutoPolicy.class);
        assertThat(all).hasAtLeastOneElementOfType(LifePolicy.class);
        assertThat(all).hasAtLeastOneElementOfType(HealthPolicy.class);
    }

    @Test
    void findByType_returnsOnlyMatchingType() {
        service.save(new AutoPolicy(null, "POL-A01", "王小明",
                BigDecimal.valueOf(12000), "sedan", 30));
        service.save(new AutoPolicy(null, "POL-A02", "李大明",
                BigDecimal.valueOf(15000), "truck", 35));
        service.save(new LifePolicy(null, "POL-L01", "李小花",
                BigDecimal.valueOf(20000), 40, 20, BigDecimal.valueOf(3000000)));

        List<AutoPolicy> autoPolicies = service.findByType(AutoPolicy.class);

        assertThat(autoPolicies).hasSize(2);
        assertThat(autoPolicies).allSatisfy(p -> assertThat(p).isInstanceOf(AutoPolicy.class));
    }
}
