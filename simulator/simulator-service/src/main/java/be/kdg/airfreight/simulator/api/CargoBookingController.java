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

/** Imitates the cargo booking system: every supplied booking record leaves as one ULD message. */
@Slf4j
@RestController
@RequestMapping("/api/cargo-bookings")
public class CargoBookingController {
    private final SimulatorService simulator;

    public CargoBookingController(SimulatorService simulator) {
        this.simulator = simulator;
    }

    /** 202 Accepted: the ULD messages are sent, the ULDs are booked later by the services that read them. */
    @PostMapping
    public ResponseEntity<SentMessagesDto> send(
        @RequestBody @NotEmpty(message = "Supply at least one booking record")
        List<@NotNull(message = "A record cannot be null") @Valid BookingRecordDto> records
    ) {
        log.info("Received {} booking records", records.size());
        List<UUID> messageIds = simulator.sendBookings(records.stream().map(BookingRecordDto::toRecord).toList());
        return ResponseEntity.accepted().body(new SentMessagesDto(messageIds));
    }
}
