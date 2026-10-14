Feature: Log in through Keycloak
  As a user I want to log in through Keycloak so that the system knows whether I am a flight planner or a load planner

  # The tokens are imitated in the tests: what Keycloak puts in a token is its own work, what the API does with
  # the roles in it is ours.

  # Waits for the endpoint to assign a load position: a load planner is one who can change the load
  @integration @wip
  Scenario Outline: Logging in gives the right role
    Given there is a user with the role <role> in Keycloak
    When that user logs in
    Then the system treats them as a <role>

    Examples:
      | role           |
      | flight planner |
      | load planner   |

  @ui
  Scenario: Not logged in
    Given I am not logged in
    When I open a page
    Then I am sent to the login

  # The API answers 403 without a body; the page tells the user why
  @integration
  Scenario Outline: A user has exactly one of the two roles
    Given there is a user with <roles> in Keycloak
    When that user logs in
    Then they get no access and see why

    Examples:
      | roles                                             |
      | neither of the two roles                          |
      | both the flight planner and the load planner role |
