Feature: Invalid and duplicate incoming messages are set aside and logged
  As an operations manager I want invalid and duplicate incoming messages to be set aside and logged so that I can find out what happened

  @integration
  Scenario Outline: An invalid ULD message is set aside
    When a ULD message arrives with <error>
    Then it is not applied
    And it is set aside and logged with its identification and the reason

    Examples:
      | error                                        |
      | no ULD code                                  |
      | ULD code AK16689R7                           |
      | a weight without a unit                      |
      | weight 0 kg                                  |
      | weight -50 kg                                |
      | weight 1.25 t                                |
      | a destination that is not in the IATA list   |
      | day 32                                       |
      | JSON that cannot be read                     |

  @integration
  Scenario Outline: An invalid SSM is set aside
    When an SSM arrives with <error>
    Then no flights are created
    And it is set aside and logged with its identification and the reason

    Examples:
      | error                                       |
      | an end date before the start date           |
      | no weekdays                                 |
      | an airport that is not in the IATA list     |
      | the same departure and arrival airport      |
      | departure time 25:00                        |
      | no aircraft registration                    |

  @integration
  Scenario: A duplicate message has one effect
    Given a ULD message has been processed
    When the same message, with the same identification, arrives again
    Then nothing changes
    And the duplicate is logged with its identification

  # "A change to a non-existent flight through the API" tests the REST API of Flight Operations: it is in
  # flight-operations/.../features/flightoperations/29-invalid-duplicate-messages.feature.
