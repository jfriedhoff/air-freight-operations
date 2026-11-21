Feature: Unreachable databases and queues and unexpected errors are logged
  As an operations manager I want unreachable databases and queues and unexpected errors to be logged so that I can track down problems quickly

  # Every service logs its own errors: this file is the Load Planning part of story 39.

  @integration
  Scenario Outline: Technical errors are logged
    Given <failure>
    When the system notices it
    Then an error is logged at error level, with enough detail to find the cause

    Examples:
      | failure                                 |
      | the database is unreachable             |
      | a database operation fails unexpectedly |
      | RabbitMQ is unreachable                 |
      | a message cannot be put on the queue    |

  @integration
  Scenario Outline: Unusual messages and attempts are logged
    When <event>
    Then that is logged at level <level>, with the identification of the message or the flight

    # Load Planning has no inbox (story 26): a message that arrives twice is only recognized
    # when it would create what already exists, such as the cargo of an announced flight.
    Examples:
      | event                                | level |
      | a message with invalid data arrives  | warn  |
      | a flight announcement arrives twice  | info  |
