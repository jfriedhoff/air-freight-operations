Feature: See why an action was refused
  As a planner I want to see why every refused action was refused so that I know what to do

  # Story 36 is split over the services, like story 37: this file keeps the rows that Load Planning
  # answers. The rows about delays and the ATD, and the screen scenario, belong to Flight Operations.

  @integration
  Scenario Outline: The API responds to an error according to REST
    Given I am logged in as a <role>
    When I <action> through the API
    Then the API responds with <status>
    And the response contains a Problem Details body with an error code and an explanation

    Examples:
      | role         | action                                                           | status          |
      | load planner | place a ULD on position Z9                                       | 400 Bad Request |
      | load planner | assign a load position outside the loading period                | 409 Conflict    |
      | load planner | change a load position that someone else changed in the meantime | 409 Conflict    |
