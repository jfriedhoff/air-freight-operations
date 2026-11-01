package be.kdg.airfreight.flightoperations.api;

import java.util.UUID;

import be.kdg.airfreight.flightoperations.TestClock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UnexpectedErrorHandling catches every exception, but only after Spring's own Problem Details handler: the errors
 * Spring answers itself keep their status and do not become a 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClock.class)
class SpringErrorAnswersTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void aPathThatDoesNotExistStaysNotFound() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").with(planner()))
            .andExpect(status().isNotFound());
    }

    @Test
    void aVerbThatTheResourceDoesNotKnowStaysMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/flights").with(planner()))
            .andExpect(status().isMethodNotAllowed());
    }

    private static RequestPostProcessor planner() {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_flight-planner"));
    }
}
