package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.TestClock;
import be.kdg.airfreight.loadplanning.TestHelper;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * It is 9 January 2026 10:00 UTC. HJ221 (STD 06:30) has departed, HJ225 (STD 08:00) has departed and landed,
 * 3V11 (STD 12:00) is still planned, and HJ300 flies on 10 January. Load Planning does not tell landed from
 * departed: for the load a landed flight is a departed one.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClock.class)
class FlightsToLoadControllerTest {
    private static final String FLIGHTS_TO_LOAD = "/api/flight-cargos";
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 9);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestHelper testHelper;
    @Autowired
    private TestClock clock;

    private FlightCargoId stillPlanned;

    @BeforeEach
    void theFlightsOf9And10January() {
        clock.set(Instant.parse("2026-01-09T10:00:00Z"));
        testHelper.depart(testHelper.saveCargo("HJ221", TODAY, LocalTime.of(6, 30)).getId());
        testHelper.depart(testHelper.saveCargo("HJ225", TODAY, LocalTime.of(8, 0)).getId());
        stillPlanned = testHelper.saveCargo("3V11", TODAY, LocalTime.of(12, 0)).getId();
        testHelper.saveCargo("HJ300", TODAY.plusDays(1), LocalTime.of(6, 30));
    }

    @AfterEach
    void cleanUp() {
        testHelper.cleanUp();
        clock.reset();
    }

    @Test
    void byDefaultTheFlightsOfTodayAreListedWithoutDepartedFlights() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scheduledDepartureDate").value("2026-01-09"))
            .andExpect(jsonPath("$.flights.length()").value(1))
            .andExpect(jsonPath("$.flights[0].id").value(stillPlanned.id().toString()))
            .andExpect(jsonPath("$.flights[0].flightNumber").value("3V11"))
            .andExpect(jsonPath("$.flights[0].scheduledDepartureDate").value("2026-01-09"))
            .andExpect(jsonPath("$.flights[0].departureAirport").value("BRU"))
            .andExpect(jsonPath("$.flights[0].arrivalAirport").value("ATH"))
            .andExpect(jsonPath("$.flights[0].std").value("2026-01-09T12:00:00Z"))
            .andExpect(jsonPath("$.flights[0].sta").value("2026-01-09T15:15:00Z"))
            .andExpect(jsonPath("$.flights[0].etd").value("2026-01-09T12:00:00Z"))
            .andExpect(jsonPath("$.flights[0].eta").value("2026-01-09T15:15:00Z"))
            .andExpect(jsonPath("$.flights[0].departed").value(false))
            .andExpect(jsonPath("$.flights[0].finalized").value(false));
    }

    @Test
    void withTheFilterOffDepartedFlightsAreListedToo() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("includeDeparted", "true").with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights.length()").value(3))
            .andExpect(jsonPath("$.flights[0].flightNumber").value("HJ221"))
            .andExpect(jsonPath("$.flights[0].departed").value(true))
            .andExpect(jsonPath("$.flights[1].flightNumber").value("HJ225"))
            .andExpect(jsonPath("$.flights[1].departed").value(true))
            .andExpect(jsonPath("$.flights[2].flightNumber").value("3V11"))
            .andExpect(jsonPath("$.flights[2].departed").value(false));
    }

    @Test
    void anotherDayShowsTheFlightsOfThatDay() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("scheduledDepartureDate", "2026-01-10")
                .with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scheduledDepartureDate").value("2026-01-10"))
            .andExpect(jsonPath("$.flights.length()").value(1))
            .andExpect(jsonPath("$.flights[0].flightNumber").value("HJ300"));
    }

    /** The id in the list is the id the cargo page opens; a flight without bookings has a cargo too. */
    @Test
    void everyListedFlightLeadsToItsCargo() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).with(planner("load-planner")))
            .andExpect(jsonPath("$.flights[0].id").value(stillPlanned.id().toString()));

        mockMvc.perform(get(FLIGHTS_TO_LOAD + "/{id}", stillPlanned.id()).with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flightNumber").value("3V11"))
            .andExpect(jsonPath("$.ulds.length()").value(0));
    }

    /** Provisional: the day of a flight is its scheduled departure date in UTC, and so is today. */
    @Test
    void todayIsTheScheduledDepartureDateInUtc() throws Exception {
        clock.set(Instant.parse("2026-01-09T23:30:00Z"));

        mockMvc.perform(get(FLIGHTS_TO_LOAD).with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scheduledDepartureDate").value("2026-01-09"))
            .andExpect(jsonPath("$.flights[0].flightNumber").value("3V11"));
    }

    @Test
    void aDayWithoutFlightsGivesAnEmptyList() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("scheduledDepartureDate", "2026-02-01")
                .with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scheduledDepartureDate").value("2026-02-01"))
            .andExpect(jsonPath("$.flights.length()").value(0));
    }

    @Test
    void aDateThatIsNotADateIsABadRequest() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("scheduledDepartureDate", "tomorrow")
                .with(planner("load-planner")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid-input"));
    }

    @Test
    void aFinalizedLoadIsMarkedInTheList() throws Exception {
        testHelper.finalizeLoad(stillPlanned);

        mockMvc.perform(get(FLIGHTS_TO_LOAD).with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights.length()").value(1))
            .andExpect(jsonPath("$.flights[0].flightNumber").value("3V11"))
            .andExpect(jsonPath("$.flights[0].finalized").value(true));
    }

    @Test
    void aFlightPlannerMayReadTheList() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).with(planner("flight-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights.length()").value(1));
    }

    @Test
    void aFlightIsFoundByItsIdentity() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("scheduledDepartureDate", "2026-01-09")
                .param("flightNumber", "3V11").param("departureAirport", "BRU").with(planner("flight-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights.length()").value(1))
            .andExpect(jsonPath("$.flights[0].id").value(stillPlanned.id().toString()));
    }

    @Test
    void aDepartedFlightIsFoundByItsIdentityOnlyWhenDepartedFlightsAreAskedFor() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("scheduledDepartureDate", "2026-01-09")
                .param("flightNumber", "HJ221").param("departureAirport", "BRU").with(planner("flight-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights.length()").value(0));
        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("scheduledDepartureDate", "2026-01-09")
                .param("flightNumber", "HJ221").param("departureAirport", "BRU").param("includeDeparted", "true")
                .with(planner("flight-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights.length()").value(1))
            .andExpect(jsonPath("$.flights[0].flightNumber").value("HJ221"))
            .andExpect(jsonPath("$.flights[0].departed").value(true));
    }

    /** Flight events reach Load Planning through Messaging: a flight just created may not be known here yet. */
    @Test
    void anIdentityThatLoadPlanningDoesNotKnowGivesAnEmptyList() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("scheduledDepartureDate", "2026-01-09")
                .param("flightNumber", "HJ999").param("departureAirport", "BRU").param("includeDeparted", "true")
                .with(planner("flight-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights.length()").value(0));
    }

    @Test
    void anIdentityFromAnotherAirportGivesAnEmptyList() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("scheduledDepartureDate", "2026-01-09")
                .param("flightNumber", "3V11").param("departureAirport", "ATH").param("includeDeparted", "true")
                .with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights.length()").value(0));
    }

    @Test
    void theSameFlightNumberFromAnotherAirportIsAnotherFlight() throws Exception {
        FlightCargoId fromAthens = testHelper.saveCargo("3V11", "ATH", TODAY, LocalTime.of(13, 0)).getId();

        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("scheduledDepartureDate", "2026-01-09")
                .param("flightNumber", "3V11").param("departureAirport", "BRU").with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights.length()").value(1))
            .andExpect(jsonPath("$.flights[0].id").value(stillPlanned.id().toString()));
        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("scheduledDepartureDate", "2026-01-09")
                .param("flightNumber", "3V11").param("departureAirport", "ATH").with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights.length()").value(1))
            .andExpect(jsonPath("$.flights[0].id").value(fromAthens.id().toString()))
            .andExpect(jsonPath("$.flights[0].departureAirport").value("ATH"));
    }

    @Test
    void anIdentityOnAnotherDayFindsNothing() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("scheduledDepartureDate", "2026-01-10")
                .param("flightNumber", "3V11").param("departureAirport", "BRU").param("includeDeparted", "true")
                .with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scheduledDepartureDate").value("2026-01-10"))
            .andExpect(jsonPath("$.flights.length()").value(0));
    }

    @Test
    void aLowerCaseIdentityIsABadRequestWithItsReason() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("scheduledDepartureDate", "2026-01-09")
                .param("flightNumber", "3v11").param("departureAirport", "bru").with(planner("load-planner")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid-input"))
            .andExpect(jsonPath("$.detail").value(containsString("3v11")));
    }

    @Test
    void halfAnIdentityIsABadRequestWithItsReason() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD).param("flightNumber", "3V11").with(planner("load-planner")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid-input"))
            .andExpect(jsonPath("$.detail").value(containsString("both or neither")));
    }

    @Test
    void withoutATokenTheListIsNotGiven() throws Exception {
        mockMvc.perform(get(FLIGHTS_TO_LOAD)).andExpect(status().isUnauthorized());
    }

    private static RequestPostProcessor planner(String role) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
