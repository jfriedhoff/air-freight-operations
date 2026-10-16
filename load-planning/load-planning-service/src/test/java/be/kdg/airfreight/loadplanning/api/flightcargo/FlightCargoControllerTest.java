package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.util.UUID;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import be.kdg.airfreight.loadplanning.TestClock;
import be.kdg.airfreight.loadplanning.TestHelper;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Uld;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;

import static org.hamcrest.Matchers.contains;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HJ221 of 9 January 2026 with AKE16689R7 on CL and AKE20417HJ without a load position, at 05:00 UTC. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClock.class)
@ExtendWith(OutputCaptureExtension.class)
class FlightCargoControllerTest {
    private static final String CARGO = "/api/flight-cargos/{id}";
    private static final String LOAD_POSITION = "/api/flight-cargos/{id}/ulds/{uldCode}/load-position";
    private static final String LOAD_CHECK = "/api/flight-cargos/{id}/load-check";
    private static final String FINALIZE = "/api/flight-cargos/{id}/finalize";
    private static final String LATERAL_BALANCE = "$.results[?(@.checkName == 'Lateral balance')]";
    private static final String REPORT_LATERAL_BALANCE = "$.report.results[?(@.checkName == 'Lateral balance')]";

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

    @Test
    void aLoadPlannerPlacesAUldAndGetsTheCargoWithItsNewPosition() throws Exception {
        LoadPlannerId loadPlanner = testHelper.saveLoadPlanner("Test load planner");

        mockMvc.perform(assign("AKE20417HJ", "{\"loadPosition\": \"CR\"}").with(token(loadPlanner.id().toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.id().toString()))
            .andExpect(jsonPath("$.ulds[1].code").value("AKE20417HJ"))
            .andExpect(jsonPath("$.ulds[1].loadPosition").value("CR"))
            .andExpect(jsonPath("$.loadCanChange").value(true));

        Uld stored = uld("AKE20417HJ");
        assertThat(stored.getPosition()).isNotNull();
        assertThat(stored.getPosition().name()).isEqualTo("CR");
        assertThat(stored.getPositionChangedBy()).isEqualTo(loadPlanner);
        assertThat(testHelper.outboxCount()).as("a load position sends nothing to partners").isZero();
    }

    @Test
    void thePlannerComesFromTheLoginNotFromTheRequest() throws Exception {
        LoadPlannerId loggedIn = testHelper.saveLoadPlanner("Bram");
        LoadPlannerId inTheRequest = testHelper.saveLoadPlanner("Someone else");

        mockMvc.perform(assign("AKE20417HJ",
                "{\"loadPosition\": \"CR\", \"planner\": \"" + inTheRequest.id() + "\"}")
                .with(token(loggedIn.id().toString())))
            .andExpect(status().isOk());

        assertThat(uld("AKE20417HJ").getPositionChangedBy()).isEqualTo(loggedIn);
    }

    /** Keycloak decides who is a load planner: a new colleague works at once, without a restart. */
    @Test
    void aLoadPlannerWhoIsNewInKeycloakIsRegisteredOnTheirFirstChange() throws Exception {
        UUID newPlanner = UUID.randomUUID();

        mockMvc.perform(assign("AKE20417HJ", "{\"loadPosition\": \"CR\"}")
                .with(jwt().jwt(token -> token.subject(newPlanner.toString()).claim("name", "Jan Janssens"))
                    .authorities(new SimpleGrantedAuthority("ROLE_load-planner"))))
            .andExpect(status().isOk());

        assertThat(uld("AKE20417HJ").getPositionChangedBy()).isEqualTo(new LoadPlannerId(newPlanner));
        assertThat(testHelper.loadPlannerName(new LoadPlannerId(newPlanner))).contains("Jan Janssens");
    }

    @Test
    void aTokenWhoseSubjectIsNotAUuidIsNotALoadPlanner() throws Exception {
        mockMvc.perform(assign("AKE20417HJ", "{\"loadPosition\": \"CR\"}").with(token("user")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("unknown-load-planner"));

        assertThat(uld("AKE20417HJ").hasPosition()).isFalse();
    }

    @Test
    void aFlightPlannerMayReadButNotAssign() throws Exception {
        mockMvc.perform(assign("AKE20417HJ", "{\"loadPosition\": \"CR\"}").with(planner("flight-planner")))
            .andExpect(status().isForbidden());
        mockMvc.perform(get(CARGO, id.id()).with(planner("flight-planner")))
            .andExpect(status().isOk());

        assertThat(uld("AKE20417HJ").hasPosition()).isFalse();
    }

    /** One refusal of the domain through the API: it leaves as 409 with its reason, never as 500. */
    @Test
    void anOccupiedPositionIsRefusedWithItsReason() throws Exception {
        LoadPlannerId loadPlanner = testHelper.saveLoadPlanner("Test load planner");

        mockMvc.perform(assign("AKE20417HJ", "{\"loadPosition\": \"CL\"}").with(token(loadPlanner.id().toString())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("rule-refused"))
            .andExpect(jsonPath("$.detail").value("Load position CL already holds ULD AKE16689R7"));

        assertThat(uld("AKE20417HJ").hasPosition()).isFalse();
    }

    /**
     * A line break in the request must not let a logged-in user start a log line of their own. The reason of the
     * refusal repeats what was sent and is logged at debug level, so the test turns that level on: the text is in
     * the log, but inside the line of the refusal, never at the start of a line.
     */
    @Test
    void theCallerCannotStartALogLineOfTheirOwn(CapturedOutput output) throws Exception {
        LoadPlannerId loadPlanner = testHelper.saveLoadPlanner("Test load planner");
        Logger apiLog = (Logger) LoggerFactory.getLogger("be.kdg.airfreight.loadplanning.api");
        Level before = apiLog.getLevel();
        apiLog.setLevel(Level.DEBUG);
        try {
            mockMvc.perform(assign("AKE20417HJ", "{\"loadPosition\": \"CL\\n2026-01-09 INFO forged log line\"}")
                    .with(token(loadPlanner.id().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid-input"));
        } finally {
            apiLog.setLevel(before);
        }

        assertThat(output.getAll()).as("the refusal is logged with its reason").contains("forged log line");
        assertThat(output.getAll().lines()).noneMatch(line -> line.startsWith("2026-01-09 INFO forged"));
    }

    @Test
    void aSuccessfulAssignmentIsLoggedWithItsCheckedValues(CapturedOutput output) throws Exception {
        LoadPlannerId loadPlanner = testHelper.saveLoadPlanner("Test load planner");

        mockMvc.perform(assign("AKE20417HJ", "{\"loadPosition\": \"CR\"}").with(token(loadPlanner.id().toString())))
            .andExpect(status().isOk());

        assertThat(output.getAll()).contains("put ULD AKE20417HJ on load position CR");
    }

    @Test
    void checkLoadGivesOneResultPerCheckAndChangesNothing() throws Exception {
        mockMvc.perform(get(LOAD_CHECK, id.id()).with(token(testHelper.saveLoadPlanner("Test load planner"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hardChecksPassed").value(false))
            .andExpect(jsonPath("$.results.length()").value(3))
            .andExpect(jsonPath(LATERAL_BALANCE + ".severity", contains("HARD")))
            .andExpect(jsonPath(LATERAL_BALANCE + ".passed", contains(false)))
            .andExpect(jsonPath(LATERAL_BALANCE + ".reason", contains(
                "Left carries 1250 kg and right 0 kg; the heaviest side may carry at most 0 kg")));

        assertThat(testHelper.find(id).isFinalized()).isFalse();
        assertThat(testHelper.outboxCount()).isZero();
    }

    @Test
    void aLoadPlannerFinalizesWhenEveryHardCheckPassesAndGetsTheReport() throws Exception {
        testHelper.place(id, "AKE20417HJ", "CR");
        LoadPlannerId loadPlanner = testHelper.saveLoadPlanner("Test load planner");

        mockMvc.perform(post(FINALIZE, id.id()).with(token(loadPlanner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hardChecksPassed").value(true))
            .andExpect(jsonPath("$.results.length()").value(3));

        FlightCargo stored = testHelper.find(id);
        assertThat(stored.isFinalized()).isTrue();
        assertThat(stored.getFinalizedBy()).isEqualTo(loadPlanner);
        assertThat(testHelper.outboxCount()).as("the finalized load leaves for the CPM").isOne();
    }

    @Test
    void aFailingHardCheckIsRefusedWithTheReportAndChangesNothing() throws Exception {
        mockMvc.perform(post(FINALIZE, id.id()).with(token(testHelper.saveLoadPlanner("Test load planner"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("hard-check-failed"))
            .andExpect(jsonPath("$.detail").value("The load cannot be finalized: a hard load check fails"))
            .andExpect(jsonPath("$.report.hardChecksPassed").value(false))
            .andExpect(jsonPath(REPORT_LATERAL_BALANCE + ".passed", contains(false)));

        assertThat(testHelper.find(id).isFinalized()).isFalse();
        assertThat(testHelper.outboxCount()).isZero();
    }

    @Test
    void outsideTheLoadingPeriodFinalizingIsRefusedWithItsReason() throws Exception {
        testHelper.place(id, "AKE20417HJ", "CR");
        clock.set(Instant.parse("2026-01-09T06:10:00Z"));

        mockMvc.perform(post(FINALIZE, id.id()).with(token(testHelper.saveLoadPlanner("Test load planner"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("rule-refused"));

        assertThat(testHelper.find(id).isFinalized()).isFalse();
    }

    private MockHttpServletRequestBuilder assign(String uldCode, String body) {
        return put(LOAD_POSITION, id.id(), uldCode).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private Uld uld(String code) {
        FlightCargo cargo = testHelper.find(id);
        return cargo.getUlds().stream().filter(uld -> uld.getCode().value().equals(code)).findFirst().orElseThrow();
    }

    /** A registered load planner. */
    private static RequestPostProcessor token(LoadPlannerId loadPlanner) {
        return token(loadPlanner.id().toString());
    }

    /** A load planner with this account. */
    private static RequestPostProcessor token(String subject) {
        return jwt().jwt(token -> token.subject(subject)).authorities(new SimpleGrantedAuthority("ROLE_load-planner"));
    }

    /** A planner who only reads: the account does not matter. */
    private static RequestPostProcessor planner(String role) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
