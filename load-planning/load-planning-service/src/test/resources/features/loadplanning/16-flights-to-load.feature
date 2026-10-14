Feature: See the flights that still have to be loaded
  As a load planner I want to see the flights that still have to be loaded so that I know where to start

  @ui
  Scenario: Today's flights by default, without departed or landed flights
    Given it is 9 January 2026
    And HJ221 has departed, HJ225 has landed and 3V11 is still planned
    When I open the flights to load
    Then I see 3V11 with flight number, scheduled departure date, times and airports
    But I do not see HJ221 and HJ225

  @ui
  Scenario: Turning the filter off
    Given I am viewing the flights to load of 9 January
    When I turn off the filter on departed flights
    Then I also see HJ221 and HJ225

  @ui
  Scenario: Choosing another day
    Given I am viewing the flights to load of 9 January
    When I choose 10 January
    Then I see the flights to load of 10 January

  @ui
  Scenario: Clicking through to the cargo page
    Given I am viewing the flights to load of 9 January
    When I click 3V11
    Then I see the cargo page of 3V11
