@wip
Feature: Be warned when booked cargo has no load position
  As a load planner I want to be warned when booked cargo has no load position so that no cargo is left behind unnoticed

  @unit
  Scenario: Booked cargo without a load position gives a warning
    Given AKE20417HJ is booked but has no load position
    When the load is checked
    Then the soft check "not all cargo loaded" has failed
    And the report states that AKE20417HJ has no load position
    But the load can still be finalized if all hard checks pass

  @unit
  Scenario: All booked cargo has a load position
    Given AKE16689R7 and AKE20417HJ are booked and both have a load position
    When the load is checked
    Then the soft check "not all cargo loaded" has passed

  @unit
  Scenario: Without booked cargo
    Given no cargo is booked on the flight
    When the load is checked
    Then the soft check "not all cargo loaded" has passed
