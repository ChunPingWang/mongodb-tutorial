package com.mongodb.course.m02.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisProductService {

    private final StringRedisTemplate redisTemplate;

    public RedisProductService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String findByKey(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public Boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }
}
