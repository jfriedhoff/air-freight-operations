Feature: See who changed the load
  As a planner I want to see who last changed a flight or its load so that I know whom to ask about it

  @integration
  Scenario: The cargo shows who put a ULD on its load position
    Given I am logged in as the load planner Lotte
    When I assign a load position through the API
    Then the cargo shows that Lotte put the ULD on its load position

  @integration
  Scenario: The cargo shows who finalized the load
    Given I am logged in as the load planner Lotte
    When I finalize the load through the API
    Then the cargo shows that Lotte finalized the load

  @integration
  Scenario: A load planner who is new in Keycloak is shown with their name
    Given I am logged in as Jan, a load planner who is new in Keycloak
    When I assign a load position through the API
    Then the cargo shows that Jan put the ULD on its load position

  @ui
  Scenario: The cargo page shows who changed the load
    Given Lotte put a ULD of HJ221 on A1 and finalized the load
    When I open the cargo of HJ221
    Then I see Lotte next to the ULD on A1
    And I see that Lotte finalized the load
