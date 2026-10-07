package be.kdg.airfreight.simulator.acceptance;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@IncludeEngines("cucumber")
@SelectPackages("features.simulator")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "be.kdg.airfreight.simulator.acceptance.integration")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@integration and not @to-confirm and not @wip")
class IntegrationAcceptanceTest {
}
