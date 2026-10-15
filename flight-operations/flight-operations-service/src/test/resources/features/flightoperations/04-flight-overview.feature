Feature: See the flights of today or of a chosen day in an overview
  As an operations employee I want to see the flights of today or of a chosen day in an overview so that I quickly see which flights there are

  # The overview asks the API for the flights of a day, so these scenarios run through that API (@integration).
  # What the page makes of the answer (the table, the text that names the time zone and the day) is checked by
  # hand (@ui).

  @integration @ui
  Scenario: Today's flights by default
    Given it is 9 January 2026 10:00 UTC and there are flights on 8 and 9 January
    When I open the flight overview
    Then I only see the flights of 9 January
    And for each flight the flight number, the scheduled departure date and the departure and arrival airport

  @integration @ui
  Scenario: Choosing another day
    Given I am viewing the overview of 9 January
    When I choose 8 January
    Then I see the flights of 8 January

  @integration @ui
  Scenario: A day without flights
    When I choose a day on which there are no flights
    Then I see that there are no flights that day

  @ui
  Scenario: Clicking through to the details
    Given I am viewing the overview of 9 January with flight HJ221
    When I click flight HJ221
    Then I see the details of that flight

  # Question 9 of the analysis, answered on 15/10/2026: a day starts at 00:00 UTC
  @integration @ui
  Scenario: The day of a flight is its scheduled departure date in UTC
    Given flight 3V11 has STD 9 January 2026 23:30 UTC, in Brussels 10 January 00:30
    When I open the overview of 9 January
    Then I see 3V11 with scheduled departure date 9 January 2026
    And the screen states that the day is the scheduled departure date in UTC
