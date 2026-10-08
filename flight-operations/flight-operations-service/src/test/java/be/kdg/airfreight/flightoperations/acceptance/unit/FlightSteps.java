package be.kdg.airfreight.flightoperations.acceptance.unit;

import io.cucumber.java.en.Given;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.STD;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.onDate;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.onDepartureDate;

/** The flight before the When step: planned, departed or landed, and the moment of the scenario. */
public class FlightSteps {
    /** A scenario that names no STA: the flight takes three hours. */
    private static final Duration FLIGHT_TIME = Duration.ofHours(3);

    private final FlightWorld world;

    public FlightSteps(FlightWorld world) {
        this.world = world;
    }

    @Given("flight {word} is planned")
    public void flightIsPlanned(String flightNumber) {
        world.plan(flightNumber);
    }

    @Given("flight {word} of {date} is planned with STD {time} and STA {time} UTC")
    public void flightIsPlannedWithStdAndSta(String flightNumber, LocalDate date, LocalTime std, LocalTime sta) {
        world.plan(flightNumber, onDate(date, std), onDate(date, sta));
    }

    @Given("flight {word} of {date} is planned with STD {time} UTC")
    public void flightIsPlannedWithStd(String flightNumber, LocalDate date, LocalTime std) {
        planDepartingAt(flightNumber, onDate(date, std));
    }

    /** Without delays the ETD is the STD. */
    @Given("flight {word} is planned with ETD {time} UTC")
    public void flightIsPlannedWithEtd(String flightNumber, LocalTime etd) {
        planDepartingAt(flightNumber, onDepartureDate(etd));
    }

    /** Without delays the ETD and ETA are the STD and STA. */
    @Given("flight {word} with ETD {time} and ETA {time} UTC")
    public void flightWithEtdAndEta(String flightNumber, LocalTime etd, LocalTime eta) {
        world.plan(flightNumber, onDepartureDate(etd), onDepartureDate(eta));
    }

    @Given("flight {word} has departed")
    public void flightHasDeparted(String flightNumber) {
        departAtTheStd(flightNumber);
    }

    /** Lands at its STA. */
    @Given("flight {word} has landed")
    public void flightHasLanded(String flightNumber) {
        departAtTheStd(flightNumber);
        world.land(flightNumber, world.flight(flightNumber).getScheduled().arrival());
    }

    @Given("flight {word} has departed with ATD {time} UTC and has ETA {time} UTC")
    public void flightHasDepartedWithAtdAndEta(String flightNumber, LocalTime atd, LocalTime eta) {
        departedOnTime(flightNumber, onDepartureDate(atd), onDepartureDate(eta));
    }

    @Given("flight {word} has departed with ETA {time} UTC")
    public void flightHasDepartedWithEta(String flightNumber, LocalTime eta) {
        departedOnTime(flightNumber, onDepartureDate(STD), onDepartureDate(eta));
    }

    @Given("it is now {time} UTC")
    public void itIsNow(LocalTime time) {
        world.now = onDepartureDate(time);
    }

    /** Not every feature plans the flight in its Background: then it gets the default schedule. */
    private void departAtTheStd(String flightNumber) {
        if (world.flight(flightNumber) == null) {
            world.plan(flightNumber);
        }
        world.depart(flightNumber, world.flight(flightNumber).getScheduled().departure());
    }

    private void planDepartingAt(String flightNumber, Instant std) {
        world.plan(flightNumber, std, std.plus(FLIGHT_TIME));
    }

    /** Without delays: the ATD is the STD and the ETA is the STA. */
    private void departedOnTime(String flightNumber, Instant atd, Instant eta) {
        world.plan(flightNumber, atd, eta);
        world.depart(flightNumber, atd);
    }
}
