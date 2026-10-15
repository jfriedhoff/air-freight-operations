package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.time.LocalDate;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.application.FinalizeOutcome;
import be.kdg.airfreight.loadplanning.application.FlightCargoSnapshot;
import be.kdg.airfreight.loadplanning.application.LoadPlannerService;
import be.kdg.airfreight.loadplanning.application.LoadService;
import be.kdg.airfreight.loadplanning.application.command.AssignLoadPositionCommand;
import be.kdg.airfreight.loadplanning.application.command.FinalizeLoadCommand;
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
import org.springframework.web.bind.annotation.PostMapping;
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
    private final LoadPlannerService loadPlannerService;

    public FlightCargoController(LoadService loadService, LoadPlannerService loadPlannerService) {
        this.loadService = loadService;
        this.loadPlannerService = loadPlannerService;
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
        return ResponseEntity.ok(toDto(loadService.get(new FlightCargoId(id))));
    }

    @PutMapping("/{id}/ulds/{uldCode}/load-position")
    public ResponseEntity<FlightCargoDto> assign(@PathVariable("id") UUID id,
                                                 @PathVariable("uldCode") String uldCode,
                                                 @Valid @RequestBody AssignLoadPositionDto dto,
                                                 @AuthenticationPrincipal Jwt jwt) {
        LoadPlannerId loadPlanner = LoadPlannerId.fromAccount(jwt.getSubject());
        loadPlannerService.register(loadPlanner, nameOf(jwt));
        UldCode uld = new UldCode(uldCode);
        FlightCargoSnapshot cargo = loadService.assign(
            new AssignLoadPositionCommand(new FlightCargoId(id), uld, dto.loadPosition(), loadPlanner));
        // Logged after the use case accepted it: by then the ULD code and the load position are checked values,
        // so nothing a caller typed reaches the log unchecked. A refusal is logged by ErrorHandling.
        log.info("Load planner {} put ULD {} on load position {} for flight cargo {}", loadPlanner.id(), uld.value(),
            dto.loadPosition(), id);
        return ResponseEntity.ok(toDto(cargo));
    }

    /** Check Load: every check on the load as it stands. It changes nothing; only a load planner may ask it. */
    @GetMapping("/{id}/load-check")
    public ResponseEntity<LoadCheckReportDto> checkLoad(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(LoadCheckReportDto.from(loadService.checkLoad(new FlightCargoId(id))));
    }

    /**
     * Finalize Load. The use case runs the checks again, because the load may have changed since the last Check
     * Load, and answers with that report: 200 with the report when the load is finalized, 409 with the same report
     * when a hard check fails (see ErrorHandling).
     */
    @PostMapping("/{id}/finalize")
    public ResponseEntity<LoadCheckReportDto> finalizeLoad(@PathVariable("id") UUID id,
                                                           @AuthenticationPrincipal Jwt jwt) {
        LoadPlannerId loadPlanner = LoadPlannerId.fromAccount(jwt.getSubject());
        loadPlannerService.register(loadPlanner, nameOf(jwt));
        FinalizeOutcome outcome = loadService.finalizeLoad(
            new FinalizeLoadCommand(new FlightCargoId(id), loadPlanner));
        LoadCheckReportDto dto = LoadCheckReportDto.from(outcome.report());
        if (!outcome.finalized()) {
            throw new HardCheckFailedException(dto);
        }
        log.info("Load planner {} finalized the load of flight cargo {}", loadPlanner.id(), id);
        return ResponseEntity.ok(dto);
    }

    private FlightCargoDto toDto(FlightCargoSnapshot snapshot) {
        return FlightCargoDto.from(snapshot,
            loadPlannerService.namesOf(FlightCargoDto.plannersOf(snapshot.cargo())));
    }

    /** The name Keycloak gives the account. A token without one falls back to the user name, then to the id. */
    private static String nameOf(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = jwt.getClaimAsString("preferred_username");
        }
        return name == null || name.isBlank() ? jwt.getSubject() : name;
    }
}
