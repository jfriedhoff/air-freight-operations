package be.kdg.airfreight.flightoperations.api.flight;

import be.kdg.airfreight.flightoperations.TestClock;
import be.kdg.airfreight.flightoperations.TestHelper;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClock.class)
class FlightControllerTest {
    private static final String VALID_DELAY = """
        {"kind": "DEPARTURE", "minutes": 25, "reason": "93", "description": "Late inbound aircraft"}""";
    private static final String FLIGHTS = "/api/flights";
    private static final LocalDate NINE_JANUARY = LocalDate.of(2026, 1, 9);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestHelper testHelper;
    @Autowired
    private TestClock clock;

    @AfterEach
    void cleanUp() {
        testHelper.cleanUp();
        clock.reset();
    }

    @Test
    void aLoadPlannerSeesTheDetailsOfAFlightWithTheNamesOfItsAirports() throws Exception {
        Flight flight = testHelper.savePlannedFlight();

        mockMvc.perform(get("/api/flights/{id}", flight.getId().id()).with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(flight.getId().id().toString()))
            .andExpect(jsonPath("$.flightNumber").value("HJ221"))
            .andExpect(jsonPath("$.scheduledDepartureDate").value("2026-01-09"))
            .andExpect(jsonPath("$.departureAirport.code").value("BRU"))
            .andExpect(jsonPath("$.departureAirport.name").value("Brussels Airport"))
            .andExpect(jsonPath("$.arrivalAirport.name").value("Athens International Airport"))
            .andExpect(jsonPath("$.status").value("PLANNED"))
            .andExpect(jsonPath("$.std").value("2026-01-09T06:30:00Z"))
            .andExpect(jsonPath("$.sta").value("2026-01-09T09:45:00Z"))
            .andExpect(jsonPath("$.etd").value("2026-01-09T06:30:00Z"))
            .andExpect(jsonPath("$.atd").doesNotExist())
            .andExpect(jsonPath("$.delays.length()").value(0));
    }

    @Test
    void aFlightThatDoesNotExistIsNotFound() throws Exception {
        mockMvc.perform(get("/api/flights/{id}", UUID.randomUUID()).with(planner("flight-planner")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    void anIdThatIsNotAUuidIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/flights/abc").with(planner("flight-planner")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid-input"));
    }

    @Test
    void aFlightPlannerEntersADelayAndGetsTheFlightWithItsNewEstimates() throws Exception {
        Flight flight = testHelper.savePlannedFlight();

        mockMvc.perform(post("/api/flights/{id}/delays", flight.getId().id())
                .with(registeredFlightPlanner())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_DELAY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etd").value("2026-01-09T06:55:00Z"))
            .andExpect(jsonPath("$.eta").value("2026-01-09T10:10:00Z"))
            .andExpect(jsonPath("$.delays[0].kind").value("DEPARTURE"))
            .andExpect(jsonPath("$.delays[0].minutes").value(25))
            .andExpect(jsonPath("$.delays[0].reason.code").value("93"))
            .andExpect(jsonPath("$.delays[0].reason.description").value("Aircraft rotation"))
            .andExpect(jsonPath("$.delays[0].description").value("Late inbound aircraft"));
    }

    @Test
    void aDelayWithAMissingFieldIsABadRequestThatNamesTheField() throws Exception {
        Flight flight = testHelper.savePlannedFlight();

        mockMvc.perform(post("/api/flights/{id}/delays", flight.getId().id())
                .with(registeredFlightPlanner())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"kind": "DEPARTURE", "reason": "93", "description": ""}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid-input"))
            .andExpect(jsonPath("$.detail").value(containsString("minutes")))
            .andExpect(jsonPath("$.detail").value(containsString("description")));
        assertThat(testHelper.find(flight.getId()).getDelays()).isEmpty();
    }

    @Test
    void aDelayWithAnUnknownKindOrTextForMinutesIsABadRequest() throws Exception {
        Flight flight = testHelper.savePlannedFlight();

        mockMvc.perform(post("/api/flights/{id}/delays", flight.getId().id())
                .with(registeredFlightPlanner())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"kind": "TAXI", "minutes": 25, "reason": "93", "description": "Late"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid-input"));
        mockMvc.perform(post("/api/flights/{id}/delays", flight.getId().id())
                .with(registeredFlightPlanner())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"kind": "DEPARTURE", "minutes": "soon", "reason": "93", "description": "Late"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid-input"));
    }

    @Test
    void aDelayOfMoreThanTwelveHoursIsABadRequestWithTheReasonFromTheDomain() throws Exception {
        Flight flight = testHelper.savePlannedFlight();

        mockMvc.perform(post("/api/flights/{id}/delays", flight.getId().id())
                .with(registeredFlightPlanner())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"kind": "DEPARTURE", "minutes": 721, "reason": "93", "description": "Late"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("A delay lasts at least 1 minute and at most 12 hours"));
    }

    @Test
    void aTokenWhoseSubjectIsNotAUuidIsNotAFlightPlanner() throws Exception {
        Flight flight = testHelper.savePlannedFlight();

        mockMvc.perform(post("/api/flights/{id}/delays", flight.getId().id())
                .with(jwt().jwt(token -> token.subject("user"))
                    .authorities(new SimpleGrantedAuthority("ROLE_flight-planner")))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_DELAY))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("unknown-flight-planner"));
    }

    /** Keycloak decides who is a flight planner: a new colleague works at once, without a restart. */
    @Test
    void aFlightPlannerWhoIsNewInKeycloakIsRegisteredOnTheirFirstChange() throws Exception {
        Flight flight = testHelper.savePlannedFlight();
        UUID newPlanner = UUID.randomUUID();

        mockMvc.perform(post("/api/flights/{id}/delays", flight.getId().id())
                .with(jwt().jwt(token -> token.subject(newPlanner.toString()).claim("name", "Jan Janssens"))
                    .authorities(new SimpleGrantedAuthority("ROLE_flight-planner")))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_DELAY))
            .andExpect(status().isOk());

        assertThat(testHelper.flightPlannerName(new FlightPlannerId(newPlanner))).contains("Jan Janssens");
    }

