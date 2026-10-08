package be.kdg.airfreight.flightoperations.api.flight;

import java.util.List;

import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The IATA delay reasons a planner chooses from. */
@RestController
@RequestMapping("/api/delay-codes")
public class DelayCodeController {
    private final DelayCodeCatalog delayCodes;

    public DelayCodeController(DelayCodeCatalog delayCodes) {
        this.delayCodes = delayCodes;
    }

    @GetMapping
    public ResponseEntity<List<DelayCodeDto>> all() {
        return ResponseEntity.ok(delayCodes.all().stream().map(DelayCodeDto::from).toList());
    }
}
