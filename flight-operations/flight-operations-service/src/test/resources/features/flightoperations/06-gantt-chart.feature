Feature: See all flights of a chosen day on a Gantt chart
  As an operations employee I want to see all flights of a chosen day on a Gantt chart so that I can take in the day at a glance

  @ui
  Scenario: Flights as bars on a timeline
    Given there are three flights on 9 January 2026
    When I open the Gantt chart of 9 January
    Then I see three bars, each from the STD to the STA in Brussels time

  @ui
  Scenario: Choosing another day
    Given I am viewing the Gantt chart of 9 January
    When I choose 10 January
    Then I see the flights of 10 January

  @ui
  Scenario: A flight across midnight runs to the edge of the timeline
    Given flight 3V11 of 9 January 2026 with STD 21:00 UTC and STA 10 January 00:30 UTC
    When I open the Gantt chart of 9 January
    Then the bar of 3V11 starts at 22:00 Brussels time and runs to the edge of the timeline
    And the bar shows that the flight lands on 10 January at 01:30 Brussels time

  @ui
  Scenario: The day of a flight is its scheduled departure date in UTC
    Given flight 3V13 has STD 9 January 2026 23:30 UTC, in Brussels 10 January 00:30
    When I open the Gantt chart of 9 January
    Then I see the bar of 3V13 at the edge of the timeline
    And the bar shows that the flight departs on 10 January at 00:30 Brussels time
    And the screen states that the day is the scheduled departure date in UTC
