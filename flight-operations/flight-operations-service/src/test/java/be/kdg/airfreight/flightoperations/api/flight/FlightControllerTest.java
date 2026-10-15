package be.kdg.airfreight.flightoperations.api.flight;

import be.kdg.airfreight.flightoperations.TestHelper;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

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
class FlightControllerTest {
    private static final String VALID_DELAY = """
        {"kind": "DEPARTURE", "minutes": 25, "reason": "93", "description": "Late inbound aircraft"}""";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestHelper testHelper;

    @AfterEach
    void cleanUp() {
        testHelper.cleanUp();
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
