Feature: Flights are removed 7 days after their scheduled departure date
  As an operations manager I want flights to be removed 7 days after their scheduled departure date so that the system keeps no old data

  # Flight Operations removes the flight and reports it: this file is the Messaging part of story 38.

  @integration
  Scenario: Messaging removes the messages about a removed flight
    Given Messaging has handled the announcement, a delay, a ULD booking and the finalized load of HJ221 of 9 January 2026
    And Messaging has handled the announcement, a delay, a ULD booking and the finalized load of HJ221 of 16 January 2026
    When Flight Operations reports that HJ221 of 9 January 2026 has been removed
    Then Messaging keeps no message about HJ221 of 9 January 2026 except the removal itself
    And the messages about HJ221 of 16 January 2026 are all still there
    And Load Planning is told that HJ221 of 9 January 2026 has been removed
