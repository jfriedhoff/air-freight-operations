package be.kdg.airfreight.flightoperations.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** No controller is needed: a request that passes security ends as 404, which tells it from 401 and 403. */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {
    private static final String NOTHING_HERE = "/api/nothing-here";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void withoutATokenTheApiAnswersUnauthorized() throws Exception {
        mockMvc.perform(get(NOTHING_HERE)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(NOTHING_HERE)).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenWithoutAPlannerRoleIsForbidden() throws Exception {
        mockMvc.perform(get(NOTHING_HERE).with(jwt())).andExpect(status().isForbidden());
    }

    @Test
    void bothPlannersMayRead() throws Exception {
        mockMvc.perform(get(NOTHING_HERE).with(roles("flight-planner"))).andExpect(status().isNotFound());
        mockMvc.perform(get(NOTHING_HERE).with(roles("load-planner"))).andExpect(status().isNotFound());
    }

    @Test
    void onlyAFlightPlannerMayChange() throws Exception {
        mockMvc.perform(post(NOTHING_HERE).with(roles("flight-planner"))).andExpect(status().isNotFound());
        mockMvc.perform(post(NOTHING_HERE).with(roles("load-planner"))).andExpect(status().isForbidden());
    }

    @Test
    void aUserWithBothPlannerRolesCannotReadAnything() throws Exception {
        mockMvc.perform(get(NOTHING_HERE).with(roles("flight-planner", "load-planner")))
            .andExpect(status().isForbidden());
    }

    @Test
    void aUserWithBothPlannerRolesCannotChangeAnything() throws Exception {
        mockMvc.perform(post(NOTHING_HERE).with(roles("flight-planner", "load-planner")))
            .andExpect(status().isForbidden());
    }

    @Test
    void thePageItselfNeedsNoToken() throws Exception {
        mockMvc.perform(get("/no-such-page.html")).andExpect(status().isNotFound());
    }

    private static RequestPostProcessor roles(String... roles) {
        SimpleGrantedAuthority[] authorities = Arrays.stream(roles)
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toArray(SimpleGrantedAuthority[]::new);
        return jwt().authorities(authorities);
    }
}
