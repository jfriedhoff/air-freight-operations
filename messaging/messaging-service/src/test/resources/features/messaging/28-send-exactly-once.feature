Feature: Messages are still sent exactly once when the connection is restored
  As an operations manager I want messages to still be sent exactly once when the connection is restored so that partners never miss a change

  Background:
    Given flight HJ221 of 9 January 2026 with STD 06:30 and STA 09:45 UTC

  # "A change succeeds even when RabbitMQ is unreachable" tests Flight Operations (its API and outbox): it is in
  # flight-operations/.../features/flightoperations/28-send-exactly-once.feature.

  @e2e
  Scenario: After recovery every message is sent exactly once
    Given RabbitMQ was unreachable when the flight planner entered a departure delay of 15 minutes
    When RabbitMQ is reachable again
    Then the MVT-ED and the MVT-EA are sent
    And each is sent exactly once

  @e2e
  Scenario: When Messaging is not running, the events wait in the queue
    Given Messaging is not running
    When the flight planner enters a departure delay of 15 minutes
    And Messaging starts again afterwards
    Then the MVT-ED and the MVT-EA are sent exactly once

  @e2e
  Scenario: Changes during an outage are sent in order
    Given RabbitMQ is unreachable
    When the flight planner enters a departure delay of 15 minutes and then changes it to 30 minutes
    And RabbitMQ is reachable again
    Then first the MVT-ED with ED0645 is sent and then the one with ED0700

  @e2e
  Scenario: An FLM is not lost
    Given Flight Operations has created a new flight
    But RabbitMQ became unreachable before the event was sent
    When RabbitMQ is reachable again
    Then the FLM is sent exactly once

  @e2e
  Scenario: A CPM is not lost
    Given RabbitMQ is unreachable
    When the load planner finalizes the load of HJ221
    And RabbitMQ is reachable again
    Then the CPM is sent exactly once
    And the flight planner can register the ATD afterwards

  @integration
  Scenario: The same event twice at Messaging gives one telegram
    When Messaging receives the same event about the departure delay of HJ221 twice
    Then one MVT-ED and one MVT-EA are created
    And the duplicate is logged
