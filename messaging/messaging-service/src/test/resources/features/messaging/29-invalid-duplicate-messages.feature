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
      | a weight without a unit                      |
      | weight 0 kg                                  |
      | weight -50 kg                                |
      | day 32                                       |
      | JSON that cannot be read                     |

    # Messaging does not check these yet: the format of the ULD code, the weight unit (Load Planning refuses
    # both later, on its own dead letter queue) and the destination against the IATA list (nobody checks that).
    @wip
    Examples:
      | error                                        |
      | ULD code AK16689R7                           |
      | weight 1.25 t                                |
      | a destination that is not in the IATA list   |

  @integration
  Scenario Outline: An invalid SSM is set aside
    When an SSM arrives with <error>
    Then no flights are created
    And it is set aside and logged with its identification and the reason

    Examples:
      | error                                       |
      | an end date before the start date           |
      | no weekdays                                 |
      | departure time 25:00                        |
      | no aircraft registration                    |

    # Nobody checks these yet, not even Flight Operations: airports against the IATA list, and a departure
    # airport that is also the arrival airport.
    @wip
    Examples:
      | error                                       |
      | an airport that is not in the IATA list     |
      | the same departure and arrival airport      |

  @integration
  Scenario: A duplicate message has one effect
    Given a ULD message has been processed
    When the same message, with the same identification, arrives again
    Then nothing changes
    And the duplicate is logged with its identification

  # "A change to a non-existent flight through the API" tests the REST API of Flight Operations: it is in
  # flight-operations/.../features/flightoperations/29-invalid-duplicate-messages.feature.
