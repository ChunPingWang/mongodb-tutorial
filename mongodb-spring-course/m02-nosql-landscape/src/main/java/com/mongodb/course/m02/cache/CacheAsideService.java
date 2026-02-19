package com.mongodb.course.m02.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.course.m02.mongo.ProductDocument;
import com.mongodb.course.m02.mongo.ProductMongoRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheAsideService {

    private static final String CACHE_PREFIX = "product:";

    private final ProductMongoRepository mongoRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheAsideService(ProductMongoRepository mongoRepository,
                             StringRedisTemplate redisTemplate) {
        this.mongoRepository = mongoRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public ProductDocument findById(String id) {
        String cacheKey = CACHE_PREFIX + id;

        // 1. Check Redis cache
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserialize(cached);
        }

        // 2. Cache miss — fetch from MongoDB
        ProductDocument product = mongoRepository.findById(id).orElse(null);
        if (product != null) {
            // 3. Populate cache
            redisTemplate.opsForValue().set(cacheKey, serialize(product));
        }
        return product;
    }

    public ProductDocument save(ProductDocument product) {
        // 1. Write to MongoDB
        ProductDocument saved = mongoRepository.save(product);
        // 2. Invalidate cache
        redisTemplate.delete(CACHE_PREFIX + saved.getId());
        return saved;
    }

    public boolean isCached(String id) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CACHE_PREFIX + id));
    }

    private String serialize(ProductDocument product) {
        try {
            return objectMapper.writeValueAsString(product);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize product", e);
        }
    }

    private ProductDocument deserialize(String json) {
        try {
            return objectMapper.readValue(json, ProductDocument.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize product", e);
        }
    }
}
