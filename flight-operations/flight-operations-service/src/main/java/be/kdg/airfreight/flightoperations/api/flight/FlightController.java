package be.kdg.airfreight.flightoperations.api.flight;

import java.time.LocalDate;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.application.FlightService;
import be.kdg.airfreight.flightoperations.application.FlightsQuery;
import be.kdg.airfreight.flightoperations.application.command.AddDelayCommand;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.DelayDuration;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** There is no PUT or PATCH on a flight on purpose: nobody changes scheduled or estimated times directly. */
@Slf4j
@RestController
@RequestMapping("/api/flights")
public class FlightController {
    private final FlightService flightService;
    private final AirportCatalog airports;
    private final DelayCodeCatalog delayCodes;

    public FlightController(FlightService flightService, AirportCatalog airports, DelayCodeCatalog delayCodes) {
        this.flightService = flightService;
        this.airports = airports;
        this.delayCodes = delayCodes;
    }

    /**
     * The flights of one UTC day; without a day, today. With a flight number and a departure airport, the one flight
     * of that day with that identity, as a page of the other context links to it. An empty list is a valid answer.
     */
    @GetMapping
    public ResponseEntity<FlightsOfDayDto> flightsOf(
        @RequestParam(required = false) @Nullable LocalDate scheduledDepartureDate,
        @RequestParam(required = false) @Nullable String flightNumber,
        @RequestParam(required = false) @Nullable String departureAirport) {
        FlightsQuery query = new FlightsQuery(scheduledDepartureDate,
            flightNumber == null ? null : new FlightNumber(flightNumber),
            departureAirport == null ? null : new AirportCode(departureAirport));
        return ResponseEntity.ok(FlightsOfDayDto.from(flightService.flightsOf(query)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightDto> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(toDto(flightService.get(new FlightId(id))));
    }

    @PostMapping("/{id}/delays")
    public ResponseEntity<FlightDto> addDelay(@PathVariable("id") UUID id, @Valid @RequestBody AddDelayDto dto,
                                              @AuthenticationPrincipal Jwt jwt) {
        FlightPlannerId flightPlanner = FlightPlannerId.fromAccount(jwt.getSubject());
        log.info("Flight planner {} enters a {} delay of {} minutes for flight {}", flightPlanner.id(),
            dto.kind(), dto.minutes(), id);
        AddDelayCommand command = new AddDelayCommand(new FlightId(id), dto.kind(),
            new DelayDuration(dto.minutes()), new DelayCode(dto.reason()), dto.description(), flightPlanner);
        return ResponseEntity.ok(toDto(flightService.addDelay(command)));
    }

    private FlightDto toDto(Flight flight) {
        return FlightDto.from(flight, airports, delayCodes);
    }
}
