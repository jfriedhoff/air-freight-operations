Feature: Partners receive an MVT-AD and MVT-AA
  As a partner I want to receive an MVT-AD and MVT-AA so that I know when the aircraft departed and landed

  Background:
    Given flight HJ221 of 9 January 2026 with registration SXBAA from BRU to ATH

  @e2e
  Scenario: A registered departure sends an MVT-AD
    Given the finalized load has been received
    When I register 06:52 as the ATD
    Then this telegram is sent:
      """
      MVT
      HJ221/09.SXBAA.BRU
      AD0652
      """

  @e2e
  Scenario: A registered arrival sends an MVT-AA
    Given the flight has departed
    When I register 10:04 as the ATA
    Then this telegram is sent:
      """
      MVT
      HJ221/09.SXBAA.ATH
      AA1004
      """

  @e2e
  Scenario: A departure after midnight uses the day of the scheduled departure date
    Given flight 3V11 of 9 January 2026 with registration OOTHA from BRU to ATH has an ETD of 10 January 00:30 UTC
    And the finalized load of 3V11 has been received
    When I register 10 January 00:28 UTC as the ATD for 3V11
    Then this telegram is sent:
      """
      MVT
      3V11/09.OOTHA.BRU
      AD0028
      """

  @e2e
  Scenario: An arrival after midnight uses the day of the scheduled departure date
    Given flight 3V11 of 9 January 2026 with registration OOTHA from BRU to ATH has departed
    When I register 10 January 01:40 UTC as the ATA for 3V11
    Then this telegram is sent:
      """
      MVT
      3V11/09.OOTHA.ATH
      AA0140
      """

  @integration
  Scenario: A registered ATD and ATA are sent to the partners as an MVT-AD and an MVT-AA
    When Flight Operations reports an ATD of 9 January 2026 06:52 and an ATA of 9 January 2026 10:04 UTC
    Then this telegram is sent:
      """
      MVT
      HJ221/09.SXBAA.BRU
      AD0652
      """
    And this telegram is sent:
      """
      MVT
      HJ221/09.SXBAA.ATH
      AA1004
      """

  @unit
  Scenario: An ATD and ATA after midnight keep the day of the scheduled departure date
    When Flight Operations reports an ATD of 10 January 2026 00:28 and an ATA of 10 January 2026 01:40 UTC
    Then this telegram is written:
      """
      MVT
      HJ221/09.SXBAA.BRU
      AD0028
      """
    And this telegram is written:
      """
      MVT
      HJ221/09.SXBAA.ATH
      AA0140
      """
