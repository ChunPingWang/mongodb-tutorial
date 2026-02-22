Feature: MongoTemplate Operations
  As a developer using Spring Data MongoDB
  I want to perform partial updates via MongoTemplate
  So that I can efficiently modify specific fields without full document replacement

  Scenario: Deposit money using $inc operator
    Given a bank account "ACC-TPL-001" for "Alice" with initial balance 2000.00
    When I deposit 500.00 via MongoTemplate
    Then the account balance becomes 2500.00
    And the account status remains "ACTIVE"

  Scenario: Add a tag to a product using $push operator
    Given a product with SKU "SKU-TPL-001" named "Laptop" with tags "computer"
    When I add the tag "portable" via MongoTemplate
    Then the product tags contain "computer" and "portable"

  Scenario: Upsert a product that does not exist
    Given no product exists with SKU "SKU-TPL-NEW"
    When I upsert a product with SKU "SKU-TPL-NEW" named "New Gadget" at price 199.99
    Then a product with SKU "SKU-TPL-NEW" exists in the database
    And its name is "New Gadget"
