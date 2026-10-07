Feature: Register the actual time of departure (ATD)
  As a flight planner I want to register the actual time of departure (ATD) so that everyone knows the flight has departed

  Background:
    Given flight HJ221 is planned with ETD 06:45 UTC

  @unit
  Scenario Outline: At most 5 minutes later than the ETD
    Given the finalized load has been received
    And it is now 08:00 UTC
    When I register <atd> as the ATD
    Then the registration is <result>

    Examples:
      | atd   | result                              |
      | 06:20 | accepted                            |
      | 06:50 | accepted                            |
      | 06:51 | refused: enter a delay first        |

  @unit
  Scenario Outline: At most the current time
    Given the finalized load has been received
    And it is now 06:47 UTC
    When I register <atd> as the ATD
    Then the registration is <result>

    Examples:
      | atd   | result                      |
      | 06:47 | accepted                    |
      | 06:48 | refused: lies in the future |

  @unit
  Scenario: Not without a finalized load
    Given the CPM has not been processed yet
    And it is now 08:00 UTC
    When I register 06:50 as the ATD
    Then the registration is refused with the explanation that the finalized load has not been received yet

  @unit
  Scenario: The same action succeeds once the CPM has been processed
    Given it is now 08:00 UTC
    And my registration was refused because the CPM had not been processed yet
    And the CPM has been processed in the meantime
    When I register 06:50 as the ATD again
    Then the registration is accepted
    And the flight has departed
    And it is recorded that I registered the ATD

  @e2e
  Scenario: Finalizing and departing right away never fails silently
    Given the load planner finalizes the load of HJ221
    When the flight planner registers the ATD immediately afterwards
    Then he gets either a confirmation or a refusal with the explanation that the finalized load has not been received yet
    And the flight has only departed if he got a confirmation
    And only then exactly one MVT-AD is sent

  @e2e
  Scenario: A late departure: first a delay, then the ATD
    Given the finalized load has been received
    And it is now 08:00 UTC
    And my ATD 06:51 was refused with the request to enter a delay first
    When I enter a departure delay of 10 minutes
    And I register 06:51 as the ATD again
    Then the registration is accepted
    And an MVT-ED, an MVT-EA and an MVT-AD are sent

  @unit
  Scenario: A registered ATD cannot be changed
    Given the ATD has been registered
    When I try to change the ATD
    Then that is refused

  @e2e
  Scenario: Registering the same ATD twice sends one MVT-AD
    Given the finalized load has been received
    And it is now 08:00 UTC
    When I register 06:50 as the ATD twice in quick succession
    Then the first registration is accepted and the second is refused
    And exactly one MVT-AD is sent
