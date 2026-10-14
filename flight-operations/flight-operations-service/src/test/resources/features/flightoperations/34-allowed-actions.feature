Feature: Only be offered the allowed actions
  As a planner I want to be offered only the actions I may perform according to my role and the state of the flight so that I don't click buttons that get refused anyway

  # Story 34 is split over the services, like story 37: this file keeps the details page of a flight.
  # The cargo page is in Load Planning.

  @ui
  Scenario: A load planner sees no actions for flights
    Given I am logged in as a load planner
    When I open the details of a flight
    Then I see the details
    But I see no buttons to change delays or actual times

  @ui
  Scenario: Actions follow the state of the flight
    Given I am logged in as a flight planner
    And flight HJ221 has departed
    When I open the details of HJ221
    Then I can no longer enter a departure delay or an ATD
    But I can still enter an arrival delay and the ATA
