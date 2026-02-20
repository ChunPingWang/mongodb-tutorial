package com.mongodb.course.m11.banking;

import com.mongodb.course.m11.SharedContainersConfig;
import com.mongodb.course.m11.banking.model.Deposit;
import com.mongodb.course.m11.banking.model.FinancialProduct;
import com.mongodb.course.m11.banking.model.Fund;
import com.mongodb.course.m11.banking.model.InsuranceProduct;
import com.mongodb.course.m11.banking.model.RiskProfile;
import com.mongodb.course.m11.banking.service.FinancialProductService;
import org.bson.Document;
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
class FinancialProductServiceTest {

    private static final String COLLECTION = "m11_financial_products";

    @Autowired
    private FinancialProductService service;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(COLLECTION);
    }

    @Test
    void save_deposit_persistsWithTypeAlias() {
        var deposit = new Deposit(null, "一年定存", BigDecimal.valueOf(100000),
                BigDecimal.valueOf(1.5), 12);

        Deposit saved = service.save(deposit);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.name()).isEqualTo("一年定存");
    }

    @Test
    void save_fund_persistsWithCustomConverter() {
        var fund = new Fund(null, "全球股票", BigDecimal.valueOf(50000),
                BigDecimal.valueOf(15.32), new RiskProfile(5, "AGGRESSIVE"));

        Fund saved = service.save(fund);

        // Verify custom converter stored RiskProfile as "5:AGGRESSIVE"
        Document raw = mongoTemplate.getCollection(COLLECTION)
                .find(new Document("_id", new org.bson.types.ObjectId(saved.id())))
                .first();
        assertThat(raw).isNotNull();
        assertThat(raw.getString("riskProfile")).isEqualTo("5:AGGRESSIVE");
    }

    @Test
    void save_insuranceProduct_persistsCorrectly() {
        var insurance = new InsuranceProduct(null, "年金險", BigDecimal.valueOf(200000),
                20, BigDecimal.valueOf(1000000));

        InsuranceProduct saved = service.save(insurance);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.premiumYears()).isEqualTo(20);
    }

    @Test
    void findAll_returnsAllTypes() {
        service.save(new Deposit(null, "定存", BigDecimal.valueOf(100000),
                BigDecimal.valueOf(1.5), 12));
        service.save(new Fund(null, "基金", BigDecimal.valueOf(50000),
                BigDecimal.valueOf(10.0), new RiskProfile(3, "MODERATE")));
        service.save(new InsuranceProduct(null, "保險", BigDecimal.valueOf(200000),
                20, BigDecimal.valueOf(1000000)));

        List<FinancialProduct> all = service.findAll();

        assertThat(all).hasSize(3);
        assertThat(all).hasAtLeastOneElementOfType(Deposit.class);
        assertThat(all).hasAtLeastOneElementOfType(Fund.class);
        assertThat(all).hasAtLeastOneElementOfType(InsuranceProduct.class);
    }

    @Test
    void findByType_returnsOnlyMatchingType() {
        service.save(new Deposit(null, "定存A", BigDecimal.valueOf(100000),
                BigDecimal.valueOf(1.5), 12));
        service.save(new Deposit(null, "定存B", BigDecimal.valueOf(200000),
                BigDecimal.valueOf(2.0), 24));
        service.save(new Fund(null, "基金", BigDecimal.valueOf(50000),
                BigDecimal.valueOf(10.0), new RiskProfile(3, "MODERATE")));

        List<Deposit> deposits = service.findByType(Deposit.class);

        assertThat(deposits).hasSize(2);
        assertThat(deposits).allSatisfy(d -> assertThat(d).isInstanceOf(Deposit.class));
    }

    @Test
    void typeAlias_rawBsonHasShortAlias() {
        service.save(new Deposit(null, "定存", BigDecimal.valueOf(100000),
                BigDecimal.valueOf(1.5), 12));

        Document raw = mongoTemplate.getCollection(COLLECTION).find().first();

        assertThat(raw).isNotNull();
        assertThat(raw.getString("_class")).isEqualTo("deposit");
    }

    @Test
    void findByValueGreaterThan_worksAcrossTypes() {
        service.save(new Deposit(null, "小額定存", BigDecimal.valueOf(10000),
                BigDecimal.valueOf(1.5), 12));
        service.save(new Fund(null, "大額基金", BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10.0), new RiskProfile(4, "GROWTH")));
        service.save(new InsuranceProduct(null, "中額保險", BigDecimal.valueOf(100000),
                20, BigDecimal.valueOf(1000000)));

        List<FinancialProduct> results = service.findByValueGreaterThan(BigDecimal.valueOf(50000));

        assertThat(results).hasSize(2);
        assertThat(results).extracting(FinancialProduct::name)
                .containsExactlyInAnyOrder("大額基金", "中額保險");
    }
}
