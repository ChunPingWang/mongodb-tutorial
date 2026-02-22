Feature: Java 23 Features with MongoDB
  As a developer using modern Java
  I want to use records and sealed interfaces with MongoDB
  So that I can leverage Java 23 features in my data layer

  Scenario: Store and retrieve record class
    Given a savings account record with name "High Yield" and rate "3.5"
    When I retrieve the record from MongoDB
    Then the record fields are correctly mapped

  Scenario: Sealed interface pattern matching
    Given financial products of different types
    When I describe each product using pattern matching
    Then the sealed interface switch expression handles all types
