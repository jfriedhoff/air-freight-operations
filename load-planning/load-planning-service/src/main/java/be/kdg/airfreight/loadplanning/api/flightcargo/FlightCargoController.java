package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.util.UUID;

import be.kdg.airfreight.loadplanning.application.LoadService;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The cargo of a flight is the resource of Load Planning. */
@RestController
@RequestMapping("/api/flight-cargos")
public class FlightCargoController {
    private final LoadService loadService;

    public FlightCargoController(LoadService loadService) {
        this.loadService = loadService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightCargoDto> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(FlightCargoDto.from(loadService.get(new FlightCargoId(id))));
    }
}
