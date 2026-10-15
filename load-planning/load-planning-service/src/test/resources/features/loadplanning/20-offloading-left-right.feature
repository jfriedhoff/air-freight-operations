Feature: Offloading along the left and right side
  As a load planner I want it to be checked whether cargo for the destination can be offloaded along the left and right side without moving other cargo so that offloading goes smoothly

  Background:
    Given the flight goes to ATH

  @unit
  Scenario Outline: Cargo for ATH must be able to leave without moving cargo for FRA
    Given <load>
    When the offloading is checked
    Then the check has <result>

    Examples:
      | load                                | result |
      | ATH on ML, FRA on PL                | failed |
      | ATH on ML, ATH on PL                | passed |
      | ATH on MR, FRA on RL                | failed |
      | ATH on RR and UR, FRA on UL         | passed |
      | ATH on RR, FRA on RL                | failed |
      | ATH on SR, FRA on RL                | failed |
      | ATH on UL, FRA on RL                | failed |
      | ATH on ML, FRA on MR and PR         | passed |
      | ATH on RL, FRA on RR and SL         | passed |
      | ATH without load position, FRA on RL | passed |
      | only FRA on board                   | passed |
      | no cargo on board                   | passed |

  @unit
  Scenario: The report names the ULD that cannot be offloaded
    Given AKE16689R7 for ATH is on ML and AKE20417HJ for FRA is on PL
    When the offloading is checked
    Then the check has failed
    And the report states that AKE16689R7 on ML cannot be offloaded because AKE20417HJ on PL is in the way

  @unit
  Scenario: The report names every ULD that cannot be offloaded, in the order of their codes
    Given these ULDs are on board:
      | ULD        | destination | position |
      | AKE20417HJ | ATH         | ML       |
      | AKE16689R7 | ATH         | MR       |
      | AKE30111HJ | FRA         | PL       |
      | AKE40222HJ | FRA         | PR       |
    When the offloading is checked
    Then the check has failed
    And the report states:
      """
      AKE16689R7 on MR cannot be offloaded because AKE40222HJ on PR is in the way; AKE20417HJ on ML cannot be offloaded because AKE30111HJ on PL is in the way
      """
