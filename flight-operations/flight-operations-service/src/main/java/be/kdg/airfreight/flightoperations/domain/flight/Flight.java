package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import be.kdg.airfreight.flightoperations.domain.DomainRuleViolationException;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.Nullable;

@AggregateRoot
public class Flight {
    @Identity
    private final FlightId id;
    private final FlightNumber flightNumber;
    private final AirportCode departureAirport;
    private final AirportCode arrivalAirport;
    private final AircraftRegistration registration;
    private final ScheduledTimes scheduled;
    private final List<Delay> delays = new ArrayList<>();
    private @Nullable Instant actualDeparture;
    private @Nullable Instant actualArrival;
    private @Nullable FlightPlannerId departureRegisteredBy;
    private @Nullable FlightPlannerId arrivalRegisteredBy;
    private boolean finalizedLoadReceived;
    private final @Nullable Long version;

    private Flight(FlightId id, FlightNumber flightNumber, AirportCode departureAirport, AirportCode arrivalAirport,
                   AircraftRegistration registration, ScheduledTimes scheduled, @Nullable Long version) {
        this.id = Objects.requireNonNull(id, "Id cannot be null");
        this.flightNumber = Objects.requireNonNull(flightNumber, "Flight number cannot be null");
        this.departureAirport = Objects.requireNonNull(departureAirport, "Departure airport cannot be null");
        this.arrivalAirport = Objects.requireNonNull(arrivalAirport, "Arrival airport cannot be null");
        this.registration = Objects.requireNonNull(registration, "Registration cannot be null");
        this.scheduled = Objects.requireNonNull(scheduled, "Scheduled times cannot be null");
        this.version = version;
    }

    public static Flight plan(FlightNumber flightNumber, AirportCode departureAirport, AirportCode arrivalAirport,
                              AircraftRegistration registration, ScheduledTimes scheduled) {
        return new Flight(FlightId.create(), flightNumber, departureAirport, arrivalAirport, registration,
            scheduled, null);
    }

    public static Flight hydrate(FlightId id, FlightNumber flightNumber, AirportCode departureAirport,
                                 AirportCode arrivalAirport, AircraftRegistration registration,
                                 ScheduledTimes scheduled, List<Delay> delays,
                                 @Nullable Instant actualDeparture, @Nullable Instant actualArrival,
                                 @Nullable FlightPlannerId departureRegisteredBy,
                                 @Nullable FlightPlannerId arrivalRegisteredBy,
                                 boolean finalizedLoadReceived, @Nullable Long version) {
        Flight flight = new Flight(id, flightNumber, departureAirport, arrivalAirport, registration, scheduled,
            version);
        flight.delays.addAll(delays);
        flight.actualDeparture = actualDeparture;
        flight.actualArrival = actualArrival;
        flight.departureRegisteredBy = departureRegisteredBy;
        flight.arrivalRegisteredBy = arrivalRegisteredBy;
        flight.finalizedLoadReceived = finalizedLoadReceived;
        return flight;
    }

    public FlightAnnounced announced(Instant now) {
        return new FlightAnnounced(id, flightNumber, scheduled.departureDate(), departureAirport, arrivalAirport,
            registration, scheduled.departure(), scheduled.arrival(), now);
    }

    public FlightRemoved removed(Instant now) {
        return new FlightRemoved(id, flightNumber, scheduled.departureDate(), departureAirport, now);
    }

    public List<FlightEvent> addDelay(DelayKind kind, DelayDuration minutes, DelayCode reason, String description,
                                      FlightPlannerId by, Instant now) {
        requireDelaysCanChange(kind);
        requireTotalDelayWithinLimit(totalDelayMinutes() + minutes.minutes());
        delays.add(new Delay(DelayId.create(), kind, minutes, reason, description, by));
        return estimateEvents(kind, by, now);
    }

    public List<FlightEvent> changeDelay(DelayId delayId, DelayDuration minutes, String description,
                                         FlightPlannerId by, Instant now) {
        Delay delay = delayWith(delayId);
        requireDelaysCanChange(delay.getKind());
        requireTotalDelayWithinLimit(totalDelayMinutes() - delay.getMinutes().minutes() + minutes.minutes());
        boolean estimateChanges = !delay.getMinutes().equals(minutes);
        delay.change(minutes, description, by);
        return estimateChanges ? estimateEvents(delay.getKind(), by, now) : List.of();
    }

    public List<FlightEvent> removeDelay(DelayId delayId, FlightPlannerId by, Instant now) {
        Delay delay = delayWith(delayId);
        requireDelaysCanChange(delay.getKind());
        delays.remove(delay);
        return estimateEvents(delay.getKind(), by, now);
    }

    public FinalizedLoadReceived recordFinalizedLoad(Instant now) {
        if (getStatus() != FlightStatus.PLANNED) {
            throw new DomainRuleViolationException(
                "The finalized load can only be received while the flight is planned");
        }
        if (finalizedLoadReceived) {
            throw new DomainRuleViolationException("The finalized load has already been received for this flight");
        }
        finalizedLoadReceived = true;
        return new FinalizedLoadReceived(id, flightNumber, scheduled.departureDate(), departureAirport, now);
    }

    public FlightDeparted registerDeparture(Instant atd, FlightPlannerId by, Instant now,
                                            ActualTimeTolerance tolerance) {
        if (getStatus() != FlightStatus.PLANNED) {
            throw new DomainRuleViolationException("The departure of this flight has already been registered");
        }
        if (!finalizedLoadReceived) {
            throw new DomainRuleViolationException(
                "The departure can only be registered once the load is finalized (CPM received)");
        }
        if (atd.isAfter(now)) {
            throw new DomainRuleViolationException("The ATD cannot be in the future");
        }
        if (!tolerance.allows(atd, getEstimated().departure())) {
            throw new DomainRuleViolationException("The ATD is more than " + tolerance.value().toMinutes()
                + " minutes after the ETD: enter a departure delay first");
        }
        actualDeparture = atd;
        departureRegisteredBy = Objects.requireNonNull(by, "Flight planner cannot be null");
        return new FlightDeparted(id, flightNumber, scheduled.departureDate(), departureAirport, registration,
            atd, by, now);
    }

