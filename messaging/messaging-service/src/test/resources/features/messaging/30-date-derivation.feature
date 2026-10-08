Feature: Dates from ULD messages are derived correctly
  As an operations manager I want dates from ULD messages to be derived correctly so that a booking always applies to the right flight

  @unit
  Scenario Outline: Deriving a date and time from a day of the month
    Given the day of the month <day> and the time <time>
    When they are read on <read>
    Then they concern the flight of <flight> and the moment <moment>

    Examples:
      | day | time | read                  | flight         | moment                |
      | 09  | 1200 | 9 January 2026 08:00  | 9 January 2026 | 9 January 2026 12:00  |
      | 09  | 2355 | 10 January 2026 00:02 | 9 January 2026 | 9 January 2026 23:55  |
      | 09  | 0015 | 8 January 2026 23:58  | 9 January 2026 | 9 January 2026 00:15  |
      | 09  | 0015 | 9 January 2026 23:58  | 9 January 2026 | 10 January 2026 00:15 |

  @unit
  Scenario: Distance in calendar days
    Given it is 24 January 2026 18:00 UTC
    When the day of the month 09 is derived
    Then the date is 9 January 2026, because it lies 15 days back and 9 February lies 16 days ahead

  @unit
  Scenario: At equal distance the future date
    Given it is 23 February 2026 00:00 UTC
    When the day of the month 09 is derived
    Then the date is 9 March 2026

  @unit
  Scenario: A booking two weeks ahead at equal distance
    Given it is 15 February 2026
    When a ULD message with day 01 is processed
    Then it concerns the flight of 1 March 2026

  @unit
  Scenario Outline: A day that does not exist makes the message invalid
    When the day of the month <day> is derived
    Then the message is invalid

    Examples:
      | day |
      | 32  |
      | 00  |

  @unit @to-confirm
  Scenario: A day that does not exist in the
    Given it is 20 April 2026
    When the day of the month 31 is derived
    Then the message is invalid, because 31

  @unit @to-confirm
  Scenario: A time exactly twelve hours away
    Given it is 9 January 2026 12:00 UTC
    When the time 0000 is derived
    Then the moment is 10 January 2026 00:00 UTC

  @integration
  Scenario: A ULD message that arrives again later is not derived again
    Given a ULD message for HJ221, day 09, was processed on 2 January 2026 and booked the ULD on the flight of 9 January
    When the same message arrives again on 25 January 2026
    Then it is recognized as the same message
    And the ULD is not booked on the flight of 9 February
