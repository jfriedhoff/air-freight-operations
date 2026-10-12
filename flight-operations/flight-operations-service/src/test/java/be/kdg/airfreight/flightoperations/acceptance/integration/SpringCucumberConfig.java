package be.kdg.airfreight.flightoperations.acceptance.integration;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClock.class)
public class SpringCucumberConfig {
}
