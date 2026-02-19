package com.mongodb.course.m04.embedded;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BankCustomerEmbeddedRepository extends MongoRepository<BankCustomerDocument, String> {

    List<BankCustomerDocument> findByName(String name);
}
