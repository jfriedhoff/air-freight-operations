Feature: A flight that already exists is not created again
  As a flight planner I want a flight that already exists not to be created again so that the flight schedule never contains duplicate flights

  @integration @e2e
  Scenario: An existing flight is skipped
    Given the flight HJ221 of 5 January 2026 from BRU already exists
    When an SSM is processed that announces HJ221 from BRU on 5 and 8 January
    Then only the flight of 8 January is created
    And the duplicate of 5 January is logged with flight number, date and departure airport

  @integration @e2e
  Scenario: An SSM with different data does not overwrite an existing flight
    Given the flight HJ221 of 5 January 2026 from BRU exists with STD 06:30 UTC and registration SXBAA
    When an SSM is processed that announces HJ221 from BRU on 5 January with STD 07:00 UTC and registration SXBAB
    Then the flight still has STD 06:30 UTC and registration SXBAA
    And the duplicate is logged

  @integration @e2e
  Scenario: The same flight number on the same day from another airport is a different flight
    Given the flight HJ221 of 5 January 2026 from BRU already exists
    When an SSM is processed that announces HJ221 from ATH on 5 January
    Then the flight HJ221 of 5 January from ATH is created
    And the flight HJ221 of 5 January from BRU still exists

  @e2e
  Scenario: Receiving the same SSM twice
    Given an SSM has already been processed
    When the same SSM arrives again
    Then no extra flights are created
    And no extra FLMs are sent
    And the duplicate is logged

  @integration
  Scenario: Two simultaneous announcements of the same flight create one flight
    Given two different SSMs both announce HJ221 from BRU on 5 January 2026
    When Flight Operations processes both announcements at the same time
    Then the flight HJ221 of 5 January from BRU exists exactly once
    And it is announced exactly once
