Feature: Cache-Aside Pattern
  As a developer building high-performance applications
  I want to use Redis as a cache layer in front of MongoDB
  So that frequently accessed data is served from memory

  Scenario: Cache miss falls through to MongoDB then caches
    Given a product "Laptop Pro" exists only in MongoDB
    When I read the product through cache-aside service
    Then the product is fetched from MongoDB
    And the product is now cached in Redis

  Scenario: Cache invalidation on write
    Given a product "Laptop Pro" is cached in Redis
    When I update the product price to 32000
    Then the Redis cache is invalidated
    And the next read returns the updated price 32000
