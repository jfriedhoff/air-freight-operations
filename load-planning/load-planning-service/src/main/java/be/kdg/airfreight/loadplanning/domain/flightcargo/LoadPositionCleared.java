package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.Instant;

import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record LoadPositionCleared(FlightCargoId flightCargoId, FlightId flightId, UldCode uldCode,
                                  LoadPosition position, LoadPlannerId clearedBy,
                                  Instant occurredOn) implements FlightCargoEvent {
}
