package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.util.UUID;

import be.kdg.airfreight.loadplanning.TestClock;
import be.kdg.airfreight.loadplanning.TestHelper;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
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

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HJ221 of 9 January 2026 with AKE16689R7 on CL and AKE20417HJ without a load position, at 05:00 UTC. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClock.class)
class FlightCargoControllerTest {
    private static final String CARGO = "/api/flight-cargos/{id}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestHelper testHelper;
    @Autowired
    private TestClock clock;

    private FlightCargoId id;

    @BeforeEach
    void theCargoOfHj221() {
        clock.set(TestHelper.IN_THE_LOADING_PERIOD);
        id = testHelper.saveCargo().getId();
        testHelper.book(id, "AKE16689R7", "1250", WeightUnit.KG, "ATH");
        testHelper.book(id, "AKE20417HJ", "2646", WeightUnit.LB, "FRA");
        testHelper.place(id, "AKE16689R7", "CL");
    }

    @AfterEach
    void cleanUp() {
        testHelper.cleanUp();
        clock.reset();
    }

    @Test
    void aPlannerSeesTheFlightAndEveryBookedUldWithAndWithoutALoadPosition() throws Exception {
        mockMvc.perform(get(CARGO, id.id()).with(planner("flight-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.id().toString()))
            .andExpect(jsonPath("$.flightNumber").value("HJ221"))
            .andExpect(jsonPath("$.scheduledDepartureDate").value("2026-01-09"))
            .andExpect(jsonPath("$.departureAirport").value("BRU"))
            .andExpect(jsonPath("$.arrivalAirport").value("ATH"))
            .andExpect(jsonPath("$.aircraftType").value("B747-400F"))
            .andExpect(jsonPath("$.std").value("2026-01-09T06:30:00Z"))
            .andExpect(jsonPath("$.sta").value("2026-01-09T09:45:00Z"))
            .andExpect(jsonPath("$.etd").value("2026-01-09T06:30:00Z"))
            .andExpect(jsonPath("$.eta").value("2026-01-09T09:45:00Z"))
            .andExpect(jsonPath("$.departed").value(false))
            .andExpect(jsonPath("$.finalized").value(false))
            .andExpect(jsonPath("$.loadingPeriod.start").value("2026-01-08T18:30:00Z"))
            .andExpect(jsonPath("$.loadingPeriod.end").value("2026-01-09T06:10:00Z"))
            .andExpect(jsonPath("$.loadCanChange").value(true))
            .andExpect(jsonPath("$.ulds.length()").value(2))
            .andExpect(jsonPath("$.ulds[0].code").value("AKE16689R7"))
            .andExpect(jsonPath("$.ulds[0].weight.value").value(1250.0))
            .andExpect(jsonPath("$.ulds[0].weight.unit").value("KG"))
            .andExpect(jsonPath("$.ulds[0].destination").value("ATH"))
            .andExpect(jsonPath("$.ulds[0].loadPosition").value("CL"))
            .andExpect(jsonPath("$.ulds[1].code").value("AKE20417HJ"))
            .andExpect(jsonPath("$.ulds[1].weight.value").value(2646.0))
            .andExpect(jsonPath("$.ulds[1].weight.unit").value("LB"))
            .andExpect(jsonPath("$.ulds[1].destination").value("FRA"))
            .andExpect(jsonPath("$.ulds[1].loadPosition").doesNotExist());
    }

    @Test
    void theSelectionListHoldsExactlyThe34LoadPositionsOfTheMainDeck() throws Exception {
        mockMvc.perform(get(CARGO, id.id()).with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loadPositions.length()").value(34))
            .andExpect(jsonPath("$.loadPositions[0]").value("A1"))
            .andExpect(jsonPath("$.loadPositions[3]").value("CL"))
            .andExpect(jsonPath("$.loadPositions[4]").value("CR"))
            .andExpect(jsonPath("$.loadPositions[33]").value("X"));
    }

    @Test
    void aFinalizedLoadIsShownAsFinalizedAndNoLongerChangeable() throws Exception {
        testHelper.finalizeLoad(id);

        mockMvc.perform(get(CARGO, id.id()).with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.finalized").value(true))
            .andExpect(jsonPath("$.loadCanChange").value(false));
    }

    @Test
    void outsideTheLoadingPeriodTheLoadIsNotChangeable() throws Exception {
        clock.set(Instant.parse("2026-01-09T06:10:00Z"));

        mockMvc.perform(get(CARGO, id.id()).with(planner("load-planner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.finalized").value(false))
            .andExpect(jsonPath("$.loadCanChange").value(false));
    }

    @Test
    void aCargoThatDoesNotExistIsNotFound() throws Exception {
        mockMvc.perform(get(CARGO, UUID.randomUUID()).with(planner("load-planner")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    void anIdThatIsNotAUuidIsABadRequest() throws Exception {
        mockMvc.perform(get(CARGO, "abc").with(planner("load-planner")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid-input"));
    }

    /** A planner who only reads: the account does not matter. */
    private static RequestPostProcessor planner(String role) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
