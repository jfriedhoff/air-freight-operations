package be.kdg.airfreight.simulator.acceptance;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.*;

/** The @production scenarios: the same steps as IntegrationAcceptanceTest, against the production profile. */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features.simulator")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "be.kdg.airfreight.simulator.acceptance.production,"
    + "be.kdg.airfreight.simulator.acceptance.steps")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME,
    value = "@integration and @production and not @to-confirm and not @wip")
class ProductionAcceptanceTest {
}
