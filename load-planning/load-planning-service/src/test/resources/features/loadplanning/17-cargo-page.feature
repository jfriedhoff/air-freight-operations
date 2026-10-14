Feature: See all booked ULDs of a flight on the cargo page
  As a load planner I want to see all booked ULDs of a flight on the cargo page so that I can plan the loading

  @ui
  Scenario: All booked ULDs, with and without a load position
    Given AKE16689R7 on CL and AKE20417HJ without a load position are booked on HJ221
    When I open the cargo page of HJ221
    Then I see the flight data of HJ221
    And I see both ULDs
    And for each ULD the code, the weight with unit, the destination as IATA code and the load position if it has one

  @ui
  Scenario: The selection list contains exactly the load positions of the floor plan
    Given I am logged in as a load planner
    When I choose a load position for AKE20417HJ
    Then the selection list contains the 34 load positions of the main deck: A1, A2, B1, left and right in rows C, D, E, F, G, H, J, K, L, M, P, R, S, T and U, and X

  @ui
  Scenario: A finalized load can be recognized
    Given the load of HJ221 has been finalized
    When I open the cargo page of HJ221
    Then I see that the load has been finalized
    And I can no longer change load positions or use Finalize Load
