Feature: Register the actual time of arrival (ATA)
  As a flight planner I want to register the actual time of arrival (ATA) so that everyone knows the flight has landed

  @unit
  Scenario: Not before departure
    Given flight HJ221 is planned
    When I register an ATA
    Then the registration is refused with an explanation

  @unit
  Scenario Outline: Strictly after the ATD
    Given flight HJ221 has departed with ATD 06:50 UTC and has ETA 10:00 UTC
    And it is now 11:00 UTC
    When I register <ata> as the ATA
    Then the registration is <result>

    Examples:
      | ata   | result   |
      | 06:40 | refused  |
      | 06:50 | refused  |
      | 06:51 | accepted |

  @unit
  Scenario Outline: At most 5 minutes later than the ETA
    Given flight HJ221 has departed with ETA 10:00 UTC
    And it is now 11:00 UTC
    When I register <ata> as the ATA
    Then the registration is <result>

    Examples:
      | ata   | result                       |
      | 09:50 | accepted                     |
      | 10:05 | accepted                     |
      | 10:06 | refused: enter a delay first |

  @unit
  Scenario Outline: At most the current time
    Given flight HJ221 has departed with ETA 10:00 UTC
    And it is now 09:58 UTC
    When I register <ata> as the ATA
    Then the registration is <result>

    Examples:
      | ata   | result                      |
      | 09:58 | accepted                    |
      | 09:59 | refused: lies in the future |

  @unit
  Scenario: After registration the flight has landed
    Given the ATA has been registered
    Then the flight has landed
    And the ATA can no longer be changed
    And it is recorded who registered the ATA
