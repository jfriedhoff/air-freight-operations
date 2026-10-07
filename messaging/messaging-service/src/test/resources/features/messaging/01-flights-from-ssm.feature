Feature: Flights are created automatically from the flight schedule (SSM)
  As a flight planner I want flights from the flight schedule (SSM) to be created automatically so that I don't have to enter the schedule by hand

  @unit @e2e
  Scenario: An SSM creates one flight per matching weekday, start and end date included
    Given an SSM for HJ221 from BRU to ATH from 5 up to and including 15 January 2026 on Monday and Thursday, departure 06:30 and arrival 09:45 UTC, registration SXBAA
    When the SSM is processed
    Then flights HJ221 from BRU exist on 5, 8, 12 and 15 January 2026
    And each flight has an STD of 06:30 and an STA of 09:45 on its date
    And each flight has registration SXBAA

  @unit
  Scenario: An arrival time before the departure time falls on the next day
    Given an SSM for 3V11 on Monday 5 January 2026 with departure 22:00 and arrival 01:30 UTC
    When the SSM is processed
    Then the flight of 5 January has an STA on 6 January 2026 at 01:30

  @unit
  Scenario: An arrival time equal to the departure time falls on the next day
    Given an SSM for 3V11 on Monday 5 January 2026 with departure and arrival at 22:00 UTC
    When the SSM is processed
    Then the flight of 5 January has an STA on 6 January 2026 at 22:00

  @unit
  Scenario: A period without a matching weekday creates no flights
    Given an SSM for HJ221 from Tuesday 6 up to and including Wednesday 7 January 2026 on Monday
    When the SSM is processed
    Then no flights are created
