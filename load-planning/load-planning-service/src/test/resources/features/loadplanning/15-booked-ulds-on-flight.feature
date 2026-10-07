@wip
Feature: Booked ULDs appear on the right flight automatically
  As a load planner I want booked ULDs to appear on the right flight automatically so that I know which cargo I have to plan

  Background:
    Given flight HJ221 of 9 January 2026 from BRU exists

  @e2e
  Scenario: A booking appears on the right flight
    Given it is 2 January 2026
    When a ULD message arrives for HJ221, day 09, BRU, with ULD AKE16689R7, 1250 kg, destination ATH
    Then AKE16689R7 is booked on HJ221 of 9 January
    And the ULD has no load position yet

  @integration
  Scenario: A second booking of the same ULD code is ignored
    Given AKE16689R7 is booked with 1250 kg on HJ221 of 9 January
    When a new ULD message, with a different identification, books AKE16689R7 with 1400 kg on the same flight
    Then there is still one booking of AKE16689R7 with 1250 kg
    And the second booking is logged

  @unit
  Scenario: The same ULD code is allowed on another flight
    Given flight HJ221 of 16 January 2026 from BRU also exists
    And AKE16689R7 is booked on HJ221 of 9 January
    When AKE16689R7 is booked on HJ221 of 16 January
    Then AKE16689R7 is booked on both flights

  @integration
  Scenario: A booking for an unknown flight
    When a ULD message arrives for HJ999, day 09, BRU, and that flight does not exist
    Then the message is set aside and logged with its identification and the reason

  @integration
  Scenario: No more bookings after departure
    Given flight HJ221 of 9 January has departed
    When a ULD message for that flight arrives
    Then the booking is not applied
    And the message is set aside and logged

  @unit @to-confirm
  Scenario: A booking after finalizing stays behind
    Given the load of HJ221 has been finalized, but the flight has not departed yet
    When AKE20417HJ is booked on HJ221
    Then AKE20417HJ is booked without a load position
    And no load position can be assigned to it anymore
    And the finalized load remains unchanged
