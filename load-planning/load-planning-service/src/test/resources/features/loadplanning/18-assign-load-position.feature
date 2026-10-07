@wip
Feature: Assign, change or clear a load position from the floor plan
  As a load planner I want to assign, change or clear a load position from the floor plan for a ULD so that the cargo is planned on the aircraft

  Background:
    Given flight HJ221 with STD 9 January 2026 06:30 UTC
    And AKE16689R7 is booked on HJ221
    And it is 9 January 05:00 UTC, within the loading period

  @unit
  Scenario: Assigning a load position within the loading period
    When I place AKE16689R7 on CL
    Then AKE16689R7 is on CL
    And it is recorded that I assigned the load position

  @unit
  Scenario Outline: Only within the loading period, from STD − 12 h until before ETD − 20 min
    Given the departure delays add up to <delay> minutes
    And it is <moment> UTC
    When I place AKE16689R7 on CL
    Then that is <result>

    Examples:
      | delay | moment          | result   |
      | 0     | 8 January 18:29 | refused  |
      | 0     | 8 January 18:30 | accepted |
      | 0     | 9 January 06:09 | accepted |
      | 0     | 9 January 06:10 | refused  |
      | 25    | 8 January 18:29 | refused  |
      | 25    | 8 January 18:30 | accepted |
      | 25    | 9 January 06:34 | accepted |
      | 25    | 9 January 06:35 | refused  |

  @unit @to-confirm
  Scenario: A departure delay reopens a loading period that has ended
    Given there is no departure delay and the load has not been finalized
    And it is 9 January 06:15 UTC
    And afterwards a departure delay of 25 minutes is entered
    When I place AKE16689R7 on CL
    Then that is accepted

  @unit @to-confirm
  Scenario: Removing a delay closes the loading period
    Given there is a departure delay of 25 minutes
    And it is 9 January 06:20 UTC
    And afterwards that delay is removed
    When I place AKE16689R7 on CL
    Then that is refused with an explanation

  @unit
  Scenario: An occupied position is refused
    Given AKE20417HJ is already on CL
    When I place AKE16689R7 on CL
    Then that is refused with an explanation

  @unit
  Scenario Outline: Only positions from the floor plan
    When I place AKE16689R7 on <position>
    Then that is refused with an explanation

    Examples:
      | position |
      | Z9       |
      | NL       |
      | IR       |
      | XL       |
      | B2       |
      | A3       |
      | C        |

  @unit
  Scenario: Moving a ULD frees the old position
    Given AKE16689R7 is on CL
    When I place AKE16689R7 on CR
    Then AKE16689R7 is on CR
    And CL is free

  @unit
  Scenario: Clearing a load position
    Given AKE16689R7 is on CL
    When I clear the load position of AKE16689R7
    Then AKE16689R7 no longer has a load position
    And CL is free
    And it is recorded that I cleared the load position

  @unit
  Scenario: Clearing is also only possible within the loading period
    Given AKE16689R7 is on CL
    And it is 9 January 06:10 UTC
    When I clear the load position of AKE16689R7
    Then that is refused with an explanation

  @unit
  Scenario Outline: No longer possible after finalizing
    Given AKE16689R7 is on CL
    And the load has been finalized
    When I <action>
    Then that is refused with an explanation

    Examples:
      | action                                 |
      | place AKE16689R7 on CR                 |
      | clear the load position of AKE16689R7  |

  @integration
  Scenario: Two load planners choose the same position at the same time
    Given AKE20417HJ is also booked on HJ221
    When two load planners place AKE16689R7 and AKE20417HJ on CL at the same time
    Then exactly one of the two ULDs is on CL
    And the other load planner gets a refusal with an explanation

  @e2e @to-confirm
  Scenario: Assigning a load position sends no message to the partners
    When I place AKE16689R7 on CL
    Then no message is sent to the partners
