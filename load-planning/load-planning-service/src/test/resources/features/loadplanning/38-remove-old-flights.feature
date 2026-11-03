Feature: Flights are removed 7 days after their scheduled departure date
  As an operations manager I want flights to be removed 7 days after their scheduled departure date so that the system keeps no old data

  # Flight Operations removes the flight and Messaging passes that on: this file is the Load Planning part of
  # story 38.

  Background:
    Given flight HJ221 of 9 January 2026 has a finalized load with AKE16689R7 on CL and AKE20417HJ booked afterwards without a load position
    And flight HJ221 of 16 January 2026 has AKE16689R7 booked

  @integration
  Scenario: The cargo of a removed flight is removed with all its ULDs
    When Messaging reports that HJ221 of 9 January 2026 has been removed
    Then Load Planning no longer has a cargo for HJ221 of 9 January
    And no ULD of HJ221 of 9 January is left, also not the one without a load position
    And the cargo of HJ221 of 16 January is unchanged

  @integration
  Scenario: The same removal twice has one effect
    Given Load Planning has already processed the removal of HJ221 of 9 January
    When the same event arrives again
    Then it is logged that HJ221 of 9 January had no cargo to remove
    And the cargo of HJ221 of 16 January is unchanged

  @integration
  Scenario: The removal of a flight without a cargo changes nothing
    When Messaging reports that HJ999 of 9 January 2026 has been removed
    Then it is logged that HJ999 of 9 January had no cargo to remove
    And the cargo of HJ221 of 9 January is unchanged
    And the cargo of HJ221 of 16 January is unchanged
