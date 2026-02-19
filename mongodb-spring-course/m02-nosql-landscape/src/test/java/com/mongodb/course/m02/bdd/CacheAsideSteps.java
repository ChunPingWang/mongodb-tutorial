package com.mongodb.course.m02.bdd;

import com.mongodb.course.m02.cache.CacheAsideService;
import com.mongodb.course.m02.mongo.ProductDocument;
import com.mongodb.course.m02.mongo.ProductMongoRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheAsideSteps {

    @Autowired
    private CacheAsideService cacheAsideService;

    @Autowired
    private ProductMongoRepository mongoRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ProductDocument savedProduct;
    private ProductDocument retrievedProduct;

    @Before
    public void cleanUp() {
        mongoRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Given("a product {string} exists only in MongoDB")
    public void aProductExistsOnlyInMongoDB(String name) {
        savedProduct = mongoRepository.save(
                new ProductDocument(name, "electronics", new BigDecimal("35000")));
        assertThat(cacheAsideService.isCached(savedProduct.getId())).isFalse();
    }

    @When("I read the product through cache-aside service")
    public void iReadTheProductThroughCacheAsideService() {
        retrievedProduct = cacheAsideService.findById(savedProduct.getId());
    }

    @Then("the product is fetched from MongoDB")
    public void theProductIsFetchedFromMongoDB() {
        assertThat(retrievedProduct).isNotNull();
        assertThat(retrievedProduct.getName()).isEqualTo(savedProduct.getName());
    }

    @And("the product is now cached in Redis")
    public void theProductIsNowCachedInRedis() {
        assertThat(cacheAsideService.isCached(savedProduct.getId())).isTrue();
    }

    @Given("a product {string} is cached in Redis")
    public void aProductIsCachedInRedis(String name) {
        savedProduct = mongoRepository.save(
                new ProductDocument(name, "electronics", new BigDecimal("35000")));
        cacheAsideService.findById(savedProduct.getId()); // warm cache
        assertThat(cacheAsideService.isCached(savedProduct.getId())).isTrue();
    }

    @When("I update the product price to {int}")
    public void iUpdateTheProductPriceTo(int newPrice) {
        savedProduct.setPrice(new BigDecimal(newPrice));
        cacheAsideService.save(savedProduct);
    }

    @Then("the Redis cache is invalidated")
    public void theRedisCacheIsInvalidated() {
        assertThat(cacheAsideService.isCached(savedProduct.getId())).isFalse();
    }

    @And("the next read returns the updated price {int}")
    public void theNextReadReturnsTheUpdatedPrice(int expectedPrice) {
        ProductDocument updated = cacheAsideService.findById(savedProduct.getId());
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal(expectedPrice));
    }
}
