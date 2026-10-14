package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AircraftType;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightInfo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPolicy;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlightToLoadDtoTest {
    private static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    private static final LoadingPolicy POLICY = new LoadingPolicy(Duration.ofHours(12), Duration.ofMinutes(20));

    private final FlightCargo cargo = FlightCargo.create(
        new FlightId(new FlightNumber("3V11"), LocalDate.of(2026, 1, 9), new AirportCode("BRU")),
        FlightInfo.scheduled(new AirportCode("ATH"), new AircraftType("TEST"),
            Instant.parse("2026-01-09T06:30:00Z"), Instant.parse("2026-01-09T09:45:00Z")), POLICY);

    @Test
    void aFlightToLoadIsShownWithItsNumberDateAirportsTimesAndWhetherItsLoadIsFinalized() {
        cargo.updateFlightInfo(cargo.getFlightInfo().withEstimatedDeparture(Instant.parse("2026-01-09T06:55:00Z"))
            .withEstimatedArrival(Instant.parse("2026-01-09T10:10:00Z")));

        assertThat(FlightToLoadDto.from(cargo)).isEqualTo(new FlightToLoadDto(cargo.getId().id(), "3V11",
            LocalDate.of(2026, 1, 9), "BRU", "ATH",
            Instant.parse("2026-01-09T06:30:00Z"), Instant.parse("2026-01-09T09:45:00Z"),
            Instant.parse("2026-01-09T06:55:00Z"), Instant.parse("2026-01-09T10:10:00Z"), false, false));
    }

    @Test
    void aFinalizedLoadAndADepartedFlightAreMarked() {
        cargo.finalizeLoad(LoadPlannerId.create(), NOW, List.of(), new FloorPlan(new AircraftType("TEST"), Map.of()),
            POLICY);
        cargo.updateFlightInfo(cargo.getFlightInfo().asDeparted());

        FlightToLoadDto dto = FlightToLoadDto.from(cargo);

        assertThat(dto.finalized()).isTrue();
        assertThat(dto.departed()).isTrue();
    }
}
