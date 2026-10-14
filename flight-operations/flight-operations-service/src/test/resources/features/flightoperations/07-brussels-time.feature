Feature: See and enter times in Brussels time
  As an operations employee in Brussels I want to see and enter times in Brussels time so that I don't have to convert them myself

  # Story 7 is split over the services, like story 37: this file keeps the screens of Flight Operations.
  # The flights to load and the cargo page are in Load Planning.

  @ui
  Scenario Outline: Times are shown in Brussels time
    Given a flight with an STD on <date> at <utc> UTC
    When I view the flight
    Then I see <brussels> as the STD

    Examples:
      | date           | utc   | brussels |
      | 9 January 2026 | 06:30 | 07:30    |
      | 9 July 2026    | 06:30 | 08:30    |

  @ui
  Scenario Outline: Every screen states which time it shows
    When I open <screen>
    Then the screen states that the times are in Brussels time

    Examples:
      | screen                  |
      | the flight overview     |
      | the details of a flight |
      | the Gantt chart         |

  @ui
  Scenario Outline: Entered times are stored in UTC
    Given it is <date>
    When I enter <brussels> Brussels time as the ATD
    Then the ATD is stored as <utc> UTC

    Examples:
      | date           | brussels | utc   |
      | 9 January 2026 | 07:50    | 06:50 |
      | 9 July 2026    | 08:50    | 06:50 |

  @ui
  Scenario: A local time that does not exist is refused
    Given it is 29 March 2026, the day daylight saving time starts
    When I enter 02:30 Brussels time as the ATD
    Then the input is refused with the explanation that this time does not exist in Brussels

  @ui
  Scenario: For an ambiguous local time the planner chooses
    Given it is 25 October 2026, the day daylight saving time ends
    When I enter 02:30 Brussels time as the ATD
    Then the screen asks whether 02:30 summer time (00:30 UTC) or winter time (01:30 UTC) is meant
    And the ATD is only registered after I have chosen
