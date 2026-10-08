package be.kdg.airfreight.simulator.acceptance;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.*;

/** The Spring context comes from acceptance.integration, the steps from acceptance.steps. */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features.simulator")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "be.kdg.airfreight.simulator.acceptance.integration,"
    + "be.kdg.airfreight.simulator.acceptance.steps")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME,
    value = "@integration and not @production and not @to-confirm and not @wip")
class IntegrationAcceptanceTest {
}
