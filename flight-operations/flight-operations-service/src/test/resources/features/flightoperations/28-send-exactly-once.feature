Feature: Messages are still sent exactly once when the connection is restored
  As an operations manager I want messages to still be sent exactly once when the connection is restored so that partners never miss a change

  # The part of story 28 that is about Flight Operations; the other scenarios are in
  # messaging/.../features/messaging/28-send-exactly-once.feature.

  Background:
    Given flight HJ221 of 9 January 2026 with STD 06:30 and STA 09:45 UTC

  @integration
  Scenario: A change succeeds even when RabbitMQ is unreachable
    Given RabbitMQ is unreachable
    When the flight planner enters a departure delay of 15 minutes
    Then he gets a confirmation
    And the delay has been stored
    And the event is waiting in the outbox of Flight Operations
