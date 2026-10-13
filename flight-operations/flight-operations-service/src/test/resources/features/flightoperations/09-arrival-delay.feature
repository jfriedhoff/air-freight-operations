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

  @integration
  Scenario: A flight planner enters an arrival delay through the API
    Given flight HJ221 of 9 January 2026 with STD 06:30 and STA 09:45 UTC
    And I am logged in as a flight planner
    When I enter an arrival delay of 15 minutes through the API
    Then the API answers 200
    And the API shows the ETD 06:30 and the ETA 10:00
    And only ArrivalEstimateChanged waits in the outbox

  @integration
  Scenario: An arrival delay on a departed flight is accepted through the API
    Given flight HJ221 of 9 January 2026 with STD 06:30 and STA 09:45 UTC
    And flight HJ221 has departed
    And I am logged in as a flight planner
    When I enter an arrival delay of 15 minutes through the API
    Then the API answers 200
    And the API shows the ETD 06:30 and the ETA 10:00

  @integration
  Scenario: An arrival delay on a landed flight is refused through the API
    Given flight HJ221 of 9 January 2026 with STD 06:30 and STA 09:45 UTC
    And flight HJ221 has landed
    And I am logged in as a flight planner
    When I enter an arrival delay of 15 minutes through the API
    Then the API answers 409
    And the explanation says "An arrival delay can no longer change once the flight has landed"
    And HJ221 has no delay
