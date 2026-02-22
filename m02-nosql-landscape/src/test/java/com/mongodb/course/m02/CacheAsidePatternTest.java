package com.mongodb.course.m02;

import com.mongodb.course.m02.cache.CacheAsideService;
import com.mongodb.course.m02.mongo.ProductDocument;
import com.mongodb.course.m02.mongo.ProductMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M02-LAB-02: Cache-Aside pattern — Redis as cache layer in front of MongoDB.
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
class CacheAsidePatternTest {

    @Autowired
    private CacheAsideService cacheAsideService;

    @Autowired
    private ProductMongoRepository mongoRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanUp() {
        mongoRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("Cache miss: fetch from MongoDB and populate Redis")
    void cacheMissFetchesFromMongoAndCaches() {
        // Given: product exists only in MongoDB
        var product = mongoRepository.save(
                new ProductDocument("Laptop Pro", "electronics", new BigDecimal("35000")));
        assertThat(cacheAsideService.isCached(product.getId())).isFalse();

        // When: first read through cache-aside
        ProductDocument result = cacheAsideService.findById(product.getId());

        // Then: product is returned and now cached
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Laptop Pro");
        assertThat(cacheAsideService.isCached(product.getId())).isTrue();
    }

    @Test
    @DisplayName("Cache hit: return from Redis without hitting MongoDB")
    void cacheHitReturnsFromRedis() {
        // Given: product exists in MongoDB and is cached (via first read)
        var product = mongoRepository.save(
                new ProductDocument("Laptop Pro", "electronics", new BigDecimal("35000")));
        cacheAsideService.findById(product.getId()); // warm cache
        assertThat(cacheAsideService.isCached(product.getId())).isTrue();

        // When: delete from MongoDB (simulating MongoDB unavailability)
        mongoRepository.deleteById(product.getId());

        // Then: still returns from cache
        ProductDocument cached = cacheAsideService.findById(product.getId());
        assertThat(cached).isNotNull();
        assertThat(cached.getName()).isEqualTo("Laptop Pro");
    }

    @Test
    @DisplayName("Write invalidation: update MongoDB and invalidate Redis cache")
    void writeInvalidatesCacheOnUpdate() {
        // Given: product is cached
        var product = mongoRepository.save(
                new ProductDocument("Laptop Pro", "electronics", new BigDecimal("35000")));
        cacheAsideService.findById(product.getId()); // warm cache
        assertThat(cacheAsideService.isCached(product.getId())).isTrue();

        // When: update through cache-aside service
        product.setPrice(new BigDecimal("32000"));
        cacheAsideService.save(product);

        // Then: cache is invalidated
        assertThat(cacheAsideService.isCached(product.getId())).isFalse();

        // And: next read fetches updated data from MongoDB and re-caches
        ProductDocument updated = cacheAsideService.findById(product.getId());
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("32000"));
        assertThat(cacheAsideService.isCached(product.getId())).isTrue();
    }
}
