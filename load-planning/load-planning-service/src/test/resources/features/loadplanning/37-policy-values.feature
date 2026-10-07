@wip
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
      | the start of the loading period           | 10 h    | the load planner assigns a load position at 19:30 the evening before, with an STD of 06:30 UTC | the assignment is refused               |
      | the end of the loading period             | 30 min  | the load planner assigns a load position at 06:05, with an STD of 06:30 UTC                    | the assignment is refused               |
      | the allowed lateral balance difference    | 20%     | the lateral balance is checked for 1000 kg left and 1200 kg right                              | the check has passed                    |
