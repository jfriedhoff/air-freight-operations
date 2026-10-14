Feature: Partners receive an MVT-ED and MVT-EA when the expected times change
  As a partner I want to receive an MVT-ED and MVT-EA when the expected times change so that I can adjust my planning

  Background:
    Given flight HJ221 of 9 January 2026 with registration SXBAA from BRU to ATH
    And the STD is 06:30 and the STA 09:45 UTC

  @e2e
  Scenario: A changed ETD sends an MVT-ED and an MVT-EA
    When I enter a departure delay of 15 minutes
    Then this telegram is sent:
      """
      MVT
      HJ221/09.SXBAA.BRU
      ED0645
      """
    And this telegram is sent:
      """
      MVT
      HJ221/09.SXBAA.ATH
      EA1000
      """

  @e2e
  Scenario: A changed departure delay sends a new MVT-ED and MVT-EA
    Given there is one departure delay of 15 minutes
    When I change that delay to 30 minutes
    Then an MVT-ED with ED0700 and an MVT-EA with EA1015 are sent

  @e2e
  Scenario: Only a changed ETA sends only an MVT-EA
    When I enter an arrival delay of 10 minutes
    Then only an MVT-EA with EA0955 is sent

  @e2e
  Scenario: An arrival delay after departure sends an MVT-EA
    Given the flight has departed
    When I enter an arrival delay of 20 minutes
    Then only an MVT-EA with EA1005 is sent

  @e2e
  Scenario: Removing the last delay is announced too
    Given there is one departure delay of 15 minutes
    When I remove that delay
    Then an MVT-ED with ED0630 and an MVT-EA with EA0945 are sent

  @e2e
  Scenario: Removing an arrival delay sends only an MVT-EA
    Given there is one arrival delay of 10 minutes
    When I remove that delay
    Then only an MVT-EA with EA0945 is sent

  @e2e
  Scenario: Changing only the description sends no telegram
    Given there is one departure delay of 15 minutes
    When I change only the description of that delay
    Then no MVT is sent

  @e2e
  Scenario: A delay across midnight does not change the day in the telegram
    Given flight 3V11 of 9 January 2026 with registration OOTHA from BRU to ATH
    And the STD of 3V11 is 9 January 22:30 and the STA 10 January 01:45 UTC
    When I enter a departure delay of 120 minutes for 3V11
    Then this telegram is sent:
      """
      MVT
      3V11/09.OOTHA.BRU
      ED0030
      """
    And this telegram is sent:
      """
      MVT
      3V11/09.OOTHA.ATH
      EA0345
      """

  @e2e
  Scenario: A telegram contains no delay reason
    When a departure delay with a delay reason is entered
    Then no sent telegram contains that delay reason
