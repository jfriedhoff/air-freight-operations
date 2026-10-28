Feature: Partners receive a CPM when the load has been finalized
  As a partner I want to receive a CPM when the load has been finalized so that I know which cargo is on board

  @e2e
  Scenario: The CPM contains the placed ULDs
    Given AKE16689R7 is on CL on HJ221, 1250 kg, destination ATH
    And AKE20417HJ is booked without a load position
    When the load is finalized
    Then a CPM is sent with flight number, scheduled departure date and departure airport
    And the CPM contains AKE16689R7 with CL, 1250 kg and ATH
    But the CPM does not contain AKE20417HJ

  @e2e
  Scenario: A load without cargo sends a CPM without ULDs
    Given no cargo is booked on HJ221
    When the load is finalized
    Then a CPM is sent with an empty list of ULDs

  @e2e
  Scenario: The CPM releases the departure
    Given the CPM of HJ221 has been sent
    When Flight Operations has processed the CPM
    Then the flight planner can register the ATD of HJ221

  @integration
  Scenario: A finalized load is sent to the partners as a CPM and releases the departure
    When Load Planning reports the finalized load of HJ221 with AKE16689R7 on CL, 1250 kg, destination ATH
    Then a CPM is sent with flight number HJ221, scheduled departure date 9 January 2026 and departure airport BRU
    And the CPM contains AKE16689R7 with CL, 1250 kg and ATH
    And Flight Operations is told that the CPM of HJ221 of 9 January 2026 from BRU has been written

  @integration
  Scenario: A finalized load without cargo is sent as a CPM without ULDs
    When Load Planning reports the finalized load of HJ221 without ULDs
    Then a CPM is sent with an empty list of ULDs

  @unit
  Scenario Outline: A ULD line in the CPM needs a ULD code, a load position and a weight unit
    When Load Planning reports a finalized load with a ULD line <line>
    Then the finalized load is <result>

    Examples:
      | line                    | result  |
      | AKE16689R7 on CL in KG  | valid   |
      | without a ULD code      | invalid |
      | without a load position | invalid |
      | without a weight unit   | invalid |
