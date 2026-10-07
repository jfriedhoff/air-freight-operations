Feature: Change or remove a delay
  As a flight planner I want to change or remove a delay so that wrong or outdated delays are corrected

  Background:
    Given flight HJ221 with STD 06:30, STA 09:45 UTC and one departure delay of 25 minutes

  @unit
  Scenario: Changing the number of minutes
    When I change the delay to 40 minutes
    Then the ETD is 07:10
    And the ETA is 10:25
    And it is recorded that I last changed the delay

  @unit
  Scenario Outline: After a change all delays together still last at most 12 hours
    Given HJ221 also has an arrival delay of 680 minutes
    When I change the departure delay to <minutes> minutes
    Then that is <result>

    Examples:
      | minutes | result   |
      | 40      | accepted |
      | 41      | refused  |

  @unit
  Scenario: Changing the description
    When I change the description of the delay
    Then the delay has the new description
    And the ETD and the ETA remain unchanged

  # Not testable yet: the domain offers no way to change the reason (Delay.reason is final). Waiting for a decision.
  @unit @to-confirm
  Scenario: The delay reason cannot be changed
    When I try to change the delay reason of the delay
    Then that is refused

  @unit
  Scenario: Removing the last delay
    When I remove the delay
    Then the ETD equals the STD again
    And the ETA equals the STA again
    And it is recorded that I removed the delay

  @unit
  Scenario: Removing one of several delays
    Given HJ221 also has a departure delay of 10 minutes
    When I remove the delay of 25 minutes
    Then the ETD is 06:40
    And the ETA is 09:55

  @unit
  Scenario Outline: A departure delay is fixed after departure
    Given flight HJ221 has departed
    When I try to <action> the departure delay
    Then that is refused with an explanation

    Examples:
      | action |
      | change |
      | remove |

  @unit
  Scenario Outline: Changing or removing an arrival delay depends on the state
    Given HJ221 also has an arrival delay of 15 minutes
    And flight HJ221 has <state>
    When I <action> the arrival delay
    Then that is <result>

    Examples:
      | state    | action | result   |
      | departed | change | accepted |
      | departed | remove | accepted |
      | landed   | change | refused  |
      | landed   | remove | refused  |
