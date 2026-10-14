Feature: Only be offered the allowed actions
  As a planner I want to be offered only the actions I may perform according to my role and the state of the flight so that I don't click buttons that get refused anyway

  # Story 34 is split over the services, like story 37: this file keeps the cargo page.
  # The details page of a flight is in Flight Operations.

  @ui
  Scenario: A flight planner sees no actions for the load
    Given I am logged in as a flight planner
    When I open the cargo page of a flight
    Then I see the ULDs and their load positions
    But I see no selection list for load positions and no Check Load and Finalize Load buttons

  @ui
  Scenario: Actions follow the loading period
    Given I am logged in as a load planner
    And the loading period of HJ221 has not started yet
    When I open the cargo page of HJ221
    Then I see the booked ULDs
    But I cannot choose load positions, use Check Load or finalize the load yet
