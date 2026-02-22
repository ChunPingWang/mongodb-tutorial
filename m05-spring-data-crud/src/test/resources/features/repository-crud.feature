Feature: MongoRepository CRUD Operations
  As a developer using Spring Data MongoDB
  I want to perform basic CRUD operations via MongoRepository
  So that I can manage bank account documents effectively

  Scenario: Create and retrieve a bank account
    Given a new bank account with number "ACC-BDD-001" for holder "Alice" with balance 5000.00
    When I save the account via repository
    Then I can retrieve the account by its id
    And the holder name is "Alice"
    And the balance is 5000.00

  Scenario: Update account balance via full replacement
    Given an existing bank account with number "ACC-BDD-002" for holder "Bob" with balance 3000.00
    When I update the balance to 4500.00 and save via repository
    Then the persisted balance is 4500.00
    And the holder name is still "Bob"

  Scenario: Delete a bank account
    Given an existing bank account with number "ACC-BDD-003" for holder "Charlie" with balance 1000.00
    When I delete the account by its id
    Then the account no longer exists in the database
