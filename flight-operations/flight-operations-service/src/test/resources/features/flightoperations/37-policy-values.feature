Feature: Change the policy values without a new release
  As an operations manager I want to change the policy values without a new release so that I can adjust the policy

  @integration
  Scenario Outline: Changing a policy value without a new release
    Given <value> has been changed to <new> in the external configuration
    And the unit has been restarted without a new build
    When <action>
    Then <result>

    Examples:
      | value                                     | new     | action                                                                                         | result                                  |
      | the tolerance for actual times            | 10 min  | the flight planner registers 06:54 as the ATD with an ETD of 06:45 UTC                         | the registration is accepted            |
      | the retention period                      | 10 days | it becomes 16 January 2026                                                                     | the flight of 9 January 2026 still exists |
