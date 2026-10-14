Feature: Offloading from the nose and the tail
  As a load planner I want it to be checked whether cargo for the destination in the nose and the tail can be offloaded without moving other cargo so that offloading goes smoothly

  Background:
    Given the flight goes to ATH

  @unit
  Scenario Outline: Nose and tail need room to manoeuvre
    Given <load>
    When the offloading is checked
    Then the check has <result>

    Examples:
      | load                                        | result |
      | ATH on B1, FRA on CR                        | failed |
      | ATH on B1, FRA on DL, CL and CR free        | passed |
      | ATH on B1 and CL, CR free                   | passed |
      | ATH on B1, FRA on DL and DR, CL and CR free | failed |
      | ATH on A1, FRA on CL                        | failed |
      | ATH on A2, FRA on DR, CL and CR free        | passed |
      | ATH on X, FRA on UL                         | failed |
      | ATH on X, FRA on TR, UL and UR free         | passed |
      | ATH on X and UL, UR free                    | passed |
      | ATH on X, FRA on TL and TR, UL and UR free  | failed |

  @unit
  Scenario Outline: Cargo in the nose must be able to pass the positions behind it
    Given <load>
    When the offloading is checked
    Then the check has <result>

    Examples:
      | load                                     | result |
      | ATH on A1, FRA on A2, CL and CR free     | failed |
      | ATH on A1, FRA on B1, CL and CR free     | failed |
      | ATH on A1, A2 and B1, CL and CR free     | passed |
      | ATH on A2, FRA on B1, CL and CR free     | failed |
      | ATH on A2, FRA on A1, CL and CR free     | passed |

  @unit
  Scenario: The report names every ULD in the way
    Given these ULDs are on board:
      | ULD        | destination | position |
      | AKE16689R7 | ATH         | B1       |
      | AKE40222HJ | FRA         | DL       |
      | AKE20417HJ | FRA         | CL       |
      | AKE30111HJ | FRA         | CR       |
    When the offloading is checked
    Then the check has failed
    And the report states:
      """
      AKE16689R7 on B1 cannot be offloaded because AKE20417HJ on CL, AKE30111HJ on CR and AKE40222HJ on DL are in the way
      """
