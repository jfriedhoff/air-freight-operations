Feature: Enter an arrival delay
  As a flight planner I want to enter an arrival delay so that the expected arrival time is correct

  @unit
  Scenario: Only the ETA shifts
    Given flight HJ221 with ETD 06:55 and ETA 10:10 UTC
    When I enter an arrival delay of 15 minutes
    Then the ETD remains 06:55
    And the ETA is 10:25
    And it is recorded that I entered the delay

  @unit
  Scenario: Possible after departure
    Given flight HJ221 has departed
    When I enter an arrival delay
    Then it is accepted

  @unit
  Scenario: No longer possible after landing
    Given flight HJ221 has landed
    When I enter an arrival delay
    Then it is refused with an explanation
