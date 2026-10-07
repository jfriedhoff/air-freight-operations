@wip
Feature: The lateral balance is checked
  As a load planner I want the lateral balance to be checked so that the aircraft does not depart loaded unevenly

  @unit
  Scenario Outline: The heaviest side weighs at most 110% of the lightest
    Given the L positions hold <left> in total and the R positions <right>
    When the lateral balance is checked
    Then the check has <result>

    Examples:
      | left    | right   | result |
      | 1000 kg | 1100 kg | passed |
      | 1000 kg | 1101 kg | failed |
      | 1100 kg | 1000 kg | passed |
      | 1101 kg | 1000 kg | failed |
      | 1000 kg | 2100 lb | passed |
      | 1000 kg | 2425 lb | passed |
      | 1000 kg | 2426 lb | failed |
      | 1000 kg | 0 kg    | failed |
      | 0 kg    | 0 kg    | passed |

  @unit
  Scenario: Nose and tail positions do not count
    Given B1 holds 5000 kg, CL 1000 kg and CR 1000 kg
    When the lateral balance is checked
    Then the check has passed

  @unit
  Scenario: ULDs without a load position do not count
    Given CL holds 1000 kg and CR 1000 kg
    And AKE20417HJ of 3000 kg is booked without a load position
    When the lateral balance is checked
    Then the check has passed

  @unit
  Scenario: The report for a failed check
    Given the left side holds 1000 kg and the right side 1200 kg
    When the lateral balance is checked
    Then the report states 1000 kg left, 1200 kg right and a maximum of 1100 kg on the heaviest side

  @unit
  Scenario: The report converts mixed units to kg
    Given the left side holds 1000 kg and the right side 2646 lb
    When the lateral balance is checked
    Then the check has failed
    And the report states 1000 kg left, 1200.2 kg right and a maximum of 1100 kg, all in kg
