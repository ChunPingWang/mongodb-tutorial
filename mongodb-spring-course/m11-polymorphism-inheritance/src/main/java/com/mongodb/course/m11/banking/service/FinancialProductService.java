package com.mongodb.course.m11.banking.service;

import com.mongodb.course.m11.banking.model.Deposit;
import com.mongodb.course.m11.banking.model.FinancialProduct;
import com.mongodb.course.m11.banking.model.Fund;
import com.mongodb.course.m11.banking.model.InsuranceProduct;
import org.bson.types.Decimal128;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FinancialProductService {

    private static final String COLLECTION = "m11_financial_products";

    private final MongoTemplate mongoTemplate;

    public FinancialProductService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public <T extends FinancialProduct> T save(T product) {
        return mongoTemplate.save(product, COLLECTION);
    }

    public List<FinancialProduct> findAll() {
        return mongoTemplate.findAll(FinancialProduct.class, COLLECTION);
    }

    public <T extends FinancialProduct> List<T> findByType(Class<T> type) {
        TypeAlias alias = type.getAnnotation(TypeAlias.class);
        String classValue = alias != null ? alias.value() : type.getName();
        Query query = Query.query(Criteria.where("_class").is(classValue));
        return mongoTemplate.find(query, type, COLLECTION);
    }

    public List<Deposit> findAllDeposits() {
        return findByType(Deposit.class);
    }

    public List<Fund> findAllFunds() {
        return findByType(Fund.class);
    }

    public List<InsuranceProduct> findAllInsuranceProducts() {
        return findByType(InsuranceProduct.class);
    }

    public List<FinancialProduct> findByValueGreaterThan(BigDecimal minValue) {
        Query query = Query.query(Criteria.where("value").gt(new Decimal128(minValue)));
        return mongoTemplate.find(query, FinancialProduct.class, COLLECTION);
    }

    public BigDecimal estimateAnnualReturn(FinancialProduct product) {
        return switch (product) {
            case Deposit d -> d.value().multiply(d.annualRate())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            case Fund f -> f.value().multiply(BigDecimal.valueOf(0.08));
            case InsuranceProduct ip -> ip.coverage()
                    .divide(BigDecimal.valueOf(ip.premiumYears()), 2, java.math.RoundingMode.HALF_UP);
        };
    }
}
