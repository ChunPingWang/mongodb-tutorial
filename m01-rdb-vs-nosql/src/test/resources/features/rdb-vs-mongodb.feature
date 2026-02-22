Feature: RDB vs MongoDB Comparison
  As a developer familiar with relational databases
  I want to compare RDB and MongoDB approaches side by side
  So that I understand the trade-offs of each data model

  Scenario: Store and retrieve customer order data in both databases
    Given the same customer order data:
      | name     | email           | city   |
      | Alice    | alice@test.com  | Taipei |
    And the customer has an order with items:
      | product   | quantity | unitPrice |
      | Laptop    | 1        | 35000     |
      | Mouse     | 2        | 500       |
    When the data is stored in PostgreSQL
    And the data is stored in MongoDB
    Then PostgreSQL requires JOIN to retrieve customer with orders
    And MongoDB retrieves the complete document in a single read

  Scenario: Nested query comparison
    Given customers with orders exist in both databases
    When I query for customers who ordered "Laptop"
    Then PostgreSQL uses JOIN across 3 tables
    And MongoDB queries nested documents directly
