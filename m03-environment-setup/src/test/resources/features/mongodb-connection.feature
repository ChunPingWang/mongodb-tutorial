Feature: MongoDB Connection Verification
  As a developer
  I want to verify MongoDB container connectivity
  So that I can build integration tests on a working infrastructure

  Scenario: Successfully connect to MongoDB
    Given a MongoDB container is running
    When I execute a ping command
    Then the response status is ok

  Scenario: Write and read a document
    Given a MongoDB container is running
    When I insert a document with name "test-doc" into collection "bdd_tests"
    Then I can read the document with name "test-doc" from collection "bdd_tests"
