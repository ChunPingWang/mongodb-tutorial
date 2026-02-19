Feature: NoSQL Types Comparison
  As a developer learning NoSQL databases
  I want to compare different NoSQL store types side by side
  So that I understand their query patterns and trade-offs

  Scenario: Store same product in three NoSQL types and compare query patterns
    Given a product "Laptop Pro" in category "electronics" priced at 35000
    When I query each store for the product
    Then MongoDB supports ad-hoc queries by any field
    And Redis returns data only by exact key
    And Cassandra returns data by partition key

  Scenario: Schema flexibility comparison across stores
    Given products with different schemas in MongoDB
    When I retrieve the products from MongoDB
    Then documents with different fields coexist in the same collection
