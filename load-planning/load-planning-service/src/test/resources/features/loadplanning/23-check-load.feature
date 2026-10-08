Feature: Check the load with Check Load
  As a load planner I want to check the load with Check Load so that I know what I still have to change

  @unit @ui
  Scenario: One report per check
    Given HJ221 goes to ATH and it is within the loading period
    And AKE16689R7, 1000 kg for ATH, is on CL and AKE20417HJ, 1200 kg for FRA, is on CR
    And AKE30111HJ is booked without a load position
    When I click Check Load
    Then I see this report:
      | check                | result | reason                                                                              | type |
      | Lateral balance      | failed | Left carries 1000 kg and right 1200 kg; the heaviest side may carry at most 1100 kg | hard |
      | Offloading           | passed |                                                                                     | hard |
      | Not all cargo loaded | failed | AKE30111HJ has no load position                                                     | soft |

  # Needs the REST endpoint for Check Load, which load-planning does not have yet. checkLoad also works outside the loading period (REFLECTIE.md)
  @ui @integration @wip
  Scenario Outline: Not outside the loading period
    Given the loading period <state>
    Then I cannot use Check Load
    And the API refuses a check too, with an explanation

    Examples:
      | state               |
      | has not started yet |
      | is over             |

  # Needs the REST endpoint for Check Load, which load-planning does not have yet
  @integration @wip
  Scenario: Check Load changes nothing
    Given it is within the loading period
    When I click Check Load
    Then the load remains unchanged
    And no message is sent
