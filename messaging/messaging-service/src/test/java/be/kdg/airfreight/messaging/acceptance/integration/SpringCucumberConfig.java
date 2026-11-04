package be.kdg.airfreight.messaging.acceptance.integration;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;

@CucumberContextConfiguration
@SpringBootTest
public class SpringCucumberConfig {
    /** System time is outside our code: the scenarios set it themselves ("processed on 2 January 2026"). */
    @MockitoBean
    private Clock clock;
}
