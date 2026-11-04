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
      | no format version                            |
      | format version 2                             |

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
      | a flight number without a letter (221)      |
      | format version 2                            |

  @integration
  Scenario: A duplicate message has one effect
    Given a ULD message has been processed
    When the same message, with the same identification, arrives again
    Then nothing changes
    And the duplicate is logged with its identification

  @unit
  Scenario Outline: A ULD message with an invalid value is invalid
    When a ULD message is read with <value>
    Then the message is invalid because "<reason>"

    Examples:
      | value                                          | reason                                         |
      | ULD code AK16689R7                             | ULD code 'AK16689R7' is invalid                |
      | ULD code ake16689R7                            | ULD code 'ake16689R7' is invalid               |
      | weight 0 kg                                    | The weight of a ULD must be greater than 0     |
      | weight -50 kg                                  | The weight of a ULD must be greater than 0     |
      | weight unit T                                  | Weight unit 'T' is invalid: expected KG or LB  |
      | weight unit kg                                 | Weight unit 'kg' is invalid: expected KG or LB |
      | destination QQQ, which is not in the IATA list | Airport 'QQQ' is not in the IATA list          |
      | format version 0                               | A format version is 1 or higher                |

  @unit
  Scenario Outline: A ULD message with valid values is accepted
    When a ULD message is read with <value>
    Then the message is valid

    Examples:
      | value               |
      | ULD code AKE16689R7 |
      | ULD code PMC1234KL  |
      | weight 0.1 kg       |
      | weight unit LB      |

  @unit
  Scenario: An SSM with the same departure and arrival airport is invalid
    When an SSM is read for a flight from BRU to BRU
    Then the message is invalid because "the same airport (BRU)"

  # "A change to a non-existent flight through the API" tests the REST API of Flight Operations: it is in
  # flight-operations/.../features/flightoperations/29-invalid-duplicate-messages.feature.
