package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.Instant;
import java.util.List;

import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record LoadFinalized(FlightCargoId flightCargoId, FlightId flightId, List<LoadedUld> load,
                            LoadPlannerId finalizedBy, Instant occurredOn) implements FlightCargoEvent {
    public LoadFinalized {
        load = List.copyOf(load);
    }
}
