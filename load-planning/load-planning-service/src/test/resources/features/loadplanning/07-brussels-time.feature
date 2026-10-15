Feature: See and enter times in Brussels time
  As an operations employee in Brussels I want to see and enter times in Brussels time so that I don't have to convert them myself

  # Story 7 is split over the services, like story 37: this file keeps the screens of Load Planning.
  # The other screens and entering the ATD are in Flight Operations.

  @ui
  Scenario Outline: Every screen states which time it shows
    When I open <screen>
    Then the screen states that the times are in Brussels time

    Examples:
      | screen              |
      | the flights to load |
      | the cargo page      |
