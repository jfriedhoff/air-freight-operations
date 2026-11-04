package be.kdg.airfreight.loadplanning.acceptance.integration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.TestHelper;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
import ch.qos.logback.classic.Level;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.persistence.EntityManager;
import org.springframework.test.web.servlet.MockMvc;

import static be.kdg.airfreight.loadplanning.acceptance.integration.Logins.token;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Story 38 in Load Planning: Messaging passes on that Flight Operations removed a flight, and Load Planning
 * deletes its cargo. The cargos are stored through TestHelper, because the story is not about how they were
 * built; the removal arrives through RabbitMQ and MessagingEventListener, as in production. The checks read
 * the REST API and the ulds table: the point is that the rows are gone, not only that the domain says so.
 * Cucumber makes a new instance for every scenario, so the fields are the state of one scenario.
 */
public class FlightRemovalSteps {
    private static final LocalTime STD = LocalTime.of(6, 30);
    private static final AirportCode BRU = new AirportCode("BRU");

    private final TestHelper testHelper;
    private final MessagingEvents messaging;
    private final MessageWorld world;
    private final MockMvc mockMvc;
    private final EntityManager entityManager;
    private final LogCapture logs;

    /** Each cargo of the scenario as it was stored, to compare with after the removal. */
    private final Map<FlightId, FlightCargo> stored = new HashMap<>();

    public FlightRemovalSteps(TestHelper testHelper, MessagingEvents messaging, MessageWorld world, MockMvc mockMvc,
                              EntityManager entityManager, LogCapture logs) {
        this.testHelper = testHelper;
        this.messaging = messaging;
        this.world = world;
        this.mockMvc = mockMvc;
        this.entityManager = entityManager;
        this.logs = logs;
    }

    /** The case asks that the bookings without a load position go too: the second ULD is one of those. */
    @Given("flight {word} of {date} has a finalized load with {uld} on {word} and {uld} booked afterwards without a load position")
    public void flightHasAFinalizedLoad(String flightNumber, LocalDate date, UldCode placed, String position,
                                        UldCode unplaced) {
        FlightCargoId id = testHelper.saveCargo(flightNumber, date, STD).getId();
        testHelper.book(id, placed.value(), "1250", WeightUnit.KG, "ATH");
        testHelper.place(id, placed.value(), position);
        testHelper.finalizeLoad(id);
        testHelper.book(id, unplaced.value(), "1000", WeightUnit.KG, "ATH");
        FlightCargo cargo = testHelper.find(id);
        assertThat(cargo.isFinalized()).isTrue();
        assertThat(cargo.getUlds()).hasSize(2);
        remember(cargo);
    }

    @Given("flight {word} of {date} has {uld} booked")
    public void flightHasABooking(String flightNumber, LocalDate date, UldCode code) {
        FlightCargoId id = testHelper.saveCargo(flightNumber, date, STD).getId();
        testHelper.book(id, code.value(), "1250", WeightUnit.KG, "ATH");
        remember(testHelper.find(id));
    }

    @When("Messaging reports that {word} of {date} has been removed")
    public void messagingReportsThatTheFlightHasBeenRemoved(String flightNumber, LocalDate date) {
        world.setMessageId(messaging.flightRemoved(flight(flightNumber, date)));
        messaging.waitUntilHandled();
    }

    /** The message id goes into MessageWorld, so "the same event arrives again" sends this removal again. */
    @Given("Load Planning has already processed the removal of {word} of {date}")
    public void loadPlanningHasAlreadyProcessedTheRemoval(String flightNumber, LocalDate date) throws Exception {
        messagingReportsThatTheFlightHasBeenRemoved(flightNumber, date);
        loadPlanningNoLongerHasACargoFor(flightNumber, date);
    }

    /** Both ways a planner reaches a cargo: by its id (the cargo page) and in the flights to load of its day. */
    @Then("Load Planning no longer has a cargo for {word} of {date}")
    public void loadPlanningNoLongerHasACargoFor(String flightNumber, LocalDate date) throws Exception {
        UUID planner = testHelper.saveLoadPlanner("Test load planner").id();
        mockMvc.perform(get("/api/flight-cargos/{id}", stored.get(flight(flightNumber, date)).getId().id())
                .with(token(planner, "load-planner")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("not-found"));
        mockMvc.perform(get("/api/flight-cargos")
                .param("scheduledDepartureDate", date.toString())
                .param("includeDeparted", "true")
                .with(token(planner, "load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights[?(@.flightNumber == '%s')]", flightNumber).isEmpty());
    }

    /** Counted in the table itself: a ULD row left behind would not show in the API once its cargo is gone. */
    @Then("no ULD of {word} of {date} is left, also not the one without a load position")
    public void noUldIsLeft(String flightNumber, LocalDate date) {
        Long left = entityManager
            .createQuery("select count(u) from JpaUld u where u.id.flightCargoId = :cargo", Long.class)
            .setParameter("cargo", stored.get(flight(flightNumber, date)).getId().id())
            .getSingleResult();
        assertThat(left).isZero();
    }

    /** Field by field, the version too: a cargo that was saved again would have a higher version. */
    @Then("the cargo of {word} of {date} is unchanged")
    public void theCargoIsUnchanged(String flightNumber, LocalDate date) {
        FlightCargo before = stored.get(flight(flightNumber, date));
        assertThat(testHelper.find(before.getId())).usingRecursiveComparison().isEqualTo(before);
    }

    @Then("it is logged that {word} of {date} had no cargo to remove")
    public void itIsLoggedThatThereWasNoCargoToRemove(String flightNumber, LocalDate date) {
        logs.assertLogged(Level.INFO, "No cargo to remove for flight " + flightNumber + " of " + date);
    }

    private void remember(FlightCargo cargo) {
        stored.put(cargo.getFlightId(), cargo);
    }

    private static FlightId flight(String flightNumber, LocalDate date) {
        return new FlightId(new FlightNumber(flightNumber), date, BRU);
    }
}
