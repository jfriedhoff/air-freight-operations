Feature: See who changed a flight
  As a planner I want to see who last changed a flight or its load so that I know whom to ask about it

  Background:
    Given flight HJ221 of 9 January 2026 is planned with STD 06:30 and STA 09:45 UTC

  @integration
  Scenario: The flight shows who entered a delay
    Given I am logged in as the flight planner Fien
    When I enter a departure delay of 25 minutes through the API
    Then the flight shows that Fien last changed the delay

  @integration
  Scenario: A flight planner who is new in Keycloak is shown with their name
    Given I am logged in as Jan, a flight planner who is new in Keycloak
    When I enter a departure delay of 25 minutes through the API
    Then the flight shows that Jan last changed the delay

  @integration @wip
  Scenario: The flight shows who changed a delay last
    Given the flight planner Fien entered a departure delay of 25 minutes
    And I am logged in as the flight planner Bram
    When I change that delay to 40 minutes through the API
    Then the flight shows that Bram last changed the delay

  @integration
  Scenario Outline: The flight shows who registered an actual time
    Given I am logged in as the flight planner Fien
    When I register the <time> of HJ221 through the API
    Then the flight shows that Fien registered the <time>

    Examples:
      | time |
      | ATD  |

    # Waits for the endpoint to register the ATA
    @wip
    Examples:
      | time |
      | ATA  |

  @ui
  Scenario: The details page shows who changed the flight
    Given Fien entered a departure delay for HJ221
    When I open the details of HJ221
    Then I see Fien next to the delay
