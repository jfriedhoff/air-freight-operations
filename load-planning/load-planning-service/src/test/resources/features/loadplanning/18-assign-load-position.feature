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

  @unit
  Scenario: A departure delay does not reopen a loading period that has ended
    Given there is no departure delay and the load has not been finalized
    And it is 9 January 06:15 UTC
    And afterwards a departure delay of 25 minutes is entered
    When I place AKE16689R7 on CL
    Then that is refused with an explanation

  @unit
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

  @e2e
  Scenario: Assigning a load position sends no message to the partners
    When I place AKE16689R7 on CL
    Then no message is sent to the partners

  # The floor plan on the cargo page (team extension, see analyse.md question 14). Walked through by hand.
  @ui
  Scenario: The deck shows free and taken positions
    Given AKE16689R7 is on CL
    When I open the cargo page of HJ221
    Then I see the main deck from above with the nose on the left and the cargo door at RL
    And CL shows AKE16689R7 in the colour of a ULD for the destination of the flight
    And every other position is shown as free

  @ui
  Scenario: Placing a ULD by clicking a free position
    When I pick AKE16689R7 among the ULDs still to place
    Then every free position is shown as a place it can go to
    When I click CL
    Then AKE16689R7 is on CL
    And the list next to AKE16689R7 shows CL

  @ui
  Scenario: The deck shows which ULD is in the way
    Given HJ221 goes to ATH
    And AKE16689R7 for ATH is on CL
    When I place AKE20417HJ for FRA on DL
    Then CL is marked as a ULD that cannot be offloaded
    And DL is marked as a ULD that is in the way
    And the reason of the offloading check is shown below the deck

  @ui
  Scenario: The deck shows the weight on each side
    Given AKE16689R7 of 1250 kg is on CL
    When I open the cargo page of HJ221
    Then I see 1250 kg on the left and 0 kg on the right, and that the heaviest side may carry at most 0 kg
    And the left side is shown as too heavy

  @ui
  Scenario: A flight planner sees the deck without changing it
    Given I am logged in as a flight planner
    When I open the cargo page of HJ221
    Then I see the deck with every ULD on its position
    But I cannot pick a ULD or a position, and I see no findings of the checks
