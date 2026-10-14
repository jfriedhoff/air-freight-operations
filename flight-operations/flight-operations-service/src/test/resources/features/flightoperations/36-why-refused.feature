Feature: See why an action was refused
  As a planner I want to see why every refused action was refused so that I know what to do

  # Story 36 is split over the services, like story 37: this file keeps the rows that Flight Operations
  # answers. The rows about load positions belong to Load Planning.

  @ui
  Scenario: The screen shows the reason for a refusal
    Given I am logged in as a flight planner
    And flight HJ221 has an ETD of 06:45 UTC and the finalized load has been received
    When I register 06:51 UTC as the ATD
    Then the flight remains planned
    And I see that I have to enter a delay first

  # No step definitions yet
  @integration @wip
  Scenario Outline: The API responds to an error according to REST
    Given I am logged in as a <role>
    When I <action> through the API
    Then the API responds with <status>
    And the response contains a Problem Details body with an error code and an explanation

    Examples:
      | role           | action                                                         | status          |
      | flight planner | enter a delay of 0 minutes                                     | 400 Bad Request |
      | flight planner | enter a delay with a delay reason that is not in the IATA list | 400 Bad Request |
      | flight planner | enter a delay for a flight that does not exist                 | 404 Not Found   |
      | flight planner | enter a departure delay for a departed flight                  | 409 Conflict    |
      | flight planner | register an ATD before the finalized load has been received    | 409 Conflict    |
      | flight planner | register an ATD more than 5 minutes after the ETD              | 409 Conflict    |

  # No step definitions yet
  @integration @wip
  Scenario: An unreachable database leaks no internal details
    Given the database of Flight Operations is unreachable
    When I request the details of HJ221
    Then the API responds with 503 Service Unavailable, without a stack trace
    And the error is logged at error level
