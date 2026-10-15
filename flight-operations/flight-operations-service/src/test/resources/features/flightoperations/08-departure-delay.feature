Feature: Enter a departure delay with a delay reason and a description
  As a flight planner I want to enter a departure delay with a delay reason and a description so that everyone works with the right departure time

  Background:
    Given flight HJ221 of 9 January 2026 is planned with STD 06:30 and STA 09:45 UTC

  @unit
  Scenario: The delay shifts the ETD and ETA
    When I enter a departure delay of 25 minutes with a valid delay reason and a description
    Then the ETD is 06:55
    And the ETA is 10:10
    And it is recorded that I entered the delay

  @unit
  Scenario Outline: Each delay lasts between 1 minute and 12 hours
    When I enter a departure delay of <minutes> minutes
    Then it is <result>

    Examples:
      | minutes | result   |
      | 0       | refused  |
      | 1       | accepted |
      | 720     | accepted |
      | 721     | refused  |

  @unit
  Scenario Outline: All delays together last at most 12 hours
    Given HJ221 already has a departure delay of 700 minutes
    When I enter a <kind> of <minutes> minutes
    Then it is <result>

    Examples:
      | kind            | minutes | result   |
      | departure delay | 20      | accepted |
      | departure delay | 21      | refused  |
      | arrival delay   | 21      | refused  |

  @unit
  Scenario: Several delays add up
    Given HJ221 has a departure delay of 25 minutes
    When I enter another departure delay of 10 minutes and an arrival delay of 15 minutes
    Then the ETD is 07:05
    And the ETA is 10:35

  @unit
  Scenario: An unknown delay reason is refused
    When I enter a departure delay with a delay reason that is not in the IATA list
    Then it is refused with an explanation

  @unit
  Scenario: A description is required
    When I enter a departure delay without a description
    Then it is refused with an explanation

  @unit
  Scenario: A delay across midnight does not change the scheduled departure date
    Given flight 3V11 of 9 January 2026 is planned with STD 22:30 UTC
    When I enter a departure delay of 120 minutes for 3V11
    Then the ETD of 3V11 is 10 January 2026 00:30 UTC
    But it remains the flight 3V11 of 9 January 2026

  @unit
  Scenario: No longer possible after departure
    Given flight HJ221 has departed
    When I enter a departure delay
    Then it is refused with an explanation

  @integration
  Scenario: A flight planner enters a departure delay through the API
    Given I am logged in as a flight planner
    When I enter a departure delay of 25 minutes through the API
    Then the API answers 200
    And the API shows the ETD 06:55 and the ETA 10:10
    And DepartureEstimateChanged and ArrivalEstimateChanged wait in the outbox

  @integration
  Scenario Outline: Only a flight planner enters a delay through the API
    Given I am logged in as <who>
    When I enter a departure delay of 25 minutes through the API
    Then the API answers <status>
    And HJ221 has no delay

    Examples:
      | who                            | status |
      | a load planner                 | 403    |
      | a user with both planner roles | 403    |
      | nobody                         | 401    |

  @integration
  Scenario: A delay reason outside the IATA list is refused through the API
    Given I am logged in as a flight planner
    When I enter a departure delay with delay reason 50 through the API
    Then the API answers 400
    And the explanation says "Delay reason '50' is not in the IATA list"

  @integration
  Scenario: A departure delay on a departed flight is refused through the API
    Given I am logged in as a flight planner
    And flight HJ221 has departed
    When I enter a departure delay of 25 minutes through the API
    Then the API answers 409
    And the explanation says "A departure delay can only change while the flight is planned"

  @integration
  Scenario: A delay for a flight that does not exist
    Given I am logged in as a flight planner
    When I enter a departure delay for a flight that does not exist through the API
    Then the API answers 404