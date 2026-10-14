Feature: Only flight planners can change flights and only load planners can change the load
  As an operations manager I want only flight planners to be able to change flights and only load planners to change the load so that nobody changes anything outside their role

  # Story 33 is split over the services, like story 37: this file keeps the rows that Flight Operations
  # answers. The rows about the load belong to Load Planning.

  @integration
  Scenario Outline: Changes according to the role, also directly through the API
    Given I am logged in as a <role>
    When I <action> through the API
    Then that is <result>

    Examples:
      | role           | action        | result                     |
      | flight planner | enter a delay | accepted                   |
      | load planner   | enter a delay | refused with 403 Forbidden |

    # Waits for the endpoints to change and remove a delay and to register the ATD and the ATA
    @wip
    Examples:
      | role         | action           | result                     |
      | load planner | change a delay   | refused with 403 Forbidden |
      | load planner | remove a delay   | refused with 403 Forbidden |
      | load planner | register the ATD | refused with 403 Forbidden |
      | load planner | register the ATA | refused with 403 Forbidden |

  @integration
  Scenario: Without login the API responds with 401
    Given I am not logged in
    When I try to enter a delay through the API
    Then the API responds with 401 Unauthorized
    But not with a redirect to the login page

  # Flight Operations shows the details; the load is in the file of Load Planning
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
      | role           | user | action        |
      | flight planner | anna | enter a delay |

    # Waits for the endpoints to change and remove a delay and to register the ATD and the ATA
    @wip
    Examples:
      | role           | user | action           |
      | flight planner | anna | change a delay   |
      | flight planner | anna | remove a delay   |
      | flight planner | anna | register the ATD |
      | flight planner | anna | register the ATA |

  @integration
  Scenario: The planner comes from the login, not from the request
    Given I am logged in as the flight planner anna
    When I enter a delay through the API with the planner bram in the request
    Then it is recorded that anna entered the delay
