@wip
Feature: Finalize the load
  As a load planner I want to finalize the load so that the aircraft may depart

  @unit
  Scenario: Finalizing when all hard checks pass
    Given it is within the loading period
    And all hard checks pass
    When I click Finalize Load
    Then the load is finalized
    And I can no longer change load positions
    And it is recorded that I finalized the load

  @unit @ui
  Scenario: A hard check fails when finalizing
    Given it is within the loading period
    And the lateral balance fails
    When I click Finalize Load
    Then the load is not finalized
    And I see the same report as with Check Load

  @integration
  Scenario: Finalizing checks again
    Given Check Load showed that all hard checks pass
    And another load planner then moved a ULD, so the lateral balance fails
    When I click Finalize Load
    Then the load is not finalized
    And I see the report with the failed lateral balance

  @unit
  Scenario: A flight without cargo can be finalized
    Given it is within the loading period
    And no cargo is booked on HJ221
    When I click Finalize Load
    Then the load is finalized

  @unit
  Scenario: A delay does not make the load changeable again
    Given the load has been finalized
    When a departure delay of 30 minutes is entered
    Then the load remains finalized
    And I can no longer change load positions

  @unit
  Scenario Outline: Not outside the loading period
    Given all hard checks pass
    But the loading period <state>
    When I click Finalize Load
    Then the load is not finalized

    Examples:
      | state               |
      | has not started yet |
      | is over             |

  @e2e
  Scenario: Finalizing twice sends one CPM
    Given the load has been finalized
    When I click Finalize Load again
    Then that is refused with an explanation
    And exactly one CPM has been sent
