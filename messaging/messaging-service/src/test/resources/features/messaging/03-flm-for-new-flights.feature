@wip
Feature: Partners receive an FLM for every newly created flight
  As a partner I want to receive an FLM for every newly created flight so that I know which flights are coming

  @integration @e2e
  Scenario: One FLM per new flight
    Given an SSM that announces two new flights
    When the SSM is processed
    Then two FLMs are sent
    And each FLM contains the flight number, the scheduled departure date, STD, STA and the departure and arrival airport
    And STD and STA are full UTC timestamps in the FLM

  @integration @e2e
  Scenario: No FLM for an existing flight
    Given an SSM that announces two flights, one of which already exists
    When the SSM is processed
    Then exactly one FLM is sent

  @integration @e2e
  Scenario: The FLM of a night flight contains the STA on the next day
    Given an SSM for 3V11 from BRU to ATH on Monday 5 January 2026 with departure 22:00 and arrival 01:30 UTC
    When the SSM is processed
    Then the FLM of 3V11 contains the scheduled departure date 5 January 2026, the STD 5 January 2026 22:00 UTC and the STA 6 January 2026 01:30 UTC
