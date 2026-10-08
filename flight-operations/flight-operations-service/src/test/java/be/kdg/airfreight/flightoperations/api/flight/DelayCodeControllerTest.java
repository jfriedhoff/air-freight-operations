package be.kdg.airfreight.flightoperations.api.flight;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DelayCodeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void aPlannerGetsTheIataDelayReasonsToChooseFrom() throws Exception {
        mockMvc.perform(get("/api/delay-codes")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_flight-planner"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(greaterThan(50)))
            .andExpect(jsonPath("$[?(@.code == '93')].description").value("Aircraft rotation"));
    }

    @Test
    void withoutATokenTheListIsNotGiven() throws Exception {
        mockMvc.perform(get("/api/delay-codes")).andExpect(status().isUnauthorized());
    }
}
