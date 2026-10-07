@wip
Feature: Simulate flight schedules by supplying JSON
  As a trainer I want to simulate flight schedules by supplying JSON so that I can give demos and training without the real source

  @e2e
  Scenario: One SSM per record
    When I supply two flight schedule records as JSON
    Then the simulator sends two SSMs over RabbitMQ
    And the flights are created according to the same rules as for a real SSM

  @integration
  Scenario: The sent SSM follows the fixed structure
    When I supply a valid flight schedule record
    Then the sent SSM conforms to the fixed structure of an SSM

  @integration
  Scenario: JSON that cannot be read is refused
    When I supply JSON that cannot be read
    Then the API responds with 400 Bad Request and the reason
    And no SSM is sent

  @integration
  Scenario: Not in production
    Given the application runs with the production profile
    When I supply flight schedule records
    Then the API responds with 404 Not Found
    And no SSM is sent
