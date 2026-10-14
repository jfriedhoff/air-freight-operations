package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.time.LocalDate;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.application.FlightCargoSnapshot;
import be.kdg.airfreight.loadplanning.application.LoadService;
import be.kdg.airfreight.loadplanning.application.command.AssignLoadPositionCommand;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The cargo of a flight is the resource of Load Planning. The load position of a ULD is a sub-resource with
 * at most one value: PUT sets it, whether it is the first one or a change.
 */
@Slf4j
@RestController
@RequestMapping("/api/flight-cargos")
public class FlightCargoController {
    private final LoadService loadService;

    public FlightCargoController(LoadService loadService) {
        this.loadService = loadService;
    }

    @GetMapping
    public ResponseEntity<FlightsToLoadDto> flightsToLoad(
        @RequestParam(required = false) @Nullable LocalDate scheduledDepartureDate,
        @RequestParam(defaultValue = "false") boolean includeDeparted) {
        return ResponseEntity.ok(FlightsToLoadDto.from(
            loadService.flightsToLoad(scheduledDepartureDate, includeDeparted)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightCargoDto> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(FlightCargoDto.from(loadService.get(new FlightCargoId(id))));
    }

    @PutMapping("/{id}/ulds/{uldCode}/load-position")
    public ResponseEntity<FlightCargoDto> assign(@PathVariable("id") UUID id,
                                                 @PathVariable("uldCode") String uldCode,
                                                 @Valid @RequestBody AssignLoadPositionDto dto,
                                                 @AuthenticationPrincipal Jwt jwt) {
        LoadPlannerId loadPlanner = LoadPlannerId.fromAccount(jwt.getSubject());
        UldCode uld = new UldCode(uldCode);
        FlightCargoSnapshot cargo = loadService.assign(
            new AssignLoadPositionCommand(new FlightCargoId(id), uld, dto.loadPosition(), loadPlanner));
        // Logged after the use case accepted it: by then the ULD code and the load position are checked values,
        // so nothing a caller typed reaches the log unchecked. A refusal is logged by ErrorHandling.
        log.info("Load planner {} put ULD {} on load position {} for flight cargo {}", loadPlanner.id(), uld.value(),
            dto.loadPosition(), id);
        return ResponseEntity.ok(FlightCargoDto.from(cargo));
    }
}
