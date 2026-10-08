package be.kdg.airfreight.flightoperations.api.flight;

import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.DelayDuration;
import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;
import be.kdg.airfreight.flightoperations.domain.flight.DelayReason;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flight.FlightStatus;
import be.kdg.airfreight.flightoperations.domain.flight.ScheduledTimes;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FlightDtoTest {
    private static final Instant STD = Instant.parse("2026-01-09T06:30:00Z");
    private static final Instant STA = Instant.parse("2026-01-09T09:45:00Z");
    private static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");

    private final AirportCatalog airports = code ->
        code.value().equals("BRU") ? Optional.of("Brussels Airport") : Optional.empty();
    private final DelayCodeCatalog delayCodes =
        () -> List.of(new DelayReason(new DelayCode("93"), "Aircraft rotation"));
    private final Flight flight = Flight.plan(new FlightNumber("HJ221"), new AirportCode("BRU"),
        new AirportCode("ATH"), new AircraftRegistration("SXBAA"), new ScheduledTimes(STD, STA));

    @Test
    void aFlightWithoutDelaysShowsItsScheduleAndAirports() {
        FlightDto dto = FlightDto.from(flight, airports, delayCodes);

        assertThat(dto.flightNumber()).isEqualTo("HJ221");
        assertThat(dto.scheduledDepartureDate()).isEqualTo(LocalDate.of(2026, 1, 9));
        assertThat(dto.departureAirport()).isEqualTo(new AirportDto("BRU", "Brussels Airport"));
        assertThat(dto.arrivalAirport()).isEqualTo(new AirportDto("ATH", null));
        assertThat(dto.status()).isEqualTo(FlightStatus.PLANNED);
        assertThat(dto.std()).isEqualTo(STD);
        assertThat(dto.etd()).isEqualTo(STD);
        assertThat(dto.eta()).isEqualTo(STA);
        assertThat(dto.atd()).isNull();
        assertThat(dto.delays()).isEmpty();
    }

    @Test
    void aDelayShowsItsKindMinutesReasonAndDescription() {
        flight.addDelay(DelayKind.DEPARTURE, new DelayDuration(25), new DelayCode("93"), "Late inbound aircraft",
            FlightPlannerId.create(), NOW, delayCodes);

        FlightDto dto = FlightDto.from(flight, airports, delayCodes);

        assertThat(dto.etd()).isEqualTo(Instant.parse("2026-01-09T06:55:00Z"));
        assertThat(dto.eta()).isEqualTo(Instant.parse("2026-01-09T10:10:00Z"));
        assertThat(dto.delays()).singleElement().satisfies(delay -> {
            assertThat(delay.kind()).isEqualTo(DelayKind.DEPARTURE);
            assertThat(delay.minutes()).isEqualTo(25);
            assertThat(delay.reason()).isEqualTo(new DelayCodeDto("93", "Aircraft rotation"));
            assertThat(delay.description()).isEqualTo("Late inbound aircraft");
        });
    }
}
