@wip
Feature: Every message contains a unique identification, its creation time and the format version
  As a partner I want every message to contain a unique identification, its creation time and the format version so that I can recognize messages and process them correctly

  @integration
  Scenario Outline: Every message can be recognized
    When the system creates a <message>
    Then that message has a unique identification, its creation time in UTC and the format version

    Examples:
      | message |
      | FLM     |
      | MVT-ED  |
      | MVT-EA  |
      | MVT-AD  |
      | MVT-AA  |
      | CPM     |

  @integration
  Scenario: For a telegram that data is in the AMQP headers
    When the system creates an MVT-ED
    Then the body of the message contains only the three lines of the telegram
    And the identification, creation time and format version are in the AMQP properties and headers

  @integration
  Scenario: Two messages never have the same identification
    When one departure delay produces an MVT-ED and an MVT-EA
    Then their identifications differ

  @integration
  Scenario: A resent message keeps its identification
    Given an MVT-ED was published, but Messaging stopped before it marked the message as sent
    When Messaging starts again
    Then the MVT-ED is sent again with the same identification
