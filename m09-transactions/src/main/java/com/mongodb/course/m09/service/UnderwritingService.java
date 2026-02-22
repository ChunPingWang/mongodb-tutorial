package com.mongodb.course.m09.service;

import com.mongodb.course.m09.insurance.BillingSchedule;
import com.mongodb.course.m09.insurance.CustomerStatus;
import com.mongodb.course.m09.insurance.InsuranceCustomer;
import com.mongodb.course.m09.insurance.InsurancePolicy;
import com.mongodb.course.m09.insurance.UnderwritingException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnderwritingService {

    private final MongoTemplate mongoTemplate;

    public UnderwritingService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Transactional
    public InsurancePolicy underwrite(String customerNumber, InsurancePolicy policy, BillingSchedule billingSchedule) {
        // Validate customer exists and is eligible
        InsuranceCustomer customer = mongoTemplate.findOne(
                Query.query(Criteria.where("customerNumber").is(customerNumber)),
                InsuranceCustomer.class
        );

        if (customer == null) {
            throw new UnderwritingException("Customer not found: " + customerNumber);
        }
        if (customer.getStatus() == CustomerStatus.SUSPENDED) {
            throw new UnderwritingException("Customer is suspended: " + customerNumber);
        }

        // Insert policy
        InsurancePolicy savedPolicy = mongoTemplate.insert(policy);

        // Insert billing schedule
        mongoTemplate.insert(billingSchedule);

        // Update customer: status → ACTIVE, policyCount++
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("customerNumber").is(customerNumber)),
                new Update()
                        .set("status", CustomerStatus.ACTIVE)
                        .inc("policyCount", 1),
                InsuranceCustomer.class
        );

        return savedPolicy;
    }
}
