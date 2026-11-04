Feature: Invalid and duplicate incoming messages are set aside and logged
  As an operations manager I want invalid and duplicate incoming messages to be set aside and logged so that I can find out what happened

  # The part of story 29 that is about the API of Flight Operations and the messages it reads from Messaging; the
  # other scenarios are in messaging/.../features/messaging/29-invalid-duplicate-messages.feature.

  @integration
  Scenario: A change to a non-existent flight through the API
    Given I am logged in as a flight planner
    When I enter a delay through the API for HJ999 of 9 January 2026 from BRU, a flight that does not exist
    Then the API responds with 404 Not Found
    And the attempt is logged with a warning

  @integration
  Scenario Outline: A message from Messaging that cannot be read is set aside and logged
    When a scheduled flight arrives from Messaging with <problem>
    Then that is logged at level warn, with the identification of the message or the flight

    Examples:
      | problem          |
      | no flight number |
      | no version       |
      | version 2        |
