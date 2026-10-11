Feature: Flights are removed 7 days after their scheduled departure date
  As an operations manager I want flights to be removed 7 days after their scheduled departure date so that the system keeps no old data

  @integration
  Scenario Outline: Removing seven days after the scheduled departure date
    Given flight HJ221 of 9 January 2026 has <state> and has a delay
    When it is 16 January 2026 00:00 UTC
    Then the flight and its delays have been removed
    And Flight Operations reports that the flight has been removed

    Examples:
      | state            |
      | not departed yet |
      | departed         |
      | landed           |

  @integration
  Scenario: The day before, the flight still exists
    Given flight HJ221 of 9 January 2026
    When it is 15 January 2026 23:59 UTC
    Then the flight still exists

  @e2e
  Scenario: Load Planning and Messaging remove their data too
    Given flight HJ221 of 9 January 2026 has a finalized load, a booked ULD without a load position and sent messages
    When Flight Operations removes the flight on 16 January 2026
    Then Load Planning removes the load and all bookings of HJ221, including those without a load position
    And Messaging removes the messages about HJ221

  @e2e
  Scenario: Removing sends no message to the partners
    When flight HJ221 is removed after the retention period
    Then no message is sent to the partners
