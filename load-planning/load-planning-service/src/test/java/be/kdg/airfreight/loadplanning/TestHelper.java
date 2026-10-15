package be.kdg.airfreight.loadplanning;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AircraftType;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoRepository;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightInfo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlanCatalog;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPolicy;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlanner;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerRepository;
import be.kdg.airfreight.loadplanning.infrastructure.flightcargo.jpa.JpaFlightCargoRepository;
import be.kdg.airfreight.loadplanning.infrastructure.messaging.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Puts cargos and load planners in the test database and empties it again. */
@Component
public class TestHelper {
    public static final LocalDate DATE = LocalDate.of(2026, 1, 9);
    public static final Instant STD = Instant.parse("2026-01-09T06:30:00Z");
    public static final Instant STA = Instant.parse("2026-01-09T09:45:00Z");
    /** A moment inside the loading period of HJ221: it runs from 8 January 18:30 to 9 January 06:10 UTC. */
    public static final Instant IN_THE_LOADING_PERIOD = Instant.parse("2026-01-09T05:00:00Z");

    @Autowired
    private FlightCargoRepository cargos;
    @Autowired
    private LoadPlannerRepository loadPlanners;
    @Autowired
    private JpaFlightCargoRepository jpaCargos;
    @Autowired
    private OutboxEventRepository outbox;
    @Autowired
    private FloorPlanCatalog floorPlans;
    @Autowired
    private LoadingPolicy policy;
    @Autowired
    private AircraftType aircraftType;

    /** The cargo of HJ221 from BRU to ATH on 9 January 2026, STD 06:30 and STA 09:45 UTC, nothing booked. */
    public FlightCargo saveCargo() {
        return saveCargo("HJ221", DATE, LocalTime.of(6, 30));
    }

    /** A flight from BRU to ATH that takes 3 h 15 min, like the flights of the stories. */
    public FlightCargo saveCargo(String flightNumber, LocalDate date, LocalTime std) {
        Instant departure = date.atTime(std).toInstant(ZoneOffset.UTC);
        FlightCargo cargo = FlightCargo.create(
            new FlightId(new FlightNumber(flightNumber), date, new AirportCode("BRU")),
            FlightInfo.scheduled(new AirportCode("ATH"), aircraftType, departure,
                departure.plus(Duration.between(STD, STA))), policy);
        cargos.save(cargo);
        return find(cargo.getId());
    }

    public void book(FlightCargoId id, String uldCode, String weight, WeightUnit unit, String destination) {
        FlightCargo cargo = find(id);
        cargo.book(new UldCode(uldCode), new Weight(new BigDecimal(weight), unit), new AirportCode(destination),
            inTheLoadingPeriodOf(cargo));
        cargos.save(cargo);
    }

    /** Puts a booked ULD on a load position, by a new load planner, an hour and a half before the STD. */
    public void place(FlightCargoId id, String uldCode, String position) {
        FlightCargo cargo = find(id);
        FloorPlan floorPlan = floorPlans.floorPlanFor(cargo.getFlightInfo().aircraftType());
        cargo.assign(new UldCode(uldCode), floorPlan.position(position), saveLoadPlanner("Test load planner"),
            inTheLoadingPeriodOf(cargo), floorPlan, policy);
        cargos.save(cargo);
    }

    /** Finalizes the load as it is. No load check runs here: the checks have their own tests. */
    public void finalizeLoad(FlightCargoId id) {
        FlightCargo cargo = find(id);
        FloorPlan floorPlan = floorPlans.floorPlanFor(cargo.getFlightInfo().aircraftType());
        cargo.finalizeLoad(saveLoadPlanner("Test load planner"), inTheLoadingPeriodOf(cargo), List.of(), floorPlan,
            policy);
        cargos.save(cargo);
    }

    /** As if the flight event "departed" was processed. */
    public void depart(FlightCargoId id) {
        FlightCargo cargo = find(id);
        cargo.updateFlightInfo(cargo.getFlightInfo().asDeparted());
        cargos.save(cargo);
    }

    public LoadPlannerId saveLoadPlanner(String name) {
        LoadPlannerId id = LoadPlannerId.create();
        loadPlanners.save(new LoadPlanner(id, name));
        return id;
    }

    public Optional<String> loadPlannerName(LoadPlannerId id) {
        return loadPlanners.findById(id).map(LoadPlanner::getName);
    }

    public FlightCargo find(FlightCargoId id) {
        return cargos.findById(id).orElseThrow();
    }

    public long outboxCount() {
        return outbox.count();
    }

    /** Load planners stay: their ids are random, so one test never finds the planner of another. */
    public void cleanUp() {
        outbox.deleteAll();
        jpaCargos.deleteAll();
    }

    private static Instant inTheLoadingPeriodOf(FlightCargo cargo) {
        return cargo.getFlightInfo().std().minus(Duration.ofMinutes(90));
    }
}
