Feature: Embedding vs Referencing
  As a developer designing MongoDB schemas
  I want to compare embedding and referencing patterns
  So that I can choose the right approach for each use case

  Scenario: Embedded pattern retrieves all data in a single query
    Given a customer "Alice" with embedded accounts and transactions
    When I query the embedded customer by name "Alice"
    Then all accounts and transactions are returned in a single document

  Scenario: Referenced pattern requires multiple queries
    Given a customer "Bob" with referenced accounts
    When I query the customer and then the accounts separately
    Then I need two separate queries to assemble the full data