    public FlightLanded registerArrival(Instant ata, FlightPlannerId by, Instant now,
                                        ActualTimeTolerance tolerance) {
        FlightStatus status = getStatus();
        if (status == FlightStatus.PLANNED) {
            throw new DomainRuleViolationException(
                "The arrival can only be registered once the flight has departed");
        }
        if (status == FlightStatus.LANDED) {
            throw new DomainRuleViolationException("The arrival of this flight has already been registered");
        }
        if (ata.isAfter(now)) {
            throw new DomainRuleViolationException("The ATA cannot be in the future");
        }
        Instant atd = Objects.requireNonNull(actualDeparture, "A departed flight has an ATD");
        if (!ata.isAfter(atd)) {
            throw new DomainRuleViolationException("The ATA must be later than the ATD");
        }
        if (!tolerance.allows(ata, getEstimated().arrival())) {
            throw new DomainRuleViolationException("The ATA is more than " + tolerance.value().toMinutes()
                + " minutes after the ETA: enter an arrival delay first");
        }
        actualArrival = ata;
        arrivalRegisteredBy = Objects.requireNonNull(by, "Flight planner cannot be null");
        return new FlightLanded(id, flightNumber, scheduled.departureDate(), departureAirport, arrivalAirport,
            registration, ata, by, now);
    }

    public EstimatedTimes getEstimated() {
        Duration departureDelay = totalDelay(DelayKind.DEPARTURE);
        Duration arrivalDelay = totalDelay(DelayKind.ARRIVAL);
        return new EstimatedTimes(
            scheduled.departure().plus(departureDelay),
            scheduled.arrival().plus(departureDelay).plus(arrivalDelay));
    }

    public FlightStatus getStatus() {
        if (actualArrival != null) {
            return FlightStatus.LANDED;
        }
        if (actualDeparture != null) {
            return FlightStatus.DEPARTED;
        }
        return FlightStatus.PLANNED;
    }

    public FlightId getId() {
        return id;
    }

    public FlightNumber getFlightNumber() {
        return flightNumber;
    }

    public AirportCode getDepartureAirport() {
        return departureAirport;
    }

    public AirportCode getArrivalAirport() {
        return arrivalAirport;
    }

    public AircraftRegistration getRegistration() {
        return registration;
    }

    public ScheduledTimes getScheduled() {
        return scheduled;
    }

    public List<Delay> getDelays() {
        return Collections.unmodifiableList(delays);
    }

    public @Nullable Instant getActualDeparture() {
        return actualDeparture;
    }

    public @Nullable Instant getActualArrival() {
        return actualArrival;
    }

    public @Nullable FlightPlannerId getDepartureRegisteredBy() {
        return departureRegisteredBy;
    }

    public @Nullable FlightPlannerId getArrivalRegisteredBy() {
        return arrivalRegisteredBy;
    }

    public boolean isFinalizedLoadReceived() {
        return finalizedLoadReceived;
    }

    public @Nullable Long getVersion() {
        return version;
    }

    private void requireDelaysCanChange(DelayKind kind) {
        FlightStatus status = getStatus();
        if (kind == DelayKind.DEPARTURE && status != FlightStatus.PLANNED) {
            throw new DomainRuleViolationException(
                "A departure delay can only change while the flight is planned");
        }
        if (kind == DelayKind.ARRIVAL && status == FlightStatus.LANDED) {
            throw new DomainRuleViolationException(
                "An arrival delay can no longer change once the flight has landed");
        }
    }

    private static void requireTotalDelayWithinLimit(int totalMinutes) {
        if (totalMinutes > DelayDuration.MAX_MINUTES) {
            throw new DomainRuleViolationException(
                "All delays of a flight together can be at most 12 hours");
        }
    }

    private Delay delayWith(DelayId delayId) {
        return delays.stream()
            .filter(delay -> delay.getId().equals(delayId))
            .findFirst()
            .orElseThrow(delayId::notFound);
    }

    private int totalDelayMinutes() {
        return delays.stream()
            .mapToInt(delay -> delay.getMinutes().minutes())
            .sum();
    }

    private Duration totalDelay(DelayKind kind) {
        return Duration.ofMinutes(delays.stream()
            .filter(delay -> delay.getKind() == kind)
            .mapToInt(delay -> delay.getMinutes().minutes())
            .sum());
    }

    /** A departure delay shifts the ETD and the ETA; an arrival delay only the ETA. */
    private List<FlightEvent> estimateEvents(DelayKind kind, FlightPlannerId by, Instant now) {
        EstimatedTimes estimated = getEstimated();
        ArrivalEstimateChanged arrivalChanged = new ArrivalEstimateChanged(id, flightNumber,
            scheduled.departureDate(), departureAirport, arrivalAirport, registration, estimated.arrival(), by, now);
        if (kind == DelayKind.ARRIVAL) {
            return List.of(arrivalChanged);
        }
        DepartureEstimateChanged departureChanged = new DepartureEstimateChanged(id, flightNumber,
            scheduled.departureDate(), departureAirport, registration, estimated.departure(), by, now);
        return List.of(departureChanged, arrivalChanged);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return this == other || other instanceof Flight flight && id.equals(flight.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
