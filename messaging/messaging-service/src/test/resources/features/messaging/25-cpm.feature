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
