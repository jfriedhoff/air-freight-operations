Feature: Invalid and duplicate incoming messages are set aside and logged
  As an operations manager I want invalid and duplicate incoming messages to be set aside and logged so that I can find out what happened

  # The part of story 29 about the messages that Load Planning reads from Messaging; the other scenarios are in
  # messaging/.../features/messaging/29-invalid-duplicate-messages.feature.

  @integration
  Scenario Outline: A message from Messaging that cannot be read is set aside
    Given flight HJ221 of 9 January 2026 from BRU exists
    When a ULD message for that flight arrives with <problem>
    Then the booking is not applied
    And the message is set aside and logged with its identification and the reason

    Examples:
      | problem     |
      | no ULD code |
      | no version  |
      | version 2   |
