package be.kdg.airfreight.simulator.api;

import java.util.List;
import java.util.UUID;

import be.kdg.airfreight.simulator.application.SimulatorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Imitates the network planning system: every supplied flight schedule record leaves as one SSM. */
@Slf4j
@RestController
@RequestMapping("/api/flight-schedules")
public class FlightScheduleController {
    private final SimulatorService simulator;

    public FlightScheduleController(SimulatorService simulator) {
        this.simulator = simulator;
    }

    /** 202 Accepted: the SSMs are sent, the flights are created later by the services that read them. */
    @PostMapping
    public ResponseEntity<SentMessagesDto> send(
        @RequestBody @NotEmpty(message = "Supply at least one flight schedule record")
        List<@NotNull(message = "A record cannot be null") @Valid ScheduleRecordDto> records
    ) {
        log.info("Received {} flight schedule records", records.size());
        List<UUID> messageIds = simulator.sendSchedules(records.stream().map(ScheduleRecordDto::toRecord).toList());
        return ResponseEntity.accepted().body(new SentMessagesDto(messageIds));
    }
}