    /** The details page shows the name Keycloak has now, not the one from the planner's first change. */
    @Test
    void aNameChangedInKeycloakIsStoredOnTheNextChange() throws Exception {
        Flight flight = testHelper.savePlannedFlight();
        FlightPlannerId fien = testHelper.saveFlightPlanner("Fien");

        mockMvc.perform(post("/api/flights/{id}/delays", flight.getId().id())
                .with(jwt().jwt(token -> token.subject(fien.id().toString()).claim("name", "Fien Peeters"))
                    .authorities(new SimpleGrantedAuthority("ROLE_flight-planner")))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_DELAY))
            .andExpect(status().isOk());

        assertThat(testHelper.flightPlannerName(fien)).contains("Fien Peeters");
    }

    @Test
    void theFlightsOfADayAreListedInOrderOfStdWithTheirIdentityAndScheduledTimes() throws Exception {
        testHelper.saveFlight("3V11", "BRU", NINE_JANUARY, LocalTime.of(12, 0), "SXBAB");
        Flight hj221 = testHelper.savePlannedFlight();
        testHelper.saveFlight("HJ300", "BRU", NINE_JANUARY.plusDays(1), LocalTime.of(6, 30), "SXBAC");

        mockMvc.perform(get(FLIGHTS).param("scheduledDepartureDate", "2026-01-09").with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scheduledDepartureDate").value("2026-01-09"))
            .andExpect(jsonPath("$.flights.length()").value(2))
            .andExpect(jsonPath("$.flights[0].id").value(hj221.getId().id().toString()))
            .andExpect(jsonPath("$.flights[0].flightNumber").value("HJ221"))
            .andExpect(jsonPath("$.flights[0].scheduledDepartureDate").value("2026-01-09"))
            .andExpect(jsonPath("$.flights[0].departureAirport").value("BRU"))
            .andExpect(jsonPath("$.flights[0].arrivalAirport").value("ATH"))
            .andExpect(jsonPath("$.flights[0].std").value("2026-01-09T06:30:00Z"))
            .andExpect(jsonPath("$.flights[0].sta").value("2026-01-09T09:45:00Z"))
            .andExpect(jsonPath("$.flights[1].flightNumber").value("3V11"));
    }

    /** At 23:30 UTC on 9 January it is already 10 January in Brussels; today is still 9 January (question 9). */
    @Test
    void todayIsTheUtcDateEvenWhenBrusselsIsAlreadyOnTheNextDay() throws Exception {
        clock.set(Instant.parse("2026-01-09T23:30:00Z"));
        Flight hj221 = testHelper.savePlannedFlight();
        testHelper.saveFlight("HJ300", "BRU", NINE_JANUARY.plusDays(1), LocalTime.of(6, 30), "SXBAC");

        mockMvc.perform(get(FLIGHTS).with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scheduledDepartureDate").value("2026-01-09"))
            .andExpect(jsonPath("$.flights.length()").value(1))
            .andExpect(jsonPath("$.flights[0].id").value(hj221.getId().id().toString()));
    }

    @Test
    void flightsWithTheSameStdAreListedByFlightNumber() throws Exception {
        testHelper.saveFlight("HJ500", "BRU", NINE_JANUARY, LocalTime.of(9, 0), "SXBAB");
        testHelper.saveFlight("3V20", "BRU", NINE_JANUARY, LocalTime.of(9, 0), "SXBAC");

        mockMvc.perform(get(FLIGHTS).param("scheduledDepartureDate", "2026-01-09").with(planner("flight-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights[0].flightNumber").value("3V20"))
            .andExpect(jsonPath("$.flights[1].flightNumber").value("HJ500"));
    }

    /** The day list loads every flight with its delays; a flight with two delays is still one flight. */
    @Test
    void aFlightWithSeveralDelaysIsListedOnce() throws Exception {
        Flight flight = testHelper.savePlannedFlight();
        testHelper.addDepartureDelay(flight.getId());
        testHelper.addDepartureDelay(flight.getId());

        mockMvc.perform(get(FLIGHTS).param("scheduledDepartureDate", "2026-01-09").with(planner("flight-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights.length()").value(1));
    }

    @Test
    void aDayWithoutFlightsGivesAnEmptyList() throws Exception {
        mockMvc.perform(get(FLIGHTS).param("scheduledDepartureDate", "2026-02-01").with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scheduledDepartureDate").value("2026-02-01"))
            .andExpect(jsonPath("$.flights.length()").value(0));
    }

    @Test
    void aFlightIsFoundByItsIdentityAndNotByTheSameNumberFromAnotherAirport() throws Exception {
        Flight hj221 = testHelper.savePlannedFlight();
        testHelper.saveFlight("HJ221", "ATH", NINE_JANUARY, LocalTime.of(12, 0), "SXBAB");

        mockMvc.perform(get(FLIGHTS).param("scheduledDepartureDate", "2026-01-09")
                .param("flightNumber", "HJ221").param("departureAirport", "BRU").with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flights.length()").value(1))
            .andExpect(jsonPath("$.flights[0].id").value(hj221.getId().id().toString()))
            .andExpect(jsonPath("$.flights[0].departureAirport").value("BRU"));
    }

    @Test
    void anIdentityOnAnotherDayFindsNothing() throws Exception {
        testHelper.savePlannedFlight();

        mockMvc.perform(get(FLIGHTS).param("scheduledDepartureDate", "2026-01-10")
                .param("flightNumber", "HJ221").param("departureAirport", "BRU").with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scheduledDepartureDate").value("2026-01-10"))
            .andExpect(jsonPath("$.flights.length()").value(0));
    }

    @Test
    void halfAnIdentityIsABadRequestWithItsReason() throws Exception {
        mockMvc.perform(get(FLIGHTS).param("scheduledDepartureDate", "2026-01-09").param("flightNumber", "HJ221")
                .with(planner("load-planner")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid-input"))
            .andExpect(jsonPath("$.detail").value(containsString("both or neither")));
    }

    @Test
    void aMalformedDateFlightNumberOrAirportIsABadRequest() throws Exception {
        mockMvc.perform(get(FLIGHTS).param("scheduledDepartureDate", "tomorrow").with(planner("load-planner")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid-input"));
        mockMvc.perform(get(FLIGHTS).param("scheduledDepartureDate", "2026-01-09")
                .param("flightNumber", "hj221").param("departureAirport", "bru").with(planner("load-planner")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid-input"))
            .andExpect(jsonPath("$.detail").value(containsString("hj221")));
    }

    @Test
    void withoutATokenTheListIsNotGiven() throws Exception {
        mockMvc.perform(get(FLIGHTS)).andExpect(status().isUnauthorized());
    }

    @Test
    void aUserWithNeitherOrBothPlannerRolesGetsNoList() throws Exception {
        mockMvc.perform(get(FLIGHTS).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_other"))))
            .andExpect(status().isForbidden());
        mockMvc.perform(get(FLIGHTS).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_flight-planner"),
                new SimpleGrantedAuthority("ROLE_load-planner"))))
            .andExpect(status().isForbidden());
    }

    private RequestPostProcessor registeredFlightPlanner() {
        String subject = testHelper.saveFlightPlanner().id().toString();
        return jwt().jwt(token -> token.subject(subject))
            .authorities(new SimpleGrantedAuthority("ROLE_flight-planner"));
    }

    private static RequestPostProcessor planner(String role) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
