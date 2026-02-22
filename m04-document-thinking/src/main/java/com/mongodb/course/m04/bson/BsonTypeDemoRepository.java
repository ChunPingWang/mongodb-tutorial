package com.mongodb.course.m04.bson;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BsonTypeDemoRepository extends MongoRepository<BsonTypeDemoDocument, String> {
}
