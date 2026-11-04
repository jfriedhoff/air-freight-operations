Feature: Load Planning follows the latest state of a flight
  As a load planner I want Load Planning to always follow the latest state of a flight so that I work with the right loading period and state

  @e2e
  Scenario: An announced flight appears among the flights to load
    When the flight schedule announces HJ221 of 9 January 2026 from BRU
    Then HJ221 is among the flights to load of 9 January
    And cargo can be booked on HJ221

  @e2e
  Scenario: A delay in Flight Operations shifts the end of the loading period
    Given flight HJ221 with STD 9 January 2026 06:30 UTC and AKE16689R7 booked
    And at 06:00 UTC the flight planner enters a departure delay of 25 minutes
    When the load planner places AKE16689R7 on CL at 06:20 UTC
    Then that is accepted

  @e2e
  Scenario: After departure Load Planning refuses new bookings
    Given the load of HJ221 has been finalized
    When the flight planner registers the ATD of HJ221
    Then a new booking for HJ221 is set aside and logged
    And HJ221 is no longer among the flights to load by default

  @e2e
  Scenario: After landing the flight stays departed in Load Planning
    Given HJ221 has departed
    When the flight planner registers the ATA of HJ221
    Then HJ221 is still not in the default list of flights to load

  @integration
  Scenario: The same flight event twice has one effect
    Given Load Planning has already processed the event about the delay of HJ221
    When the same event arrives again
    Then nothing changes

  @integration
  Scenario: Flight events are applied in the order they were sent
    Given Flight Operations sends an ETD of 06:45 UTC and then an ETD of 07:00 UTC for HJ221
    When Load Planning processes both events
    Then the ETD in Load Planning is 07:00 UTC

  @integration
  Scenario: A delay entered within the loading period extends it, even when Load Planning processes it late
    Given HJ221 departs at 06:45 UTC, so its loading period ends at 06:25 UTC
    And at 06:20 UTC the flight planner enters a departure delay of 25 minutes
    When Load Planning only processes that event at 06:30 UTC
    Then the loading period of HJ221 now ends at 06:50 UTC
