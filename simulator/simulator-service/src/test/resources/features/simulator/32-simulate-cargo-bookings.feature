Feature: Simulate cargo bookings by supplying JSON
  As a trainer I want to simulate cargo bookings by supplying JSON so that I can demonstrate the loading

  @e2e
  Scenario: One ULD message per record
    Given the flights of the bookings have been processed
    When I supply three booking records as JSON
    Then the simulator sends three ULD messages over RabbitMQ
    And the ULDs are booked on their flights

  @integration
  Scenario: The scheduled departure date becomes the day of the month
    When I supply a booking record for HJ221 with scheduled departure date 9 January 2026 from BRU
    Then the sent ULD message contains flight number HJ221, day 09 and airport BRU
    And it conforms to the fixed structure of a ULD message

  @integration
  Scenario: JSON that cannot be read is refused
    When I supply JSON that cannot be read
    Then the API responds with 400 Bad Request and the reason
    And no ULD message is sent

  @integration @production
  Scenario: Not in production
    Given the application runs with the production profile
    When I supply booking records
    Then the API responds with 404 Not Found
    And no ULD message is sent
