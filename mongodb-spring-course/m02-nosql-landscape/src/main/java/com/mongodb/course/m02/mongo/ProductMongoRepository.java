package com.mongodb.course.m02.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductMongoRepository extends MongoRepository<ProductDocument, String> {

    List<ProductDocument> findByCategory(String category);

    List<ProductDocument> findByNameContaining(String keyword);
}
