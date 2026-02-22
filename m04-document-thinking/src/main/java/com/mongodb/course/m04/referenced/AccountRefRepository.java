package com.mongodb.course.m04.referenced;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AccountRefRepository extends MongoRepository<AccountRef, String> {

    List<AccountRef> findByCustomerId(String customerId);
}
