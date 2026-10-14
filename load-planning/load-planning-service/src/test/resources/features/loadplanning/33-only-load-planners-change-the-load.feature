Feature: Only flight planners can change flights and only load planners can change the load
  As an operations manager I want only flight planners to be able to change flights and only load planners to change the load so that nobody changes anything outside their role

  # Story 33 is split over the services, like story 37: this file keeps the rows that Load Planning answers.
  # The rows about delays, the ATD and the ATA belong to Flight Operations. Two scenarios of the story are
  # worded as entering a delay ("Without login the API responds with 401" and "The planner comes from the
  # login, not from the request"): for Load Planning they are JUnit tests, SecurityConfigTest and
  # FlightCargoControllerTest.

  @integration
  Scenario Outline: Changes according to the role, also directly through the API
    Given I am logged in as a <role>
    When I <action> through the API
    Then that is <result>

    Examples:
      | role           | action                 | result                     |
      | load planner   | assign a load position | accepted                   |
      | flight planner | assign a load position | refused with 403 Forbidden |

    # Waits for the endpoints to clear a load position, to check the load and to finalize it
    @wip
    Examples:
      | role           | action                | result                     |
      | flight planner | clear a load position | refused with 403 Forbidden |
      | flight planner | check the load        | refused with 403 Forbidden |
      | flight planner | finalize the load     | refused with 403 Forbidden |

  @integration
  Scenario Outline: Everyone can view
    Given I am logged in as a <role>
    When I request the details and the load of HJ221
    Then I get to see them

    Examples:
      | role           |
      | flight planner |
      | load planner   |

  @integration
  Scenario Outline: Every change records which planner made it
    Given I am logged in as the <role> <user>
    When I <action> through the API
    Then it is recorded that <user> did that

    Examples:
      | role         | user | action                 |
      | load planner | bram | assign a load position |

    # Waits for the endpoints to clear a load position and to finalize the load
    @wip
    Examples:
      | role         | user | action                |
      | load planner | bram | clear a load position |
      | load planner | bram | finalize the load     |
