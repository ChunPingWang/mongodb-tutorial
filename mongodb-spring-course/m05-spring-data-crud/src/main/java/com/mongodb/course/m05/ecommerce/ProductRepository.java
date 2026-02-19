package com.mongodb.course.m05.ecommerce;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByCategory(String category);

    Page<Product> findByCategory(String category, Pageable pageable);

    List<Product> findByInStockTrue();

    List<Product> findByTagsContaining(String tag);

    List<Product> findByNameContainingIgnoreCase(String keyword);

    List<Product> findByCategoryOrderByPriceAsc(String category);

    @Query("{ 'tags': { $all: ?0 } }")
    List<Product> findByAllTags(List<String> tags);
}
