package com.mongodb.course.m04.referenced;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRefRepository extends MongoRepository<CustomerRef, String> {
}
