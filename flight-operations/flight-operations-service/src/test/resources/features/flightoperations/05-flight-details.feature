@wip
Feature: See the details of a flight
  As an operations employee I want to see the details of a flight so that I know the current state of the flight

  @ui
  Scenario: No estimated times without a delay
    Given flight HJ221 from BRU to ATH without delays
    When I open the details
    Then I see BRU and ATH with their names
    And the scheduled departure and arrival time with date
    But no estimated times

  @ui
  Scenario: With a departure delay
    Given flight HJ221 with a departure delay of 25 minutes
    When I open the details
    Then I see the ETD and ETA with date
    And the delay with its minutes, delay reason and description

  @ui
  Scenario: With only an arrival delay only the ETA is shown
    Given flight HJ221 with an arrival delay of 15 minutes and no departure delay
    When I open the details
    Then I see the ETA with date
    But no ETD

  @ui
  Scenario: Departure and arrival delays can be told apart
    Given flight HJ221 with a departure delay and an arrival delay
    When I open the details
    Then I see for each delay whether it applies to the departure or to the arrival

  @ui
  Scenario: After the actual times are registered
    Given flight HJ221 with a registered ATD and ATA
    When I open the details
    Then I see the ATD and ATA with date

  @ui
  Scenario: Scheduled and estimated times cannot be edited directly
    Given I am logged in as a flight planner
    When I open the details of HJ221
    Then I cannot edit the STD, STA, ETD and ETA

  @integration
  Scenario: Nobody can change the scheduled or estimated times through the API either
    Given I am logged in as a flight planner
    When I try to change the STD or the ETD of HJ221 through the API
    Then that is refused
    And the STD and the ETD remain unchanged
